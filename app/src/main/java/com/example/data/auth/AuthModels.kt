package com.example.data.auth

import java.io.Serializable

/**
 * User Profile information captured during onboarding and persisted in the backend/local session.
 */
data class UserProfile(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val gender: String = "",
    val otherGenderDescription: String = "",
    val dateOfBirth: String = "",
    val age: Int = 0,
    val isVerified: Boolean = false,
    val isOfflineAccount: Boolean = false,
    val isGuest: Boolean = false,
    val createdAt: String = ""
) : Serializable {
    val fullName: String
        get() = when {
            firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
            firstName.isNotBlank() -> firstName
            lastName.isNotBlank() -> lastName
            isGuest -> "Guest User"
            else -> "Self"
        }

    val displayGender: String
        get() = when {
            gender.equals("Other", ignoreCase = true) && otherGenderDescription.isNotBlank() -> otherGenderDescription
            gender.isNotBlank() -> gender
            else -> "Not specified"
        }
}

/**
 * User preferences chosen during Step 4 of the registration flow.
 */
data class UserOnboardingPreferences(
    val getReminders: Boolean = true,
    val trackDoseTaken: Boolean = true,
    val keepMedList: Boolean = true,
    val refillReminders: Boolean = true,
    val checkInteractions: Boolean = true,
    val trackSideEffects: Boolean = true,
    val trackMeasurements: Boolean = true
) : Serializable

/**
 * Active authenticated session model.
 */
data class AuthSession(
    val userId: String,
    val email: String,
    val accessToken: String = "",
    val refreshToken: String = "",
    val isEmailVerified: Boolean = false,
    val userProfile: UserProfile = UserProfile(),
    val preferences: UserOnboardingPreferences = UserOnboardingPreferences(),
    val isOfflineMode: Boolean = false,
    val isGuest: Boolean = false,
    val loginTimestamp: Long = System.currentTimeMillis()
) : Serializable

/**
 * State of the multi-step registration draft while the user progresses through steps.
 */
data class RegistrationDraft(
    val firstName: String = "",
    val lastName: String = "",
    val gender: String = "",
    val otherGenderText: String = "",
    val birthDateMillis: Long? = null,
    val birthDateFormatted: String = "",
    val calculatedAge: Int = 0,
    val selectedPreferences: Set<String> = setOf(
        PREF_GET_REMINDERS,
        PREF_TRACK_TAKEN,
        PREF_KEEP_LIST,
        PREF_REFILL_REMINDERS
    ),
    val email: String = "",
    val password: String = "",
    val verificationCode: String = "",
    val isUnderageError: Boolean = false
) : Serializable {
    companion object {
        const val PREF_GET_REMINDERS = "Get medication reminders"
        const val PREF_TRACK_TAKEN = "Track whether I took my medications"
        const val PREF_KEEP_LIST = "Keep a list of my medications"
        const val PREF_REFILL_REMINDERS = "Remember when it's time to refill"
        const val PREF_CHECK_INTERACTIONS = "Check for potential drug interactions"
        const val PREF_TRACK_SIDE_EFFECTS = "Track side effects or symptoms"
        const val PREF_TRACK_MEASUREMENTS = "Track measurements, such as blood pressure"

        val ALL_PREFERENCES = listOf(
            PREF_GET_REMINDERS,
            PREF_TRACK_TAKEN,
            PREF_KEEP_LIST,
            PREF_REFILL_REMINDERS,
            PREF_CHECK_INTERACTIONS,
            PREF_TRACK_SIDE_EFFECTS,
            PREF_TRACK_MEASUREMENTS
        )
    }

    fun toOnboardingPreferences(): UserOnboardingPreferences {
        return UserOnboardingPreferences(
            getReminders = selectedPreferences.contains(PREF_GET_REMINDERS),
            trackDoseTaken = selectedPreferences.contains(PREF_TRACK_TAKEN),
            keepMedList = selectedPreferences.contains(PREF_KEEP_LIST),
            refillReminders = selectedPreferences.contains(PREF_REFILL_REMINDERS),
            checkInteractions = selectedPreferences.contains(PREF_CHECK_INTERACTIONS),
            trackSideEffects = selectedPreferences.contains(PREF_TRACK_SIDE_EFFECTS),
            trackMeasurements = selectedPreferences.contains(PREF_TRACK_MEASUREMENTS)
        )
    }
}

/**
 * Result sealed class for authentication operations.
 */
sealed class AuthResult {
    data class Success(val session: AuthSession, val message: String? = null) : AuthResult()
    data class EmailVerificationRequired(val email: String, val userId: String, val message: String) : AuthResult()
    data class Error(val message: String, val isNetworkError: Boolean = false, val statusCode: Int = 0) : AuthResult()
}
