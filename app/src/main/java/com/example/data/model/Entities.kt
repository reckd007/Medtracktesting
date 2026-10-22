package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val form: String, // "Capsule", "Tablet", "Liquid", "Injection", "Inhaler"
    val colorHex: String = "#0D9488",
    val instructions: String = "Take with water",
    val timesOfDay: String = "08:00", // Comma separated e.g. "08:00,20:00"
    val timingSlots: String = "Morning", // Comma separated e.g. "Morning,Evening"
    val frequencyPerDay: Int = 1,
    val frequencyDaysPerWeek: Int = 7,
    val courseDurationDays: Int = 0, // 0 = Ongoing chronic, >0 = temporary (e.g. 10 days)
    val stockRemaining: Int = 30,
    val refillThreshold: Int = 5,
    val profileName: String = "Self",
    val notes: String = "",
    val rxcui: String = "", // NIH RxNorm unique concept identifier
    val daysOfWeek: String = "Mon,Tue,Wed,Thu,Fri,Sat,Sun" // Selected intake days
)

@Entity(tableName = "dose_logs")
data class DoseLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val scheduledTime: String,
    val dateString: String, // "YYYY-MM-DD"
    val loggedTimestamp: Long = System.currentTimeMillis(),
    val status: String, // "TAKEN", "SNOOZED", "SKIPPED", "PENDING"
    val notes: String = ""
)

@Entity(tableName = "health_diary")
data class HealthDiaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateString: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mood: String = "Good", // "Great", "Good", "Neutral", "Low", "Rough"
    val painLevel: Int = 0, // 0 - 10
    val systolicBp: Int = 120,
    val diastolicBp: Int = 80,
    val bloodSugar: Int = 95, // mg/dL
    val weightKg: Float = 70.0f,
    val symptoms: String = "",
    val notes: String = ""
)

@Entity(tableName = "doctor_appointments")
data class DoctorAppointmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val doctorName: String,
    val specialty: String,
    val dateTimeString: String,
    val clinicLocation: String,
    val reason: String,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val reminderLeadTime: String = "1_DAY_BEFORE" // "AT_TIME", "1_HOUR_BEFORE", "2_HOURS_BEFORE", "12_HOURS_BEFORE", "1_DAY_BEFORE", "2_DAYS_BEFORE"
)

@Entity(tableName = "injection_site_logs")
data class InjectionSiteLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val siteCode: String, // "ABDOMEN_LEFT", "ABDOMEN_RIGHT", "THIGH_LEFT", "THIGH_RIGHT", "ARM_LEFT", "ARM_RIGHT"
    val siteLabel: String,
    val timestamp: Long = System.currentTimeMillis()
)
