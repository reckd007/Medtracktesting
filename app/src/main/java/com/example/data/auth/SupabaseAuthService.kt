package com.example.data.auth

import android.util.Log
import com.example.data.supabase.SupabaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Service providing direct communication with Supabase GoTrue Authentication REST endpoints.
 * Standard RFC-compliant Supabase Auth endpoints:
 * - POST /auth/v1/signup
 * - POST /auth/v1/token?grant_type=password
 * - POST /auth/v1/verify
 * - POST /auth/v1/resend
 * - POST /auth/v1/recover
 * - POST /auth/v1/logout
 */
class SupabaseAuthService(
    private val baseUrlProvider: () -> String = { SupabaseConfig.getSupabaseUrl() },
    private val anonKeyProvider: () -> String = { SupabaseConfig.getAnonKey() }
) {
    companion object {
        private const val TAG = "SupabaseAuthService"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(2500, TimeUnit.MILLISECONDS)
        .readTimeout(4000, TimeUnit.MILLISECONDS)
        .writeTimeout(4000, TimeUnit.MILLISECONDS)
        .build()

    private fun getAuthUrl(path: String): String {
        val base = baseUrlProvider().trimEnd('/')
        val cleanPath = path.trimStart('/')
        return "$base/auth/v1/$cleanPath"
    }

    /**
     * Registers a new account with email, password, and custom metadata.
     */
    suspend fun signUp(
        email: String,
        password: String,
        userMetadata: Map<String, Any?> = emptyMap()
    ): AuthResult = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext AuthResult.Error(
                "Supabase cloud backend is not configured.",
                isNetworkError = true
            )
        }
        try {
            val url = getAuthUrl("signup")
            val payload = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", password)
                if (userMetadata.isNotEmpty()) {
                    val metadataJson = JSONObject()
                    userMetadata.forEach { (k, v) -> metadataJson.put(k, v) }
                    put("data", metadataJson)
                }
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKeyProvider())
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            Log.d(TAG, "signUp response: code=${response.code}, body=$body")

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val userId = json.optString("id").ifEmpty {
                    json.optJSONObject("user")?.optString("id") ?: ""
                }
                val token = json.optString("access_token")
                val isConfirmed = json.optString("confirmed_at").isNotEmpty() ||
                        json.optJSONObject("user")?.optString("confirmed_at")?.isNotEmpty() == true

                if (token.isNotEmpty() && isConfirmed) {
                    val session = AuthSession(
                        userId = userId,
                        email = email,
                        accessToken = token,
                        refreshToken = json.optString("refresh_token"),
                        isEmailVerified = true
                    )
                    AuthResult.Success(session)
                } else {
                    AuthResult.EmailVerificationRequired(
                        email = email,
                        userId = userId,
                        message = "Account created. A verification code has been dispatched to $email."
                    )
                }
            } else {
                val errorMessage = parseErrorMessage(body, "Registration failed (HTTP ${response.code})")
                AuthResult.Error(errorMessage, statusCode = response.code)
            }
        } catch (e: Exception) {
            handleException(e, "Registration")
        }
    }

    /**
     * Authenticates existing user with email and password.
     */
    suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext AuthResult.Error(
                "Supabase cloud backend is not configured.",
                isNetworkError = true
            )
        }
        try {
            val url = getAuthUrl("token?grant_type=password")
            val payload = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", password)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKeyProvider())
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            Log.d(TAG, "signIn response: code=${response.code}")

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val accessToken = json.optString("access_token")
                val refreshToken = json.optString("refresh_token")
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id") ?: ""
                val confirmedAt = userObj?.optString("confirmed_at") ?: ""
                val isVerified = confirmedAt.isNotBlank() || accessToken.isNotBlank()

                val userMetadata = userObj?.optJSONObject("user_metadata")
                val profile = UserProfile(
                    id = userId,
                    firstName = userMetadata?.optString("first_name", "") ?: "",
                    lastName = userMetadata?.optString("last_name", "") ?: "",
                    email = email,
                    gender = userMetadata?.optString("gender", "") ?: "",
                    otherGenderDescription = userMetadata?.optString("other_gender", "") ?: "",
                    dateOfBirth = userMetadata?.optString("date_of_birth", "") ?: "",
                    age = userMetadata?.optInt("age", 0) ?: 0,
                    isVerified = isVerified
                )

                val session = AuthSession(
                    userId = userId,
                    email = email,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    isEmailVerified = isVerified,
                    userProfile = profile
                )
                AuthResult.Success(session)
            } else {
                val errorMessage = parseErrorMessage(body, "Invalid email or password.")
                AuthResult.Error(errorMessage, statusCode = response.code)
            }
        } catch (e: Exception) {
            handleException(e, "Sign In")
        }
    }

    /**
     * Verifies the email confirmation code (OTP).
     */
    suspend fun verifyEmailOtp(email: String, tokenCode: String): AuthResult = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext AuthResult.Error("Supabase cloud backend is not configured.", isNetworkError = true)
        }
        try {
            val url = getAuthUrl("verify")
            val payload = JSONObject().apply {
                put("type", "signup")
                put("email", email.trim().lowercase())
                put("token", tokenCode.trim())
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKeyProvider())
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            Log.d(TAG, "verifyEmailOtp response: code=${response.code}")

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val accessToken = json.optString("access_token")
                val refreshToken = json.optString("refresh_token")
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id") ?: ""

                val session = AuthSession(
                    userId = userId,
                    email = email,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    isEmailVerified = true
                )
                AuthResult.Success(session, "Email successfully verified.")
            } else {
                val errorMessage = parseErrorMessage(body, "Invalid or expired verification code.")
                AuthResult.Error(errorMessage, statusCode = response.code)
            }
        } catch (e: Exception) {
            handleException(e, "Email Verification")
        }
    }

    /**
     * Resends email confirmation OTP.
     */
    suspend fun resendVerificationOtp(email: String): Result<String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext Result.failure(Exception("Supabase cloud backend is not configured."))
        }
        try {
            val url = getAuthUrl("resend")
            val payload = JSONObject().apply {
                put("type", "signup")
                put("email", email.trim().lowercase())
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKeyProvider())
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Result.success("Verification code resent to $email.")
            } else {
                val error = parseErrorMessage(body, "Could not resend code. Please try again.")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(mapException(e, "Resend Code"))
        }
    }

    /**
     * Requests a password recovery email.
     */
    suspend fun recoverPassword(email: String): Result<String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured()) {
            return@withContext Result.failure(Exception("Supabase cloud backend is not configured."))
        }
        try {
            val url = getAuthUrl("recover")
            val payload = JSONObject().apply {
                put("email", email.trim().lowercase())
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKeyProvider())
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Result.success("Password reset instructions dispatched to $email.")
            } else {
                val error = parseErrorMessage(body, "Password recovery request failed.")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(mapException(e, "Password Recovery"))
        }
    }

    /**
     * Signs out the user session from the Supabase backend.
     */
    suspend fun signOut(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (accessToken.isBlank()) return@withContext true
            val url = getAuthUrl("logout")
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKeyProvider())
                .addHeader("Authorization", "Bearer $accessToken")
                .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.w(TAG, "Error logging out from Supabase: ${e.message}")
            true
        }
    }

    private fun parseErrorMessage(jsonBody: String, defaultMsg: String): String {
        return try {
            val json = JSONObject(jsonBody)
            when {
                json.has("msg") -> json.getString("msg")
                json.has("message") -> json.getString("message")
                json.has("error_description") -> json.getString("error_description")
                json.has("error") -> json.getString("error")
                else -> defaultMsg
            }
        } catch (_: Exception) {
            defaultMsg
        }
    }

    private fun handleException(e: Exception, action: String): AuthResult {
        val isNetwork = e is ConnectException || e is SocketTimeoutException || e is UnknownHostException || e is IOException
        Log.d(TAG, "$action status: ${e.message ?: e.javaClass.simpleName}")
        val message = when {
            e is ConnectException || e is UnknownHostException ->
                "Unable to connect to authentication server. Ready for offline local setup."
            e is SocketTimeoutException ->
                "Connection timed out while contacting authentication server."
            else ->
                e.message ?: "An unexpected error occurred during $action."
        }
        return AuthResult.Error(message, isNetworkError = isNetwork)
    }

    private fun mapException(e: Exception, action: String): Exception {
        Log.d(TAG, "$action status: ${e.message ?: e.javaClass.simpleName}")
        val msg = when {
            e is ConnectException || e is UnknownHostException ->
                "Authentication server unreachable."
            e is SocketTimeoutException ->
                "Request timed out. Please check network connection."
            else -> e.message ?: "$action failed."
        }
        return Exception(msg, e)
    }
}
