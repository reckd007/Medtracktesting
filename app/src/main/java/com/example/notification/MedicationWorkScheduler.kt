package com.example.notification

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.model.MedicationEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility class for scheduling, enqueuing, and managing WorkManager tasks for medication reminders.
 */
object MedicationWorkScheduler {
    private const val TAG = "MedicationWorkScheduler"

    private const val PERIODIC_WORK_NAME = "periodic_medication_dose_scanner"
    private const val UNIQUE_WORK_PREFIX = "med_dose_work_"

    /**
     * Enqueues a WorkManager task for a specific medication dose scheduled time.
     */
    fun scheduleDoseWork(
        context: Context,
        medication: MedicationEntity,
        timeIndex: Int,
        timeString: String
    ) {
        val workManager = WorkManager.getInstance(context)
        val timeParts = timeString.trim().split(":")
        if (timeParts.size < 2) return

        val hour = timeParts[0].toIntOrNull() ?: return
        val minute = timeParts[1].toIntOrNull() ?: return

        val now = Calendar.getInstance()
        val scheduledCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (scheduledCal.before(now)) {
            scheduledCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delayMillis = scheduledCal.timeInMillis - now.timeInMillis
        val uniqueWorkName = "${UNIQUE_WORK_PREFIX}${medication.id}_$timeIndex"

        val inputData = Data.Builder()
            .putLong(MedicationReminderWorker.KEY_MEDICATION_ID, medication.id)
            .putString(MedicationReminderWorker.KEY_MEDICATION_NAME, medication.name)
            .putString(MedicationReminderWorker.KEY_DOSAGE, medication.dosage)
            .putString(MedicationReminderWorker.KEY_FORM, medication.form)
            .putString(MedicationReminderWorker.KEY_SCHEDULED_TIME, timeString)
            .putString(MedicationReminderWorker.KEY_INSTRUCTIONS, medication.instructions)
            .putBoolean(MedicationReminderWorker.KEY_IS_PERIODIC_CHECK, false)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "Enqueued WorkManager task '$uniqueWorkName' for '${medication.name}' at $timeString with delay ${delayMillis / 1000}s")
    }

    /**
     * Schedules a periodic background scan (runs every 1 hour) using WorkManager
     * to check for any pending due medication doses or low-stock items.
     */
    fun schedulePeriodicCheck(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val inputData = Data.Builder()
            .putBoolean(MedicationReminderWorker.KEY_IS_PERIODIC_CHECK, true)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
            1, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )

        Log.d(TAG, "Enqueued periodic WorkManager scanner task '$PERIODIC_WORK_NAME'")
    }

    /**
     * Reschedules all WorkManager tasks for active medications.
     */
    fun rescheduleAllMedicationWork(context: Context, medications: List<MedicationEntity>) {
        schedulePeriodicCheck(context)
        medications.forEach { med ->
            val times = med.timesOfDay.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            times.forEachIndexed { idx, timeStr ->
                scheduleDoseWork(context, med, idx, timeStr)
            }
        }
        Log.d(TAG, "Rescheduled WorkManager tasks for ${medications.size} medications")
    }

    /**
     * Cancels WorkManager tasks for a specific medication.
     */
    fun cancelDoseWork(context: Context, medicationId: Long) {
        val workManager = WorkManager.getInstance(context)
        for (i in 0..5) {
            val uniqueWorkName = "${UNIQUE_WORK_PREFIX}${medicationId}_$i"
            workManager.cancelUniqueWork(uniqueWorkName)
        }
        Log.d(TAG, "Cancelled WorkManager tasks for medicationId=$medicationId")
    }

    /**
     * Enqueues an immediate test WorkManager task firing in specified delaySeconds (e.g. 5 seconds).
     */
    fun scheduleTestWorkTask(
        context: Context,
        delaySeconds: Int = 5,
        medicationName: String = "Amoxicillin Test Dose"
    ) {
        val workManager = WorkManager.getInstance(context)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val nowTimeStr = timeFormat.format(System.currentTimeMillis() + (delaySeconds * 1000L))

        val inputData = Data.Builder()
            .putLong(MedicationReminderWorker.KEY_MEDICATION_ID, 1L)
            .putString(MedicationReminderWorker.KEY_MEDICATION_NAME, medicationName)
            .putString(MedicationReminderWorker.KEY_DOSAGE, "500 mg")
            .putString(MedicationReminderWorker.KEY_FORM, "Capsule")
            .putString(MedicationReminderWorker.KEY_SCHEDULED_TIME, nowTimeStr)
            .putString(MedicationReminderWorker.KEY_INSTRUCTIONS, "Take after meals with water")
            .putBoolean(MedicationReminderWorker.KEY_IS_PERIODIC_CHECK, false)
            .build()

        val testWorkRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delaySeconds.toLong(), TimeUnit.SECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            "test_workmanager_task_${System.currentTimeMillis()}",
            ExistingWorkPolicy.REPLACE,
            testWorkRequest
        )

        Log.d(TAG, "Enqueued immediate test WorkManager task firing in ${delaySeconds}s")
    }
}
