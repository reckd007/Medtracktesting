package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.supabase.models.FamilyProfileDto
import com.example.data.supabase.models.SupabaseMedicineDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Repository providing secure Family Member management and verification backed by local Supabase.
 */
class SupabaseFamilyRepository(
    context: Context,
    private val client: SupabaseClient = SupabaseClient()
) {
    private val prefs = context.applicationContext.getSharedPreferences("supabase_family_cache", Context.MODE_PRIVATE)
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

    init {
        SupabaseConfig.initialize(context)
        seedInitialSampleProfilesIfEmpty()
    }

    /**
     * Requirement 4.1: Adds a dependent relative (e.g. Uncle Arthur) with a random 6-digit verification code.
     * Starts in 'pending' status until verified from patient's phone.
     */
    suspend fun addFamilyMemberWithVerification(
        patientName: String,
        relationship: String,
        patientPhone: String,
        primaryUserId: String? = null
    ): Result<FamilyProfileDto> = withContext(Dispatchers.IO) {
        val verificationCode = (100000..999999).random().toString()
        val profileId = UUID.randomUUID().toString()
        val now = isoFormat.format(Date())

        val newProfile = FamilyProfileDto(
            id = profileId,
            primaryUserId = primaryUserId,
            patientName = patientName.trim(),
            patientPhone = patientPhone.trim(),
            relationship = relationship.trim(),
            verificationStatus = FamilyProfileDto.STATUS_PENDING,
            verificationCode = verificationCode,
            createdAt = now,
            updatedAt = now
        )

        // 1. Save to local cached list
        saveProfileToLocal(newProfile)

        // 2. Post to Supabase PostgREST table
        try {
            val jsonPayload = JSONObject().apply {
                put("id", newProfile.id)
                if (primaryUserId != null) put("primary_user_id", primaryUserId)
                put("patient_name", newProfile.patientName)
                put("patient_phone", newProfile.patientPhone)
                put("relationship", newProfile.relationship)
                put("verification_status", newProfile.verificationStatus)
                put("verification_code", newProfile.verificationCode)
            }.toString()

            val remoteResult = client.post("family_profiles", jsonPayload)
            if (remoteResult.isSuccess) {
                Log.d(TAG, "Successfully synced pending profile to local Supabase: ${newProfile.patientName}")
            } else {
                Log.w(TAG, "Supabase remote sync warning (using cached copy): ${remoteResult.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Notice syncing to Supabase: ${e.message}")
        }

        Result.success(newProfile)
    }

    /**
     * Requirement 4.2: Verifies patient device entry using the 6-digit confirmation code.
     * Transitions verification_status to 'verified' in Supabase and local cache.
     */
    suspend fun verifyFamilyMember(profileId: String, code: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val localProfile = getLocalProfile(profileId)

        // Check verification code against local cache or Supabase RPC
        val matches = localProfile != null && localProfile.verificationCode.trim() == code.trim()

        if (matches && localProfile != null) {
            val updated = localProfile.copy(
                verificationStatus = FamilyProfileDto.STATUS_VERIFIED,
                updatedAt = isoFormat.format(Date())
            )
            saveProfileToLocal(updated)

            // Sync update to Supabase via RPC or PATCH
            try {
                val rpcPayload = JSONObject().apply {
                    put("target_profile_id", profileId)
                    put("entered_code", code.trim())
                }.toString()

                val rpcResult = client.rpc("verify_family_profile", rpcPayload)
                if (rpcResult.isFailure) {
                    // Fallback to direct PostgREST PATCH
                    val patchPayload = JSONObject().apply {
                        put("verification_status", FamilyProfileDto.STATUS_VERIFIED)
                        put("updated_at", isoFormat.format(Date()))
                    }.toString()
                    client.patch("family_profiles?id=eq.$profileId", patchPayload)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase verify sync error: ${e.message}")
            }

            Result.success(true)
        } else {
            // Also attempt remote check directly in case code was updated remotely
            try {
                val rpcPayload = JSONObject().apply {
                    put("target_profile_id", profileId)
                    put("entered_code", code.trim())
                }.toString()
                val rpcResult = client.rpc("verify_family_profile", rpcPayload)
                if (rpcResult.isSuccess && rpcResult.getOrNull()?.contains("true") == true) {
                    localProfile?.let {
                        saveProfileToLocal(it.copy(verificationStatus = FamilyProfileDto.STATUS_VERIFIED))
                    }
                    return@withContext Result.success(true)
                }
            } catch (_: Exception) {}

            Result.failure(IllegalArgumentException("Invalid verification code. Please check the code sent to the patient's phone."))
        }
    }

    /**
     * Requirement 4.3: Fetches verified family profiles where verification_status = 'verified'.
     */
    suspend fun fetchVerifiedFamilyProfiles(): Result<List<FamilyProfileDto>> = withContext(Dispatchers.IO) {
        val remote = fetchProfilesFromSupabase(query = "verification_status=eq.verified")
        val remoteList = remote.getOrNull()
        if (remote.isSuccess && !remoteList.isNullOrEmpty()) {
            remoteList.forEach { saveProfileToLocal(it) }
            Result.success(remoteList)
        } else {
            // Return verified from local cache
            val localVerified = getLocalProfiles().filter { it.isVerified }
            Result.success(localVerified)
        }
    }

    /**
     * Fetches all family profiles (both pending and verified) for care management.
     */
    suspend fun fetchAllFamilyProfiles(): Result<List<FamilyProfileDto>> = withContext(Dispatchers.IO) {
        val remote = fetchProfilesFromSupabase(query = "order=created_at.desc")
        val remoteList = remote.getOrNull()
        if (remote.isSuccess && !remoteList.isNullOrEmpty()) {
            remoteList.forEach { saveProfileToLocal(it) }
            Result.success(remoteList)
        } else {
            Result.success(getLocalProfiles())
        }
    }

    /**
     * Requirement 3.3 Access Lockout:
     * Adds medicine for a family profile ONLY if that profile is verified.
     */
    suspend fun addMedicineForProfile(
        familyProfileId: String,
        name: String,
        dosage: String,
        form: String = "pill",
        instructions: String = "Take as directed",
        timesOfDay: String = "08:00",
        timingSlots: String = "Morning"
    ): Result<SupabaseMedicineDto> = withContext(Dispatchers.IO) {
        val profile = getLocalProfile(familyProfileId)
        if (profile == null || profile.isPending) {
            return@withContext Result.failure(
                IllegalStateException("Access Lockout: Cannot add medication. Waiting for verification from ${profile?.patientName ?: "patient"}'s device.")
            )
        }

        val medicine = SupabaseMedicineDto(
            id = UUID.randomUUID().toString(),
            familyProfileId = familyProfileId,
            name = name,
            dosage = dosage,
            form = form,
            instructions = instructions,
            timesOfDay = timesOfDay,
            timingSlots = timingSlots,
            createdAt = isoFormat.format(Date())
        )

        try {
            val payload = JSONObject().apply {
                put("id", medicine.id)
                put("family_profile_id", medicine.familyProfileId)
                put("name", medicine.name)
                put("dosage", medicine.dosage)
                put("form", medicine.form)
                put("instructions", medicine.instructions)
                put("times_of_day", medicine.timesOfDay)
                put("timing_slots", medicine.timingSlots)
            }.toString()
            client.post("medicines", payload)
        } catch (e: Exception) {
            Log.w(TAG, "Error posting medicine to Supabase: ${e.message}")
        }

        Result.success(medicine)
    }

    suspend fun removeProfile(profileId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val list = getLocalProfiles().filter { it.id != profileId }
        saveAllProfilesToLocal(list)
        try {
            client.patch("family_profiles?id=eq.$profileId", JSONObject().apply {
                put("verification_status", "archived")
            }.toString())
        } catch (_: Exception) {}
        Result.success(true)
    }

    // --- Helper & Caching Functions ---

    private suspend fun fetchProfilesFromSupabase(query: String): Result<List<FamilyProfileDto>> {
        return try {
            val res = client.get("family_profiles?$query")
            if (res.isSuccess) {
                val array = JSONArray(res.getOrNull())
                val list = mutableListOf<FamilyProfileDto>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(parseProfileJson(obj))
                }
                Result.success(list)
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseProfileJson(obj: JSONObject): FamilyProfileDto {
        return FamilyProfileDto(
            id = obj.optString("id", UUID.randomUUID().toString()),
            primaryUserId = obj.optString("primary_user_id").takeIf { it.isNotBlank() },
            patientName = obj.optString("patient_name", "Relative"),
            patientPhone = obj.optString("patient_phone", ""),
            relationship = obj.optString("relationship", "Dependent"),
            verificationStatus = obj.optString("verification_status", FamilyProfileDto.STATUS_PENDING),
            verificationCode = obj.optString("verification_code", ""),
            createdAt = obj.optString("created_at").takeIf { it.isNotBlank() },
            updatedAt = obj.optString("updated_at").takeIf { it.isNotBlank() }
        )
    }

    fun getLocalProfiles(): List<FamilyProfileDto> {
        val json = prefs.getString("cached_profiles_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<FamilyProfileDto>()
            for (i in 0 until array.length()) {
                list.add(parseProfileJson(array.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getLocalProfile(id: String): FamilyProfileDto? {
        return getLocalProfiles().find { it.id == id }
    }

    private fun saveProfileToLocal(profile: FamilyProfileDto) {
        val current = getLocalProfiles().filter { it.id != profile.id } + profile
        saveAllProfilesToLocal(current)
    }

    private fun saveAllProfilesToLocal(list: List<FamilyProfileDto>) {
        val array = JSONArray()
        list.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                if (p.primaryUserId != null) put("primary_user_id", p.primaryUserId)
                put("patient_name", p.patientName)
                put("patient_phone", p.patientPhone)
                put("relationship", p.relationship)
                put("verification_status", p.verificationStatus)
                put("verification_code", p.verificationCode)
                put("created_at", p.createdAt)
                put("updated_at", p.updatedAt)
            }
            array.put(obj)
        }
        prefs.edit().putString("cached_profiles_json", array.toString()).apply()
    }

    private fun seedInitialSampleProfilesIfEmpty() {
        if (getLocalProfiles().isEmpty()) {
            val initial = listOf(
                FamilyProfileDto(
                    id = "a0000000-0000-0000-0000-000000000001",
                    patientName = "Robert Henderson",
                    patientPhone = "+1 (555) 876-5432",
                    relationship = "Father",
                    verificationStatus = FamilyProfileDto.STATUS_VERIFIED,
                    verificationCode = "849201"
                ),
                FamilyProfileDto(
                    id = "a0000000-0000-0000-0000-000000000002",
                    patientName = "Arthur Miller",
                    patientPhone = "+1 (555) 432-9876",
                    relationship = "Uncle",
                    verificationStatus = FamilyProfileDto.STATUS_PENDING,
                    verificationCode = "739104"
                )
            )
            saveAllProfilesToLocal(initial)
        }
    }

    companion object {
        private const val TAG = "SupabaseFamilyRepo"
    }
}
