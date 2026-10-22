package com.example.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log

class AlarmRingtoneService : Service() {

    companion object {
        private const val TAG = "AlarmRingtoneService"
        const val ACTION_START_ALARM = "com.example.notification.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.notification.ACTION_STOP_ALARM"

        val isRinging: Boolean
            get() = AlarmPlaybackController.activeSession.value != null

        val currentMedicationName: String?
            get() = AlarmPlaybackController.activeSession.value?.medicationName

        val currentMedicationId: Long
            get() = AlarmPlaybackController.activeSession.value?.medicationId ?: -1L

        fun startAlarm(
            context: Context,
            medicationId: Long,
            medicationName: String,
            dosage: String,
            form: String,
            scheduledTime: String,
            instructions: String,
            notificationId: Int
        ) {
            // Strict permission gate: do not attempt to start service if notifications are disabled
            if (!AlarmPlaybackController.isNotificationPermitted(context)) {
                Log.w(TAG, "Notification permission check failed in startAlarm. Blocking alarm service.")
                return
            }

            val intent = Intent(context, AlarmRingtoneService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(NotificationHelper.EXTRA_MEDICATION_ID, medicationId)
                putExtra(NotificationHelper.EXTRA_MEDICATION_NAME, medicationName)
                putExtra(NotificationHelper.EXTRA_DOSAGE, dosage)
                putExtra(NotificationHelper.EXTRA_FORM, form)
                putExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, scheduledTime)
                putExtra(NotificationHelper.EXTRA_INSTRUCTIONS, instructions)
                putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, notificationId)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Sent startAlarm command to AlarmRingtoneService for $medicationName (req=$notificationId)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AlarmRingtoneService", e)
            }
        }

        fun stopAlarm(context: Context) {
            val intent = Intent(context, AlarmRingtoneService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            try {
                context.startService(intent)
                Log.d(TAG, "Sent stopAlarm command to AlarmRingtoneService")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send stop command to AlarmRingtoneService", e)
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MedTrack:AlarmWakeLock")?.apply {
                acquire(5 * 60 * 1000L) // 5 minutes max
            }
            Log.d(TAG, "Acquired partial WakeLock for alarm playback")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "Released alarm WakeLock")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        } finally {
            wakeLock = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_ALARM) {
            Log.d(TAG, "Received ACTION_STOP_ALARM - stopping alarm playback and service")
            stopPlaybackAndService()
            return START_NOT_STICKY
        }

        if (action == ACTION_START_ALARM) {
            // Strict Gate check
            if (!AlarmPlaybackController.isNotificationPermitted(applicationContext)) {
                Log.w(TAG, "Notification permission missing during onStartCommand - terminating service")
                stopPlaybackAndService()
                return START_NOT_STICKY
            }

            val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1L)
            val medicationName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_NAME) ?: "Medication"
            val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE) ?: ""
            val form = intent.getStringExtra(NotificationHelper.EXTRA_FORM) ?: "Pill"
            val scheduledTime = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_TIME) ?: "Now"
            val instructions = intent.getStringExtra(NotificationHelper.EXTRA_INSTRUCTIONS) ?: ""
            val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 1001)

            // 1. Build and post foreground notification
            val notification = NotificationHelper.buildMedicationNotification(
                context = applicationContext,
                medicationId = medicationId,
                medicationName = medicationName,
                dosage = dosage,
                form = form,
                scheduledTime = scheduledTime,
                instructions = instructions,
                notificationId = notificationId
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        notificationId,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(notificationId, notification)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service in foreground - cannot post visible notification. Halting audio.", e)
                stopPlaybackAndService()
                return START_NOT_STICKY
            }

            // 2. Delegate audio and vibration to AlarmPlaybackController
            val started = AlarmPlaybackController.startAlarmSession(
                context = applicationContext,
                medicationId = medicationId,
                medicationName = medicationName,
                dosage = dosage,
                form = form,
                scheduledTime = scheduledTime,
                instructions = instructions,
                notificationId = notificationId
            )

            if (!started) {
                Log.w(TAG, "AlarmPlaybackController suppressed alarm audio")
            }
        }

        return START_NOT_STICKY
    }

    private fun stopPlaybackAndService() {
        AlarmPlaybackController.stopPlaybackOnly(applicationContext)
        releaseWakeLock()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping foreground", e)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmPlaybackController.stopPlaybackOnly(applicationContext)
        releaseWakeLock()
        Log.d(TAG, "AlarmRingtoneService destroyed")
    }
}
