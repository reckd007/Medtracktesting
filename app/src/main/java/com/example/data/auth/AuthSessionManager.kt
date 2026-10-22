package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject

/**
 * Manages secure persistent local storage of user session, profile, and onboarding states.
 */
class AuthSessionManager(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val TAG = "AuthSessionManager"
        private const val PREFS_NAME = "medtrack_auth_session_prefs"

        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_IS_EMAIL_VERIFIED = "is_email_verified"
        private const val KEY_IS_OFFLINE_MODE = "is_offline_mode"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"

        // Profile keys
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_GENDER = "gender"
        private const val KEY_OTHER_GENDER = "other_gender"
        private const val KEY_DOB = "date_of_birth"
        private const val KEY_AGE = "user_age"

        // Preferences keys
        private const val KEY_PREF_REMINDERS = "pref_onboarding_reminders"
        private const val KEY_PREF_TRACK_TAKEN = "pref_onboarding_track_taken"
        private const val KEY_PREF_KEEP_LIST = "pref_onboarding_keep_list"
        private const val KEY_PREF_REFILLS = "pref_onboarding_refills"
        private const val KEY_PREF_INTERACTIONS = "pref_onboarding_interactions"
        private const val KEY_PREF_SIDE_EFFECTS = "pref_onboarding_side_effects"
        private const val KEY_PREF_MEASUREMENTS = "pref_onboarding_measurements"

        // Pending verification
        private const val KEY_PENDING_VERIFY_EMAIL = "pending_verify_email"
        private const val KEY_PENDING_VERIFY_UID = "pending_verify_uid"
    }

    /**
     * Checks if there is an active session and onboarding was fully finished.
     */
    fun hasActiveSession(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val hasCompletedOnboarding = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        val isVerified = prefs.getBoolean(KEY_IS_EMAIL_VERIFIED, false)
        return isLoggedIn && hasCompletedOnboarding && isVerified
    }

    /**
     * Restores the saved session or returns null if no valid session exists.
     */
    fun getSession(): AuthSession? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
        val isEmailVerified = prefs.getBoolean(KEY_IS_EMAIL_VERIFIED, false)
        val isOfflineMode = prefs.getBoolean(KEY_IS_OFFLINE_MODE, false)
        val isGuest = prefs.getBoolean(KEY_IS_GUEST, false)
        val loginTimestamp = prefs.getLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())

        val profile = UserProfile(
            id = userId,
            firstName = prefs.getString(KEY_FIRST_NAME, "") ?: "",
            lastName = prefs.getString(KEY_LAST_NAME, "") ?: "",
            email = email,
            gender = prefs.getString(KEY_GENDER, "") ?: "",
            otherGenderDescription = prefs.getString(KEY_OTHER_GENDER, "") ?: "",
            dateOfBirth = prefs.getString(KEY_DOB, "") ?: "",
            age = prefs.getInt(KEY_AGE, 0),
            isVerified = isEmailVerified,
            isOfflineAccount = isOfflineMode,
            isGuest = isGuest
        )

        val preferences = UserOnboardingPreferences(
            getReminders = prefs.getBoolean(KEY_PREF_REMINDERS, true),
            trackDoseTaken = prefs.getBoolean(KEY_PREF_TRACK_TAKEN, true),
            keepMedList = prefs.getBoolean(KEY_PREF_KEEP_LIST, true),
            refillReminders = prefs.getBoolean(KEY_PREF_REFILLS, true),
            checkInteractions = prefs.getBoolean(KEY_PREF_INTERACTIONS, true),
            trackSideEffects = prefs.getBoolean(KEY_PREF_SIDE_EFFECTS, true),
            trackMeasurements = prefs.getBoolean(KEY_PREF_MEASUREMENTS, true)
        )

        return AuthSession(
            userId = userId,
            email = email,
            accessToken = accessToken,
            refreshToken = refreshToken,
            isEmailVerified = isEmailVerified,
            userProfile = profile,
            preferences = preferences,
            isOfflineMode = isOfflineMode,
            isGuest = isGuest,
            loginTimestamp = loginTimestamp
        )
    }

    /**
     * Persists an authenticated session.
     */
    fun saveSession(session: AuthSession) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, session.userId)
            putString(KEY_EMAIL, session.email)
            putString(KEY_ACCESS_TOKEN, session.accessToken)
            putString(KEY_REFRESH_TOKEN, session.refreshToken)
            putBoolean(KEY_IS_EMAIL_VERIFIED, session.isEmailVerified)
            putBoolean(KEY_IS_OFFLINE_MODE, session.isOfflineMode)
            putBoolean(KEY_IS_GUEST, session.isGuest)
            putBoolean(KEY_ONBOARDING_COMPLETED, session.isEmailVerified)
            putLong(KEY_LOGIN_TIMESTAMP, session.loginTimestamp)

            // Save profile details
            putString(KEY_FIRST_NAME, session.userProfile.firstName)
            putString(KEY_LAST_NAME, session.userProfile.lastName)
            putString(KEY_GENDER, session.userProfile.gender)
            putString(KEY_OTHER_GENDER, session.userProfile.otherGenderDescription)
            putString(KEY_DOB, session.userProfile.dateOfBirth)
            putInt(KEY_AGE, session.userProfile.age)

            // Save preferences
            putBoolean(KEY_PREF_REMINDERS, session.preferences.getReminders)
            putBoolean(KEY_PREF_TRACK_TAKEN, session.preferences.trackDoseTaken)
            putBoolean(KEY_PREF_KEEP_LIST, session.preferences.keepMedList)
            putBoolean(KEY_PREF_REFILLS, session.preferences.refillReminders)
            putBoolean(KEY_PREF_INTERACTIONS, session.preferences.checkInteractions)
            putBoolean(KEY_PREF_SIDE_EFFECTS, session.preferences.trackSideEffects)
            putBoolean(KEY_PREF_MEASUREMENTS, session.preferences.trackMeasurements)

            // Clear any pending verification
            remove(KEY_PENDING_VERIFY_EMAIL)
            remove(KEY_PENDING_VERIFY_UID)
            apply()
        }
        Log.d(TAG, "Auth session saved successfully for user: ${session.email} (verified=${session.isEmailVerified})")
    }

    /**
     * Saves pending verification email and UID when waiting for email code.
     */
    fun setPendingVerification(email: String, userId: String) {
        prefs.edit()
            .putString(KEY_PENDING_VERIFY_EMAIL, email)
            .putString(KEY_PENDING_VERIFY_UID, userId)
            .apply()
    }

    fun getPendingVerificationEmail(): String? = prefs.getString(KEY_PENDING_VERIFY_EMAIL, null)
    fun getPendingVerificationUid(): String? = prefs.getString(KEY_PENDING_VERIFY_UID, null)

    fun markEmailVerified() {
        prefs.edit()
            .putBoolean(KEY_IS_EMAIL_VERIFIED, true)
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .remove(KEY_PENDING_VERIFY_EMAIL)
            .remove(KEY_PENDING_VERIFY_UID)
            .apply()
    }

    /**
     * Completely terminates the current session and clears user credentials.
     */
    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            putBoolean(KEY_ONBOARDING_COMPLETED, false)
            remove(KEY_USER_ID)
            remove(KEY_EMAIL)
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_IS_EMAIL_VERIFIED)
            remove(KEY_IS_OFFLINE_MODE)
            remove(KEY_IS_GUEST)
            remove(KEY_LOGIN_TIMESTAMP)
            remove(KEY_FIRST_NAME)
            remove(KEY_LAST_NAME)
            remove(KEY_GENDER)
            remove(KEY_OTHER_GENDER)
            remove(KEY_DOB)
            remove(KEY_AGE)
            remove(KEY_PENDING_VERIFY_EMAIL)
            remove(KEY_PENDING_VERIFY_UID)
            apply()
        }
        Log.d(TAG, "Auth session cleared.")
    }
}
