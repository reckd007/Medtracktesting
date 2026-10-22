package com.example.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.MedTrackDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WorkManager CoroutineWorker for reliable background medication reminders,
 * dose checking, and low-stock refill notifications.
 */
class MedicationReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "MedicationReminderWorker"

        const val KEY_MEDICATION_ID = "key_medication_id"
        const val KEY_MEDICATION_NAME = "key_medication_name"
        const val KEY_DOSAGE = "key_dosage"
        const val KEY_FORM = "key_form"
        const val KEY_SCHEDULED_TIME = "key_scheduled_time"
        const val KEY_INSTRUCTIONS = "key_instructions"
        const val KEY_IS_PERIODIC_CHECK = "key_is_periodic_check"
    }

    override suspend fun doWork(): Result {
        return try {
            val medId = inputData.getLong(KEY_MEDICATION_ID, -1L)
            val medName = inputData.getString(KEY_MEDICATION_NAME) ?: "Medication"
            val dosage = inputData.getString(KEY_DOSAGE) ?: ""
            val form = inputData.getString(KEY_FORM) ?: "Tablet"
            val scheduledTime = inputData.getString(KEY_SCHEDULED_TIME) ?: "08:00"
            val instructions = inputData.getString(KEY_INSTRUCTIONS) ?: "Take as directed"
            val isPeriodicCheck = inputData.getBoolean(KEY_IS_PERIODIC_CHECK, false)

            Log.d(TAG, "doWork started: medId=$medId, name='$medName', time='$scheduledTime', isPeriodic=$isPeriodicCheck")

            val database = MedTrackDatabase.getDatabase(applicationContext)
            val doseDao = database.doseLogDao()
            val medDao = database.medicationDao()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = dateFormat.format(Date())

            if (isPeriodicCheck) {
                // Periodic WorkManager task: scan for all pending due doses today and low stock
                val allMeds = medDao.getAllMedicationsSync()
                val todayLogs = doseDao.getLogForDose(0, todayStr, "") // query if needed
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val nowStr = timeFormat.format(Date())

                var notificationsSent = 0
                allMeds.forEach { med ->
                    val times = med.timesOfDay.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    times.forEachIndexed { idx, tStr ->
                        val log = doseDao.getLogForDose(med.id, todayStr, tStr)
                        if (log == null || (log.status != "TAKEN" && log.status != "SKIPPED")) {
                            // Check if dose time has arrived or passed
                            if (tStr <= nowStr) {
                                val notifId = (med.id * 100 + idx).toInt()
                                NotificationHelper.showMedicationNotification(
                                    context = applicationContext,
                                    medicationId = med.id,
                                    medicationName = med.name,
                                    dosage = med.dosage,
                                    form = med.form,
                                    scheduledTime = tStr,
                                    instructions = med.instructions,
                                    notificationId = notifId
                                )
                                notificationsSent++
                            }
                        }
                    }
                }

                // Check low stock
                val lowStockList = medDao.getLowStockMedicationsSync()
                lowStockList.forEach { med ->
                    NotificationHelper.showRefillNotification(
                        context = applicationContext,
                        medicationId = med.id,
                        medicationName = med.name,
                        stockRemaining = med.stockRemaining,
                        refillThreshold = med.refillThreshold,
                        form = med.form,
                        dosage = med.dosage
                    )
                }

                Log.d(TAG, "Periodic WorkManager scan completed: sent $notificationsSent dose alerts, ${lowStockList.size} refill alerts")
            } else if (medId > 0L) {
                // Unique/Specific dose reminder task
                val existingLog = doseDao.getLogForDose(medId, todayStr, scheduledTime)
                if (existingLog == null || (existingLog.status != "TAKEN" && existingLog.status != "SKIPPED")) {
                    val notifId = (medId * 100 + (scheduledTime.hashCode() % 90)).toInt().coerceAtLeast(1)
                    NotificationHelper.showMedicationNotification(
                        context = applicationContext,
                        medicationId = medId,
                        medicationName = medName,
                        dosage = dosage,
                        form = form,
                        scheduledTime = scheduledTime,
                        instructions = instructions,
                        notificationId = notifId
                    )
                    Log.d(TAG, "Dispatched WorkManager notification for '$medName' at $scheduledTime")
                } else {
                    Log.d(TAG, "Dose for '$medName' at $scheduledTime already ${existingLog.status}, skipping notification.")
                }
            } else {
                // General test reminder
                NotificationHelper.showMedicationNotification(
                    context = applicationContext,
                    medicationId = 1L,
                    medicationName = medName,
                    dosage = dosage.ifBlank { "500 mg" },
                    form = form.ifBlank { "Capsule" },
                    scheduledTime = scheduledTime,
                    instructions = instructions,
                    notificationId = (System.currentTimeMillis() % 10000).toInt()
                )
                Log.d(TAG, "Dispatched WorkManager test reminder notification")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "MedicationReminderWorker error: ${e.message}", e)
            Result.retry()
        }
    }
}
