package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.MedicationEntity
import java.util.Calendar

object MedicationAlarmScheduler {
    private const val TAG = "MedAlarmScheduler"
    const val ACTION_ALARM_TRIGGER = "com.example.notification.ALARM_TRIGGER"

    /**
     * Calculates request code for a specific medication and its time slot index.
     */
    fun getRequestCode(medicationId: Long, timeIndex: Int): Int {
        return ((medicationId % 10000) * 100 + (timeIndex % 100)).toInt()
    }

    fun parseDaysOfWeek(daysStr: String?): Set<Int> {
        if (daysStr.isNullOrBlank() || daysStr.equals("Everyday", ignoreCase = true) || daysStr.equals("Daily", ignoreCase = true)) {
            return (1..7).toSet()
        }
        val set = mutableSetOf<Int>()
        daysStr.split(",").map { it.trim().lowercase() }.forEach {
            when {
                it.startsWith("sun") -> set.add(Calendar.SUNDAY)
                it.startsWith("mon") -> set.add(Calendar.MONDAY)
                it.startsWith("tue") -> set.add(Calendar.TUESDAY)
                it.startsWith("wed") -> set.add(Calendar.WEDNESDAY)
                it.startsWith("thu") -> set.add(Calendar.THURSDAY)
                it.startsWith("fri") -> set.add(Calendar.FRIDAY)
                it.startsWith("sat") -> set.add(Calendar.SATURDAY)
            }
        }
        return if (set.isEmpty()) (1..7).toSet() else set
    }

    /**
     * Calculates the next calendar timestamp for a scheduled dose matching the user's
     * configured intake days of the week.
     */
    fun findNextIntakeCalendar(hour: Int, minute: Int, daysOfWeekStr: String?, allowToday: Boolean = true): Calendar {
        val allowedDays = parseDaysOfWeek(daysOfWeekStr)
        val now = System.currentTimeMillis()
        val candidate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!allowToday || candidate.timeInMillis <= now) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Advance until a day matching user's selected days of the week is reached
        for (i in 0..7) {
            val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
            if (allowedDays.contains(dayOfWeek)) {
                break
            }
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }

