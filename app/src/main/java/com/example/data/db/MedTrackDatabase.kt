package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Database(
    entities = [
        MedicationEntity::class,
        DoseLogEntity::class,
        HealthDiaryEntity::class,
        DoctorAppointmentEntity::class,
        InjectionSiteLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MedTrackDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun healthDiaryDao(): HealthDiaryDao
    abstract fun doctorAppointmentDao(): DoctorAppointmentDao
    abstract fun injectionSiteDao(): InjectionSiteDao

    companion object {
        @Volatile
        private var INSTANCE: MedTrackDatabase? = null

        fun getDatabase(context: Context): MedTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedTrackDatabase::class.java,
                    "medtrack_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(database: MedTrackDatabase) {
            val medDao = database.medicationDao()
            val doseDao = database.doseLogDao()
            val diaryDao = database.healthDiaryDao()
            val apptDao = database.doctorAppointmentDao()
            val injectionDao = database.injectionSiteDao()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = dateFormat.format(Date())

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(calendar.time)

            // 1. Initial Medications
            val starterMeds = listOf(
                MedicationEntity(
                    id = 1,
                    name = "Amoxicillin",
                    dosage = "500 mg",
                    form = "Capsule",
                    colorHex = "#0D9488",
                    instructions = "Take after breakfast with water",
                    timesOfDay = "08:00,14:00,20:00",
                    timingSlots = "Morning,Afternoon,Evening",
                    frequencyPerDay = 3,
                    frequencyDaysPerWeek = 7,
                    courseDurationDays = 10,
                    stockRemaining = 12,
                    refillThreshold = 6,
                    profileName = "Self",
                    notes = "Prescribed for respiratory infection"
                ),
                MedicationEntity(
                    id = 2,
                    name = "Metformin",
                    dosage = "850 mg",
                    form = "Tablet",
                    colorHex = "#0284C7",
                    instructions = "Take with main meals",
                    timesOfDay = "08:00,20:00",
                    timingSlots = "Morning,Evening",
                    frequencyPerDay = 2,
                    frequencyDaysPerWeek = 7,
                    courseDurationDays = 0,
                    stockRemaining = 4, // Trigger amber low stock!
                    refillThreshold = 8,
                    profileName = "Self",
                    notes = "Maintain consistent carbohydrate timing"
                ),
                MedicationEntity(
                    id = 3,
                    name = "Lisinopril",
                    dosage = "10 mg",
                    form = "Tablet",
                    colorHex = "#10B981",
                    instructions = "Take first thing in the morning",
                    timesOfDay = "08:00",
                    timingSlots = "Morning",
                    frequencyPerDay = 1,
                    frequencyDaysPerWeek = 7,
                    courseDurationDays = 0,
                    stockRemaining = 24,
                    refillThreshold = 7,
                    profileName = "Self",
                    notes = "Monitor blood pressure weekly"
                ),
                MedicationEntity(
                    id = 4,
                    name = "Vitamin D3",
                    dosage = "2000 IU",
                    form = "Capsule",
                    colorHex = "#F59E0B",
                    instructions = "Take with healthy fats",
                    timesOfDay = "08:00",
                    timingSlots = "Morning",
                    frequencyPerDay = 1,
                    frequencyDaysPerWeek = 7,
                    courseDurationDays = 90,
                    stockRemaining = 45,
                    refillThreshold = 10,
                    profileName = "Self",
                    notes = "Daily immune and bone support"
                ),
                MedicationEntity(
                    id = 5,
                    name = "Semaglutide / GLP-1",
                    dosage = "0.5 mg",
                    form = "Injection",
                    colorHex = "#8B5CF6",
                    instructions = "Subcutaneous injection once weekly",
                    timesOfDay = "20:00",
                    timingSlots = "Evening",
                    frequencyPerDay = 1,
                    frequencyDaysPerWeek = 1,
                    courseDurationDays = 0,
                    stockRemaining = 2, // Low stock warning
                    refillThreshold = 2,
                    profileName = "Self",
                    notes = "Rotate injection site each week"
                ),
                MedicationEntity(
                    id = 6,
                    name = "Melatonin",
                    dosage = "3 mg",
                    form = "Tablet",
                    colorHex = "#6366F1",
                    instructions = "30 minutes before sleep",
                    timesOfDay = "22:00",
                    timingSlots = "Bedtime",
                    frequencyPerDay = 1,
                    frequencyDaysPerWeek = 7,
                    courseDurationDays = 30,
                    stockRemaining = 18,
                    refillThreshold = 5,
                    profileName = "Self",
                    notes = "As needed for sleep cycle"
                )
            )
            medDao.insertAll(starterMeds)

            // 2. Initial Dose Logs for Today & Past Consecutive Days (28 taken doses so user can achieve 30 doses and 1-week milestones)
            val starterLogs = mutableListOf<DoseLogEntity>()

            // Today's morning doses taken (3 taken, leaving afternoon/evening/bedtime for active user interaction)
            starterLogs.addAll(
                listOf(
                    DoseLogEntity(
                        medicationId = 1,
                        medicationName = "Amoxicillin",
                        scheduledTime = "08:00",
                        dateString = todayStr,
                        loggedTimestamp = System.currentTimeMillis() - 14400000,
                        status = "TAKEN",
                        notes = "With oatmeal breakfast"
                    ),
                    DoseLogEntity(
                        medicationId = 3,
                        medicationName = "Lisinopril",
                        scheduledTime = "08:00",
                        dateString = todayStr,
                        loggedTimestamp = System.currentTimeMillis() - 14400000,
                        status = "TAKEN",
                        notes = "BP 118/78"
                    ),
                    DoseLogEntity(
                        medicationId = 4,
                        medicationName = "Vitamin D3",
                        scheduledTime = "08:00",
                        dateString = todayStr,
                        loggedTimestamp = System.currentTimeMillis() - 14000000,
                        status = "TAKEN",
                        notes = ""
                    )
                )
            )

            // Past 6 days full on-time adherence (Days -1 to -6, 4 doses/day = 24 doses + today's 3 = 27 doses taken)
            for (dayOffset in 1..6) {
                val pastCal = Calendar.getInstance()
                pastCal.add(Calendar.DAY_OF_YEAR, -dayOffset)
                val pastDateStr = dateFormat.format(pastCal.time)
                val basePastTimestamp = pastCal.timeInMillis

                starterLogs.add(
                    DoseLogEntity(
                        medicationId = 1,
                        medicationName = "Amoxicillin",
                        scheduledTime = "08:00",
                        dateString = pastDateStr,
                        loggedTimestamp = basePastTimestamp + 28800000,
                        status = "TAKEN"
                    )
                )
                starterLogs.add(
                    DoseLogEntity(
                        medicationId = 2,
                        medicationName = "Metformin",
                        scheduledTime = "08:00",
                        dateString = pastDateStr,
                        loggedTimestamp = basePastTimestamp + 29100000,
                        status = "TAKEN"
                    )
                )
                starterLogs.add(
                    DoseLogEntity(
                        medicationId = 3,
                        medicationName = "Lisinopril",
                        scheduledTime = "08:00",
                        dateString = pastDateStr,
                        loggedTimestamp = basePastTimestamp + 29400000,
                        status = "TAKEN"
                    )
                )
                starterLogs.add(
                    DoseLogEntity(
                        medicationId = 1,
                        medicationName = "Amoxicillin",
                        scheduledTime = "20:00",
                        dateString = pastDateStr,
                        loggedTimestamp = basePastTimestamp + 72000000,
                        status = "TAKEN"
                    )
                )
            }
            // Add day -7 dose to bring total starting taken doses to 28
            val day7Cal = Calendar.getInstance()
            day7Cal.add(Calendar.DAY_OF_YEAR, -7)
            val day7Str = dateFormat.format(day7Cal.time)
            starterLogs.add(
                DoseLogEntity(
                    medicationId = 1,
                    medicationName = "Amoxicillin",
                    scheduledTime = "08:00",
                    dateString = day7Str,
                    loggedTimestamp = day7Cal.timeInMillis + 28800000,
                    status = "TAKEN"
                )
            )

            doseDao.insertAll(starterLogs)

            // 3. Initial Health Diary entries (for chart rendering)
            // 3. Initial Health Diary entries (30 days for trend analytics)
            val starterDiary = mutableListOf<HealthDiaryEntity>()
            val sampleSymptoms = mapOf(
                3 to "Mild headache",
                5 to "Fatigue",
                7 to "Drowsiness",
                8 to "Mild dizziness",
                10 to "Headache",
                11 to "Mild nausea",
                12 to "Fatigue",
                14 to "Drowsiness",
                15 to "Joint stiffness",
                18 to "Headache",
                19 to "Fatigue",
                20 to "Dizziness",
                21 to "Drowsiness",
                22 to "Fatigue",
                23 to "Joint stiffness",
                25 to "Headache",
                27 to "Mild nausea",
                28 to "Fatigue"
            )

            for (i in 29 downTo 0) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val dStr = dateFormat.format(cal.time)
                val symptom = sampleSymptoms[i] ?: if (i % 4 == 0) "Feeling energetic" else "None"
                val mood = when {
                    symptom.contains("Headache") || symptom.contains("nausea") -> if (i % 2 == 0) "Low" else "Rough"
                    symptom.contains("Fatigue") || symptom.contains("dizziness") -> "Neutral"
                    i % 3 == 0 -> "Great"
                    else -> "Good"
                }
                val pain = when (mood) {
                    "Rough" -> 6
                    "Low" -> 4
                    "Neutral" -> 2
                    "Good" -> 1
                    else -> 0
                }

                starterDiary.add(
                    HealthDiaryEntity(
                        dateString = dStr,
                        timestamp = cal.timeInMillis,
                        mood = mood,
                        painLevel = pain,
                        systolicBp = 118 + ((i * 3) % 7) - 2,
                        diastolicBp = 78 + ((i * 2) % 5) - 1,
                        bloodSugar = 94 + ((i * 4) % 11) - 3,
                        weightKg = 72.8f - (29 - i) * 0.03f,
                        symptoms = symptom,
                        notes = when (mood) {
                            "Great" -> "High energy, exercised 30 mins, adherence 100%"
                            "Good" -> "Normal day, slept well, medication taken on time"
                            "Neutral" -> "Slight afternoon slump, rested and hydrated"
                            "Low" -> "Felt mild fatigue after afternoon dose, took a nap"
                            else -> "Headache after stressful morning, took doses with water"
                        }
                    )
                )
            }
            diaryDao.insertAll(starterDiary)


            // 4. Initial Doctor Appointments
            val starterAppointments = listOf(
                DoctorAppointmentEntity(
                    doctorName = "Dr. Sarah Chen, MD",
                    specialty = "Cardiology & Preventive Care",
                    dateTimeString = "Thu, Oct 22 • 10:30 AM",
                    clinicLocation = "Metro Health Pavilion, Suite 402",
                    reason = "Quarterly BP and Lisinopril medication review",
                    notes = "Bring current 30-day vitals export and question about potassium"
                ),
                DoctorAppointmentEntity(
                    doctorName = "Dr. Michael Ross, Endocrine",
                    specialty = "Endocrinology",
                    dateTimeString = "Wed, Nov 04 • 02:15 PM",
                    clinicLocation = "St. Jude Wellness Center, Bldg B",
                    reason = "Metformin & HbA1c review",
                    notes = "Fast 8 hours prior for bloodwork"
                )
            )
            apptDao.insertAll(starterAppointments)

            // 5. Initial Injection Site Log
            injectionDao.insertLog(
                InjectionSiteLogEntity(
                    medicationId = 5,
                    siteCode = "ABDOMEN_LEFT",
                    siteLabel = "Left Lower Abdomen",
                    timestamp = System.currentTimeMillis() - (5 * 24 * 3600 * 1000L)
                )
            )
        }
    }
}
