package com.example.data.model

data class FamilyMember(
    val id: String,
    val name: String,
    val relationship: String,
    val initial: String,
    val ageOrBirth: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    val emergencyContact: String = "",
    val isPrimary: Boolean = false,
    val patientPhone: String = "",
    val verificationStatus: String = "verified",
    val verificationCode: String = ""
) {
    val isVerified: Boolean
        get() = isPrimary || verificationStatus.equals("verified", ignoreCase = true)

    val isPending: Boolean
        get() = !isVerified
}

data class UserAccountInfo(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val bloodType: String = "",
    val primaryDoctor: String = "",
    val insuranceProvider: String = "",
    val preferredPharmacy: String = "",
    val userId: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val isVerified: Boolean = false,
    val isGuest: Boolean = false
)

data class AppPreferences(
    val pushNotificationsEnabled: Boolean = true,
    val alertSoundEnabled: Boolean = true,
    val alertVibrationEnabled: Boolean = true,
    val defaultLowStockThreshold: Int = 10,
    val snoozeMinutes: Int = 15,
    val bloodSugarUnit: String = "mg/dL",
    val weightUnit: String = "lbs",
    val bloodPressureUnit: String = "mmHg",
    val textSize: String = "NORMAL", // "SMALL", "NORMAL", "LARGE", "EXTRA_LARGE"
    val colorPalette: String = "ROYAL", // "ROYAL", "TEAL", "NAVY", "MINT", "SKY", "CORAL"
    val appearance: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    val aiSeeMedicinesEnabled: Boolean = false,
    val aiSeenMedPrompt: Boolean = false,
    val aiHideDisclaimer: Boolean = false,
    val hasCompletedOnboarding: Boolean = false
) {
    val textSizeScale: Float
        get() = when (textSize.uppercase()) {
            "SMALL" -> 0.88f
            "LARGE" -> 1.15f
            "EXTRA_LARGE" -> 1.30f
            else -> 1.0f
        }
}
