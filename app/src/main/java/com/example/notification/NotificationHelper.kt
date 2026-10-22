package com.example.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

object NotificationHelper {
    // Upgraded channel v2 configured with silent sound to avoid dual-playback conflict with AlarmPlaybackController
    const val CHANNEL_ID = "med_intake_reminders_channel"
    const val CHANNEL_ID_V2 = "med_intake_reminders_v2"
    const val CHANNEL_NAME = "Daily Medication Reminders"
    const val CHANNEL_DESC = "Push reminders scheduled with AlarmManager for daily medication doses"

    const val REFILL_CHANNEL_ID = "med_refill_alerts_channel"
    const val REFILL_CHANNEL_NAME = "Medication Refill Alerts"
    const val REFILL_CHANNEL_DESC = "Push notifications when medication quantity drops to or below your custom refill threshold"

    const val APPOINTMENT_CHANNEL_ID = "doctor_appointments_channel"
    const val APPOINTMENT_CHANNEL_NAME = "Doctor Appointment Reminders"
    const val APPOINTMENT_CHANNEL_DESC = "Scheduled reminders for upcoming doctor appointments and clinic visits"

    const val ACTION_TAKE_DOSE = "com.example.notification.ACTION_TAKE_DOSE"
    const val ACTION_STOP_ALARM = "com.example.notification.ACTION_STOP_ALARM"
    const val ACTION_SNOOZE_DOSE = "com.example.notification.ACTION_SNOOZE_DOSE"
    const val ACTION_DISMISS_ALARM = "com.example.notification.ACTION_DISMISS_ALARM"
    const val ACTION_REFILL_STOCK = "com.example.notification.ACTION_REFILL_STOCK"

    const val EXTRA_MEDICATION_ID = "extra_medication_id"
    const val EXTRA_MEDICATION_NAME = "extra_medication_name"
    const val EXTRA_DOSAGE = "extra_dosage"
    const val EXTRA_FORM = "extra_form"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_INSTRUCTIONS = "extra_instructions"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_TIME_INDEX = "extra_time_index"
    const val EXTRA_REFILL_AMOUNT = "extra_refill_amount"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Daily intake reminders channel v2: High importance, vibration enabled, sound null (managed by AlarmPlaybackController)
            val intakeChannel = NotificationChannel(
                CHANNEL_ID_V2,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = Color.parseColor("#0D9488")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 800)
                setShowBadge(true)
                // Critical: Channel does not play sound to avoid duplicate audio with MediaPlayer
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(intakeChannel)

            // Low-stock / Refill alerts channel
            val refillChannel = NotificationChannel(
                REFILL_CHANNEL_ID,
                REFILL_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = REFILL_CHANNEL_DESC
                enableLights(true)
                lightColor = Color.parseColor("#F59E0B")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(refillChannel)

            // Doctor appointment alerts channel
            val apptChannel = NotificationChannel(
                APPOINTMENT_CHANNEL_ID,
                APPOINTMENT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = APPOINTMENT_CHANNEL_DESC
                enableLights(true)
                lightColor = Color.parseColor("#2563EB")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(apptChannel)
        }
    }

