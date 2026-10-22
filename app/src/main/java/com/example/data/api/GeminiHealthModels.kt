package com.example.data.api

import java.util.UUID

enum class ChatRole { USER, ASSISTANT, SYSTEM }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEmergencyAlert: Boolean = false,
    val languageCode: String = "en" // "en", "hi", "ur"
)

data class PatientHealthContext(
    val profileName: String,
    val medications: List<PatientMedicationContext> = emptyList(),
    val allergies: String = "None specified",
    val age: String = "Not specified"
)

data class PatientMedicationContext(
    val name: String,
    val dosage: String,
    val form: String,
    val timesOfDay: String,
    val instructions: String
)

object EmergencyDetector {
    private val emergencyKeywordsEn = listOf(
        "chest pain", "trouble breathing", "shortness of breath", "stroke", "numbness",
        "paralysis", "severe allergic", "anaphylaxis", "heavy bleeding", "unconscious",
        "fainted", "overdose", "poison", "poisoning", "suicide", "self harm", "self-harm",
        "seizure", "choking"
    )

    private val emergencyKeywordsHi = listOf(
        "छाती में दर्द", "सीने में दर्द", "सांस लेने में तकलीफ", "सांस फूलना",
        "स्ट्रोक", "लकवा", "अत्यधिक रक्तस्राव", "बेहोश", "ओवरडोज", "जहर", "आत्महत्या"
    )

    private val emergencyKeywordsUr = listOf(
        "سینے میں درد", "سانس لینے میں دشواری", "فالج", "شدید الرجی", "زیادہ خون بہنا",
        "بے ہوش", "زہر", "خودکشی"
    )

    fun checkEmergency(text: String): Boolean {
        val lower = text.lowercase()
        return emergencyKeywordsEn.any { lower.contains(it) } ||
                emergencyKeywordsHi.any { text.contains(it) } ||
                emergencyKeywordsUr.any { text.contains(it) }
    }
}