        return candidate
    }

    /**
     * Schedules daily AlarmManager reminders for all intake times of a medication.
     */
    fun scheduleMedicationAlarms(context: Context, medication: MedicationEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val timesStr = medication.timesOfDay ?: "08:00"
        val times = timesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (times.isEmpty()) {
            Log.w(TAG, "No times configured for medication ${medication.name}")
            return
        }

        val medName = medication.name.ifBlank { "Medication" }
        val medDays = medication.daysOfWeek ?: "Mon,Tue,Wed,Thu,Fri,Sat,Sun"

        times.forEachIndexed { index, timeStr ->
            try {
                val parts = timeStr.split(":")
                if (parts.size >= 2) {
                    val hour = parts[0].toIntOrNull() ?: 8
                    val minute = parts[1].toIntOrNull() ?: 0

                    val targetCalendar = findNextIntakeCalendar(
                        hour = hour,
                        minute = minute,
                        daysOfWeekStr = medDays,
                        allowToday = true
                    )

                    val requestCode = getRequestCode(medication.id, index)
                    val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                        action = ACTION_ALARM_TRIGGER
                        putExtra(NotificationHelper.EXTRA_MEDICATION_ID, medication.id)
                        putExtra(NotificationHelper.EXTRA_MEDICATION_NAME, medName)
                        putExtra(NotificationHelper.EXTRA_DOSAGE, medication.dosage ?: "")
                        putExtra(NotificationHelper.EXTRA_FORM, medication.form ?: "Tablet / Capsule")
                        putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, timeStr)
                        putExtra(NotificationHelper.EXTRA_INSTRUCTIONS, medication.instructions ?: "Take as prescribed")
                        putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, requestCode)
                        putExtra(NotificationHelper.EXTRA_TIME_INDEX, index)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    setExactOrAllowedAlarm(alarmManager, targetCalendar.timeInMillis, pendingIntent)

                    Log.d(
                        TAG,
                        "Scheduled alarm for $medName ($timeStr) at timestamp ${targetCalendar.time} (reqCode=$requestCode, days=$medDays)"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule alarm for $medName at $timeStr", e)
            }
        }
    }

    /**
     * Reschedules the next occurrence once the current alarm fires,
     * maintaining an uninterrupted intake schedule matching the selected days of week.
     */
    fun scheduleNextDailyAlarm(
        context: Context,
        medicationId: Long,
        medicationName: String?,
        dosage: String?,
        form: String?,
        scheduledTime: String?,
        instructions: String?,
        timeIndex: Int,
        daysOfWeek: String? = "Mon,Tue,Wed,Thu,Fri,Sat,Sun"
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val validTime = scheduledTime ?: "08:00"
        val parts = validTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val nextCalendar = findNextIntakeCalendar(
            hour = hour,
            minute = minute,
            daysOfWeekStr = daysOfWeek,
            allowToday = false
        )

        val requestCode = getRequestCode(medicationId, timeIndex)
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(NotificationHelper.EXTRA_MEDICATION_ID, medicationId)
            putExtra(NotificationHelper.EXTRA_MEDICATION_NAME, medicationName ?: "Medication")
            putExtra(NotificationHelper.EXTRA_DOSAGE, dosage ?: "")
            putExtra(NotificationHelper.EXTRA_FORM, form ?: "Tablet / Capsule")
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, validTime)
            putExtra(NotificationHelper.EXTRA_INSTRUCTIONS, instructions ?: "Take as prescribed")
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(NotificationHelper.EXTRA_TIME_INDEX, timeIndex)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactOrAllowedAlarm(alarmManager, nextCalendar.timeInMillis, pendingIntent)
        Log.d(TAG, "Scheduled next occurrence for $medicationName ($scheduledTime) at ${nextCalendar.time}")
    }

    /**
     * Schedules a configurable snooze alarm (default 10 minutes).
     */
    fun scheduleSnoozeAlarm(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        form: String,
        scheduledTime: String,
        instructions: String,
        snoozeMinutes: Int = 10
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snoozeTimeMs = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val requestCode = ((medicationId % 10000) * 100 + 99).toInt() // dedicated snooze requestCode

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(NotificationHelper.EXTRA_MEDICATION_ID, medicationId)
            putExtra(NotificationHelper.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(NotificationHelper.EXTRA_DOSAGE, dosage)
            putExtra(NotificationHelper.EXTRA_FORM, form)
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, "$scheduledTime (Snoozed)")
            putExtra(NotificationHelper.EXTRA_INSTRUCTIONS, instructions)
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(NotificationHelper.EXTRA_TIME_INDEX, 99)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactOrAllowedAlarm(alarmManager, snoozeTimeMs, pendingIntent)
        Log.d(TAG, "Scheduled snooze alarm for $medicationName in $snoozeMinutes minutes")
    }

    /**
     * Cancels all scheduled alarms for a specific medication.
     */
    fun cancelMedicationAlarms(context: Context, medication: MedicationEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancel up to 10 possible time slots + snooze slot 99
        val slotIndices = (0..10).toList() + listOf(99)
        slotIndices.forEach { index ->
            val requestCode = getRequestCode(medication.id, index)
            val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                action = ACTION_ALARM_TRIGGER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Cancelled alarm reqCode=$requestCode for ${medication.name}")
            }
        }
    }

    /**
     * Reschedules daily alarms for all medications in the database (e.g. after device boot).
     */
    fun rescheduleAllAlarms(context: Context, medications: List<MedicationEntity>) {
        Log.d(TAG, "Rescheduling daily alarms for ${medications.size} medications")
        medications.forEach { med ->
            scheduleMedicationAlarms(context, med)
        }
    }

    /**
     * Schedules a quick test alarm using AlarmManager to fire in `delaySeconds` seconds.
     */
    fun scheduleQuickTestAlarm(context: Context, delaySeconds: Int = 3) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = System.currentTimeMillis() + (delaySeconds * 1000L)
        val requestCode = 8888

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(NotificationHelper.EXTRA_MEDICATION_ID, 8888L)
            putExtra(NotificationHelper.EXTRA_MEDICATION_NAME, "Test Dose: Amoxicillin")
            putExtra(NotificationHelper.EXTRA_DOSAGE, "500 mg")
            putExtra(NotificationHelper.EXTRA_FORM, "Capsule")
            putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, "Now")
            putExtra(NotificationHelper.EXTRA_INSTRUCTIONS, "Take with plenty of water")
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(NotificationHelper.EXTRA_TIME_INDEX, 0)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactOrAllowedAlarm(alarmManager, triggerAt, pendingIntent)
        Log.d(TAG, "Quick test alarm scheduled to fire in $delaySeconds seconds via AlarmManager")
    }

    private fun setExactOrAllowedAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
