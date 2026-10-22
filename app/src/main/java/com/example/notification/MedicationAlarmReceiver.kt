package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MedicationAlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MedAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1L)
        val medicationName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_NAME) ?: "Medication"
        val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE) ?: ""
        val form = intent.getStringExtra(NotificationHelper.EXTRA_FORM) ?: "Pill"
        val scheduledTime = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_TIME) ?: "08:00"
        val instructions = intent.getStringExtra(NotificationHelper.EXTRA_INSTRUCTIONS) ?: "Take as prescribed"
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, medicationId.toInt().coerceAtLeast(100))
        val timeIndex = intent.getIntExtra(NotificationHelper.EXTRA_TIME_INDEX, 0)

        Log.d(TAG, "Alarm triggered for $medicationName ($dosage), time=$scheduledTime, reqId=$notificationId")

        // STRICT NOTIFICATION-PERMISSION GATE:
        // If notification permission is NOT granted or channel is disabled,
        // DO NOT start audible alarm, background audio, or hidden sound!
        if (!AlarmPlaybackController.isNotificationPermitted(context)) {
            Log.w(
                TAG,
                "STRICT PERMISSION GATE ENFORCED: Notification permission is disabled. Suppressing audible alarm for $medicationName."
            )
            // Schedule tomorrow's intake so when permission is re-enabled, schedule continues
            if (medicationId > 0 && medicationId != 8888L && timeIndex != 99) {
                MedicationAlarmScheduler.scheduleNextDailyAlarm(
                    context = context,
                    medicationId = medicationId,
                    medicationName = medicationName,
                    dosage = dosage,
                    form = form,
                    scheduledTime = scheduledTime,
                    instructions = instructions,
                    timeIndex = timeIndex
                )
            }
            return
        }

        // 1. Display the high-priority push reminder notification & start audio
        NotificationHelper.showMedicationNotification(
            context = context,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            form = form,
            scheduledTime = scheduledTime,
            instructions = instructions,
            notificationId = notificationId
        )

        // 2. If this is a regular scheduled alarm (not a snooze or quick test), schedule tomorrow's intake
        if (medicationId > 0 && medicationId != 8888L && timeIndex != 99) {
            MedicationAlarmScheduler.scheduleNextDailyAlarm(
                context = context,
                medicationId = medicationId,
                medicationName = medicationName,
                dosage = dosage,
                form = form,
                scheduledTime = scheduledTime,
                instructions = instructions,
                timeIndex = timeIndex
            )
        }
    }
}
