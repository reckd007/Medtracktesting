package com.example.data.supabase.models

import java.util.UUID

/**
 * Data model matching the Supabase `family_profiles` table.
 */
data class FamilyProfileDto(
    val id: String = UUID.randomUUID().toString(),
    val primaryUserId: String? = null,
    val patientName: String,
    val patientPhone: String,
    val relationship: String,
    val verificationStatus: String = STATUS_PENDING,
    val verificationCode: String = "",
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    val isVerified: Boolean
        get() = verificationStatus.equals(STATUS_VERIFIED, ignoreCase = true)

    val isPending: Boolean
        get() = !isVerified

    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_VERIFIED = "verified"
    }
}

/**
 * Data model matching the Supabase `medicines` table linked to `family_profiles`.
 */
data class SupabaseMedicineDto(
    val id: String = UUID.randomUUID().toString(),
    val familyProfileId: String,
    val name: String,
    val dosage: String,
    val form: String = "pill",
    val instructions: String = "Take as directed",
    val timesOfDay: String = "08:00",
    val timingSlots: String = "Morning",
    val stockRemaining: Int = 30,
    val refillThreshold: Int = 10,
    val createdAt: String? = null
)
