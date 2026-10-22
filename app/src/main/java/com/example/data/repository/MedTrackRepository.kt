package com.example.data.repository

import com.example.data.api.OpenFdaService
import com.example.data.api.ScannedBarcodeDrug
import com.example.data.db.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

import android.util.Log

class MedTrackRepository(
    private val medicationDao: MedicationDao,
    private val doseLogDao: DoseLogDao,
    private val healthDiaryDao: HealthDiaryDao,
    private val doctorAppointmentDao: DoctorAppointmentDao,
    private val injectionSiteDao: InjectionSiteDao,
    private val openFdaService: OpenFdaService = OpenFdaService()
) {
    companion object {
        private const val TAG = "MedTrackRepository"
    }

    /**
     * Queries external openFDA drug registry with comprehensive Logcat logging
     * of raw network responses and parsing status.
     * Returns null if no matching record is found, allowing the UI to present
     * a user-friendly error and manual entry instead of placeholder dummy data.
     */
    suspend fun lookupBarcodeDrug(rawBarcode: String, format: String): ScannedBarcodeDrug? {
        Log.d(TAG, "lookupBarcodeDrug initiated for barcode='$rawBarcode', format='$format'")
        return try {
            val result = openFdaService.lookupBarcode(rawBarcode, format)
            if (result != null) {
                Log.d(
                    TAG,
                    "lookupBarcodeDrug succeeded: Brand='${result.brandName}', Generic='${result.genericName}', Strength='${result.dosageStrength}', Form='${result.formFactor}', NDC='${result.productNdc}'"
                )
            } else {
                Log.d(TAG, "lookupBarcodeDrug: No match found for barcode '$rawBarcode'")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "lookupBarcodeDrug: Error looking up barcode '$rawBarcode': ${e.message}", e)
            throw e
        }
    }

    /**
     * Maps extracted openFDA / Barcode API drug attributes directly into
     * MedTrackDatabase local entity storage via MedicationDao.
     */
    suspend fun saveScannedDrugToDatabase(
        drug: ScannedBarcodeDrug,
        profileName: String = "Self",
        timingSlots: String = "Morning,Evening",
        timesOfDay: String = "08:00,20:00"
    ): Long {
        val entity = MedicationEntity(
            name = drug.brandName,
            dosage = drug.dosageStrength,
            form = drug.formFactor,
            instructions = drug.instructions,
            timesOfDay = timesOfDay,
            timingSlots = timingSlots,
            frequencyPerDay = timingSlots.split(",").filter { it.isNotBlank() }.size.coerceAtLeast(1),
            frequencyDaysPerWeek = 7,
            courseDurationDays = 0,
            stockRemaining = drug.defaultStock,
            refillThreshold = drug.defaultRefillThreshold,
            profileName = profileName,
            notes = "Identified via ${drug.dataSource} (${drug.barcodeFormat})",
            rxcui = drug.productNdc
        )
        return medicationDao.insertMedication(entity)
    }

    // Seed check
    suspend fun checkAndSeedDatabase(database: MedTrackDatabase) {
        if (medicationDao.getCount() == 0) {
            MedTrackDatabase.seedInitialData(database)
        }
    }

    // Medications
    val allMedications: Flow<List<MedicationEntity>> = medicationDao.getAllMedications()

    val lowStockMedications: Flow<List<MedicationEntity>> = medicationDao.getLowStockMedications()

    suspend fun getAllMedicationsSync(): List<MedicationEntity> =
        medicationDao.getAllMedicationsSync()

    suspend fun getLowStockMedicationsSync(): List<MedicationEntity> =
        medicationDao.getLowStockMedicationsSync()

    /**
     * Checks medication quantities in Room DB against defined thresholds and
     * dispatches low-stock alert push notifications for any item where quantity drops below threshold.
     */
    suspend fun checkAndTriggerLowStockAlerts(context: android.content.Context): List<MedicationEntity> {
        val lowStockList = medicationDao.getLowStockMedicationsSync()
        for (med in lowStockList) {
            com.example.notification.NotificationHelper.showRefillNotification(
                context = context,
                medicationId = med.id,
                medicationName = med.name,
                stockRemaining = med.stockRemaining,
                refillThreshold = med.refillThreshold,
                form = med.form,
                dosage = med.dosage
            )
        }
        return lowStockList
    }

    fun getMedicationsByProfile(profile: String): Flow<List<MedicationEntity>> =
        medicationDao.getMedicationsByProfile(profile)

    suspend fun getMedicationById(id: Long): MedicationEntity? =
        medicationDao.getMedicationById(id)

    suspend fun insertMedication(medication: MedicationEntity): Long =
        medicationDao.insertMedication(medication)

    suspend fun updateMedication(medication: MedicationEntity) =
        medicationDao.updateMedication(medication)

    suspend fun updateStock(id: Long, newStock: Int) =
        medicationDao.updateStock(id, newStock)

    suspend fun refillStock(id: Long, amount: Int = 30) =
        medicationDao.addStock(id, amount)

    suspend fun deleteMedication(medication: MedicationEntity) =
        medicationDao.deleteMedication(medication)

    // Dose Logs
    fun getLogsForDate(dateString: String): Flow<List<DoseLogEntity>> =
        doseLogDao.getLogsForDate(dateString)

    val allDoseLogs: Flow<List<DoseLogEntity>> = doseLogDao.getAllLogs()

    suspend fun recordDoseStatus(
        medicationId: Long,
        medicationName: String,
        scheduledTime: String,
        dateString: String,
        status: String,
        notes: String = ""
    ) {
        if (status == "TAKEN") {
            medicationDao.decrementStock(medicationId)
            val updatedMed = medicationDao.getMedicationById(medicationId)
            if (updatedMed != null && updatedMed.stockRemaining <= updatedMed.refillThreshold) {
                Log.w(
                    TAG,
                    "Stock for '${updatedMed.name}' dropped to ${updatedMed.stockRemaining} (threshold: ${updatedMed.refillThreshold}). Low stock alert active."
                )
            }
        }
        val existing = doseLogDao.getLogForDose(medicationId, dateString, scheduledTime)
        if (existing != null) {
            doseLogDao.updateLog(
                existing.copy(
                    status = status,
                    loggedTimestamp = System.currentTimeMillis(),
                    notes = notes
                )
            )
        } else {
            doseLogDao.insertLog(
                DoseLogEntity(
                    medicationId = medicationId,
                    medicationName = medicationName,
                    scheduledTime = scheduledTime,
                    dateString = dateString,
                    loggedTimestamp = System.currentTimeMillis(),
                    status = status,
                    notes = notes
                )
            )
        }
    }

    // Health Diary
    val allHealthEntries: Flow<List<HealthDiaryEntity>> = healthDiaryDao.getAllEntries()
    val recentHealthEntries: Flow<List<HealthDiaryEntity>> = healthDiaryDao.getRecentEntries()

    suspend fun logHealthEntry(entry: HealthDiaryEntity): Long =
        healthDiaryDao.insertEntry(entry)

    suspend fun deleteHealthEntry(entry: HealthDiaryEntity) =
        healthDiaryDao.deleteEntry(entry)

    // Doctor Appointments
    val allAppointments: Flow<List<DoctorAppointmentEntity>> = doctorAppointmentDao.getAllAppointments()

    suspend fun insertAppointment(appointment: DoctorAppointmentEntity): Long =
        doctorAppointmentDao.insertAppointment(appointment)

    suspend fun updateAppointment(appointment: DoctorAppointmentEntity) =
        doctorAppointmentDao.updateAppointment(appointment)

    suspend fun deleteAppointment(appointment: DoctorAppointmentEntity) =
        doctorAppointmentDao.deleteAppointment(appointment)

    // Injection Site Tracker
    val injectionLogs: Flow<List<InjectionSiteLogEntity>> = injectionSiteDao.getAllLogs()
    val latestInjectionLog: Flow<InjectionSiteLogEntity?> = injectionSiteDao.getLatestLog()

    suspend fun logInjection(siteCode: String, siteLabel: String, medicationId: Long = 5) {
        injectionSiteDao.insertLog(
            InjectionSiteLogEntity(
                medicationId = medicationId,
                siteCode = siteCode,
                siteLabel = siteLabel,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