    /**
     * Constructs the exact notification required by Part 4 & Part 5 of specifications:
     * Title: "Medication Reminder"
     * Body: "It is time to take your [MEDICINE NAME]."
     * SubText: "Scheduled time: [MEDICATION TIME]"
     * Gentle reminder: "This is a gentle reminder to take your [MEDICINE NAME]."
     * Action 1: "Snooze"
     * Action 2: "Mark as Taken"
     */
    fun buildMedicationNotification(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        form: String,
        scheduledTime: String,
        instructions: String,
        notificationId: Int
    ): Notification {
        createNotificationChannel(context)

        // Main Tap: Opens MainActivity
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_tab", "REMINDERS")
            putExtra(EXTRA_MEDICATION_ID, medicationId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full Screen Intent on lock screen
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_FORM, form)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_INSTRUCTIONS, instructions)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 500,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ACTION 1: SNOOZE
        val snoozeIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE_DOSE
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_FORM, form)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_INSTRUCTIONS, instructions)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ACTION 2: MARK AS TAKEN
        val takeIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_TAKE_DOSE
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Dismissal Intent: Stops audio if user clears notification
        val dismissIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigText = "It is time to take your $medicationName.\nScheduled time: $scheduledTime\nThis is a gentle reminder to take your $medicationName."

        return NotificationCompat.Builder(context, CHANNEL_ID_V2)
            .setSmallIcon(R.drawable.ic_medication_notification)
            .setContentTitle("Medication Reminder")
            .setContentText("It is time to take your $medicationName.")
            .setSubText("Scheduled time: $scheduledTime")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(Color.parseColor("#0D9488"))
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(R.drawable.ic_medication_notification, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.ic_medication_notification, "Mark as Taken", takePendingIntent)
            .build()
    }

    /**
     * Dispatches the medication reminder flow.
     * Enforces the STRICT NOTIFICATION PERMISSION RULE:
     * If notification permission is NOT granted, NO audible alarm or service starts!
     */
    fun showMedicationNotification(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        form: String,
        scheduledTime: String,
        instructions: String,
        notificationId: Int
    ) {
        if (!AlarmPlaybackController.isNotificationPermitted(context)) {
            android.util.Log.w(
                "NotificationHelper",
                "STRICT PERMISSION GATE: Notifications disabled for app/channel. Audible alarm blocked for $medicationName."
            )
            return
        }

        // Start AlarmRingtoneService in foreground with the verified notification
        AlarmRingtoneService.startAlarm(
            context = context,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            form = form,
            scheduledTime = scheduledTime,
            instructions = instructions,
            notificationId = notificationId
        )
    }

    fun showTestNotification(context: Context) {
        showMedicationNotification(
            context = context,
            medicationId = 9999L,
            medicationName = "Advil Dual",
            dosage = "250 mg / 125 mg",
            form = "Tablet",
            scheduledTime = "Now",
            instructions = "Take with water after food",
            notificationId = 9999
        )
    }

    /**
     * Dispatches a push notification when medication stock is at or below the user-defined threshold.
     */
    fun showRefillNotification(
        context: Context,
        medicationId: Long,
        medicationName: String,
        stockRemaining: Int,
        refillThreshold: Int,
        form: String,
        dosage: String
    ) {
        if (!AlarmPlaybackController.isNotificationPermitted(context)) {
            android.util.Log.w("NotificationHelper", "Refill notification skipped: notifications disabled.")
            return
        }

        createNotificationChannel(context)
        val notificationId = 20000 + medicationId.toInt().coerceAtMost(9999)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_tab", "CABINET")
            putExtra(EXTRA_MEDICATION_ID, medicationId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val refillIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = ACTION_REFILL_STOCK
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_REFILL_AMOUNT, 30)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val refillPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            refillIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val unitName = when (form.lowercase(Locale.getDefault())) {
            "liquid", "solution", "suspension", "syrup" -> "mL"
            "inhaler" -> "puffs"
            "injection" -> "doses"
            "capsule" -> "capsules"
            "tablet" -> "tablets"
            else -> "units"
        }

        val title = "Refill Required: $medicationName"
        val shortContent = "Only $stockRemaining $unitName remaining (alert threshold: $refillThreshold $unitName)."
        val bigContent = "Your inventory for $medicationName ($dosage $form) is running low.\n" +
                "• Stock remaining: $stockRemaining $unitName\n" +
                "• Your alert threshold: $refillThreshold $unitName\n" +
                "Please reorder or log a prescription refill to prevent missing scheduled doses."

        val builder = NotificationCompat.Builder(context, REFILL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication_notification)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(Color.parseColor("#F59E0B"))
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_medication_notification, "Log Refill (+30)", refillPendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
            android.util.Log.d("NotificationHelper", "Refill notification sent for $medicationName (ID $notificationId)")
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Cannot show refill notification: permission not granted", e)
        }
    }

    /**
     * Dispatches a push reminder for an upcoming doctor appointment.
     */
    fun showDoctorAppointmentNotification(
        context: Context,
        appointmentId: Long,
        doctorName: String,
        specialty: String,
        dateTimeString: String,
        clinicLocation: String,
        reason: String,
        leadTimeLabel: String = "Upcoming"
    ) {
        if (!AlarmPlaybackController.isNotificationPermitted(context)) {
            android.util.Log.w("NotificationHelper", "Doctor appointment notification skipped: notifications disabled.")
            return
        }

        createNotificationChannel(context)
        val notificationId = 30000 + (appointmentId % 10000).toInt()

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_tab", "CARE_PROFILE")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Doctor Appointment Reminder ($leadTimeLabel)"
        val shortContent = "Dr. $doctorName ($specialty) • $dateTimeString"
        val bigContent = "Upcoming appointment with Dr. $doctorName ($specialty)\n" +
                "• Date & Time: $dateTimeString\n" +
                "• Location: $clinicLocation\n" +
                "• Reason for visit: $reason"

        val builder = NotificationCompat.Builder(context, APPOINTMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_medication_notification)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setColor(Color.parseColor("#2563EB"))
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
            android.util.Log.d("NotificationHelper", "Doctor appointment reminder sent for $doctorName (ID $notificationId)")
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Cannot show doctor appointment notification: permission denied", e)
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
