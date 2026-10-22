package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthAndOnboardingSystemTest {

    private lateinit var context: Context
    private lateinit var sessionManager: AuthSessionManager
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = AuthSessionManager(context)
        sessionManager.clearSession()
        authRepository = AuthRepository(context, sessionManager = sessionManager)
    }

    @Test
    fun testInitialSessionStateIsLoggedOut() {
        assertFalse(sessionManager.hasActiveSession())
        assertNull(sessionManager.getSession())
        assertFalse(authRepository.isLoggedIn)
    }

    @Test
    fun testSaveAndRestoreSession() {
        val profile = UserProfile(
            id = "user_123",
            firstName = "Alex",
            lastName = "Henderson",
            email = "alex.henderson@example.com",
            gender = "Male",
            dateOfBirth = "January 15, 1990",
            age = 34,
            isVerified = true
        )
        val session = AuthSession(
            userId = "user_123",
            email = "alex.henderson@example.com",
            accessToken = "access_token_abc",
            refreshToken = "refresh_token_xyz",
            isEmailVerified = true,
            userProfile = profile,
            preferences = UserOnboardingPreferences(
                getReminders = true,
                trackDoseTaken = true,
                keepMedList = true,
                refillReminders = true,
                checkInteractions = true
            )
        )

        sessionManager.saveSession(session)

        assertTrue(sessionManager.hasActiveSession())
        val restored = sessionManager.getSession()
        assertNotNull(restored)
        assertEquals("user_123", restored?.userId)
        assertEquals("alex.henderson@example.com", restored?.email)
        assertEquals("Alex Henderson", restored?.userProfile?.fullName)
        assertEquals(34, restored?.userProfile?.age)
        assertTrue(restored?.preferences?.getReminders == true)
        assertTrue(restored?.preferences?.checkInteractions == true)
    }

    @Test
    fun testClearSessionLogsOut() {
        val session = AuthSession(
            userId = "user_456",
            email = "test@example.com",
            isEmailVerified = true
        )
        sessionManager.saveSession(session)
        assertTrue(sessionManager.hasActiveSession())

        sessionManager.clearSession()
        assertFalse(sessionManager.hasActiveSession())
        assertNull(sessionManager.getSession())
    }

    @Test
    fun testOfflineLocalSessionCreation() {
        val draft = RegistrationDraft(
            firstName = "Sarah",
            lastName = "Connor",
            gender = "Female",
            birthDateFormatted = "May 12, 1985",
            calculatedAge = 39,
            email = "sarah.connor@example.com",
            selectedPreferences = setOf(
                RegistrationDraft.PREF_GET_REMINDERS,
                RegistrationDraft.PREF_CHECK_INTERACTIONS
            )
        )

        val session = authRepository.createLocalOfflineSession(draft)
        assertTrue(session.isOfflineMode)
        assertTrue(session.isEmailVerified)
        assertEquals("Sarah Connor", session.userProfile.fullName)
        assertEquals(39, session.userProfile.age)
        assertTrue(session.preferences.getReminders)
        assertTrue(session.preferences.checkInteractions)
        assertFalse(session.preferences.trackMeasurements)

        // Session manager should now have active session
        assertTrue(sessionManager.hasActiveSession())
    }

    @Test
    fun testRegistrationDraftToPreferencesMapping() {
        val draft = RegistrationDraft(
            selectedPreferences = setOf(
                RegistrationDraft.PREF_GET_REMINDERS,
                RegistrationDraft.PREF_TRACK_TAKEN,
                RegistrationDraft.PREF_REFILL_REMINDERS
            )
        )
        val prefs = draft.toOnboardingPreferences()
        assertTrue(prefs.getReminders)
        assertTrue(prefs.trackDoseTaken)
        assertFalse(prefs.keepMedList)
        assertTrue(prefs.refillReminders)
        assertFalse(prefs.checkInteractions)
    }

    @Test
    fun testEmailVerificationOtpCompletion() {
        sessionManager.setPendingVerification("pending@example.com", "uid_999")
        assertEquals("pending@example.com", sessionManager.getPendingVerificationEmail())

        sessionManager.markEmailVerified()
        assertNull(sessionManager.getPendingVerificationEmail())
    }

    @Test
    fun testRegisterUserWhenServerUnreachableFallsBackToLocalVerification() = kotlinx.coroutines.test.runTest {
        val draft = RegistrationDraft(
            firstName = "John",
            lastName = "Doe",
            email = "john.doe@example.com",
            password = "Password123"
        )
        val result = authRepository.registerUser(draft)
        assertTrue(result is AuthResult.EmailVerificationRequired)
        val verifyReq = result as AuthResult.EmailVerificationRequired
        assertEquals("john.doe@example.com", verifyReq.email)
    }

    @Test
    fun testLoginWhenServerUnreachableFallsBackToLocalSession() = kotlinx.coroutines.test.runTest {
        val result = authRepository.login("offline.user@example.com", "Password123")
        assertTrue(result is AuthResult.Success)
        val success = result as AuthResult.Success
        assertEquals("offline.user@example.com", success.session.email)
        assertTrue(success.session.isOfflineMode)
        assertTrue(sessionManager.hasActiveSession())
    }
}
