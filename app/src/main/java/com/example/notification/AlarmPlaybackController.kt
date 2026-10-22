package com.example.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.db.MedTrackDatabase
import com.example.data.model.DoseLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Central, reliable alarm-playback controller for MedTrack.
 *
 * Invariants enforced:
 * 1. Strict Notification-Permission Gate: An audible medication alarm NEVER starts
 *    unless the application is currently permitted to post the visible notification.
 * 2. Silent & Vibrate Mode Respect: When phone is in Silent Mode, ringtone is NOT played.
 *    Only vibration occurs if enabled. When in Vibrate Mode, only vibration occurs.
 * 3. Single Source of Truth for Ringtone: The user's selected preset or custom uploaded
 *    file is played exclusively without competing notification-channel duplicate sounds.
 * 4. Safe Stopping: Any stop action (Snooze, Mark as Taken, Dismiss, In-App Stop,
 *    Physical Volume Button) terminates the active audio playback and vibrator completely.
 */
object AlarmPlaybackController {
    private const val TAG = "AlarmPlaybackController"

    enum class PhoneRingerState {
        NORMAL,
        VIBRATE,
        SILENT
    }

    data class ActiveAlarmSession(
        val medicationId: Long,
        val medicationName: String,
        val dosage: String,
        val form: String,
        val scheduledTime: String,
        val instructions: String,
        val notificationId: Int,
        val ringtoneTitle: String,
        val isAudible: Boolean,
        val isVibrating: Boolean,
        val startedAtMillis: Long = System.currentTimeMillis()
    )

    private val _activeSession = MutableStateFlow<ActiveAlarmSession?>(null)
    val activeSession: StateFlow<ActiveAlarmSession?> = _activeSession.asStateFlow()

    @Volatile
    var lastSuppressionReason: String? = null
        private set

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var safetyTimeoutRunnable: Runnable? = null

    /**
     * Checks whether medication notifications can visibly be posted to the user.
     * Evaluates package-level permission, Android 13+ POST_NOTIFICATIONS runtime grant,
     * and notification channel importance.
     */
    fun isNotificationPermitted(context: Context): Boolean {
        // 1. Package-level notification enabled check
        val nmCompat = NotificationManagerCompat.from(context)
        if (!nmCompat.areNotificationsEnabled()) {
            Log.w(TAG, "Notification permission check failed: areNotificationsEnabled() is false")
            return false
        }

        // 2. Android 13+ (API 33+) runtime POST_NOTIFICATIONS check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w(TAG, "Notification permission check failed: POST_NOTIFICATIONS is not granted")
                return false
            }
        }

        // 3. Notification Channel enabled check (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (nm != null) {
                // Ensure channel is initialized
                NotificationHelper.createNotificationChannel(context)
                val channel = nm.getNotificationChannel(NotificationHelper.CHANNEL_ID_V2)
                if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                    Log.w(TAG, "Notification permission check failed: notification channel is BLOCKED")
                    return false
                }
            }
        }

        return true
    }

    /**
     * Checks if exact alarm scheduling is allowed on Android 12+ (API 31+).
     */
    fun isExactAlarmAllowed(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    /**
     * Inspects current device ringer mode.
     */
    fun getPhoneRingerState(context: Context): PhoneRingerState {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return when (am?.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> PhoneRingerState.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> PhoneRingerState.VIBRATE
            else -> PhoneRingerState.NORMAL
        }
    }

    /**
     * Checks if vibration is available on the hardware.
     */
    fun hasVibrator(context: Context): Boolean {
        val v = getVibratorInstance(context)
        return v?.hasVibrator() == true
    }

    private fun getVibratorInstance(context: Context): Vibrator? {
        if (vibrator != null) return vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        return vibrator
    }

    /**
     * Initiates the central alarm playback session for a medication dose.
     * Enforces the notification permission gate and ringer mode invariants.
     */
    @Synchronized
    fun startAlarmSession(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        form: String,
        scheduledTime: String,
        instructions: String,
        notificationId: Int
    ): Boolean {
        // STRICT GATE: Do not start audible alarm if notification is disabled
        if (!isNotificationPermitted(context)) {
            val reason = "Medication notifications are disabled. Enable notifications to receive medication reminders and alarm sounds."
            lastSuppressionReason = reason
            Log.w(TAG, "SUPPRESSED ALARM: $reason (Medication: $medicationName, reqId=$notificationId)")
            return false
        }

        lastSuppressionReason = null
        stopPlaybackOnly(context)

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val ringerState = getPhoneRingerState(context)
        val selectedRingtone = AlertRingtoneManager.getSelectedRingtone(context)

        val shouldPlayAudio = (ringerState == PhoneRingerState.NORMAL)
        val shouldVibrate = (ringerState != PhoneRingerState.SILENT) || hasVibrator(context)

        Log.i(
            TAG,
            "Starting alarm session for $medicationName (ID $medicationId). RingerState=$ringerState, PlayAudio=$shouldPlayAudio, Vibrate=$shouldVibrate, Ringtone=${selectedRingtone.title}"
        )

        val session = ActiveAlarmSession(
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            form = form,
            scheduledTime = scheduledTime,
            instructions = instructions,
            notificationId = notificationId,
            ringtoneTitle = selectedRingtone.title,
            isAudible = shouldPlayAudio,
            isVibrating = shouldVibrate
        )
        _activeSession.value = session

        // 1. Play Audio if allowed by normal ringer mode
        if (shouldPlayAudio) {
            requestAudioFocus(context)
            playSelectedRingtone(context, selectedRingtone)
        }

        // 2. Start Vibration if permitted
        if (shouldVibrate) {
            startVibration(context)
        }

        // 3. Safety auto-timeout: Silence audio after 5 minutes of unattended ringing
        safetyTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        safetyTimeoutRunnable = Runnable {
            Log.w(TAG, "5-minute safety timeout reached for $medicationName - silencing audio playback")
            stopPlaybackOnly(context)
        }
        mainHandler.postDelayed(safetyTimeoutRunnable!, 5 * 60 * 1000L)

        return true
    }

    private fun playSelectedRingtone(context: Context, ringtone: AlertRingtone) {
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            var configured = false

            // Preset resolution
            if (ringtone.isPreset) {
                val resId = when (ringtone.id) {
                    AlertRingtoneManager.ID_PRESET_CLASSIC -> R.raw.preset_classic
                    AlertRingtoneManager.ID_PRESET_URGENT -> R.raw.preset_urgent
                    else -> R.raw.preset_gentle
                }
                try {
                    val afd = context.resources.openRawResourceFd(resId)
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    configured = true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load preset resource $resId", e)
                }
            } else {
                // Custom local uploaded file
                try {
                    val uri = Uri.parse(ringtone.uriString)
                    val filePath = uri.path
                    if (filePath != null) {
                        val file = File(filePath)
                        if (file.exists()) {
                            val fis = FileInputStream(file)
                            mp.setDataSource(fis.fd)
                            fis.close()
                            configured = true
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load custom ringtone file ${ringtone.uriString}", e)
                }
            }

            // Fallback if loading failed
            if (!configured) {
                Log.w(TAG, "Falling back to gentle preset audio descriptor")
                val afd = context.resources.openRawResourceFd(R.raw.preset_gentle)
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            }

            mp.isLooping = true
            mp.prepare()
            mp.start()
            mediaPlayer = mp
            Log.d(TAG, "MediaPlayer successfully started looping playback for ${ringtone.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error starting ringtone playback", e)
            stopAudioPlayer()
        }
    }

    private fun startVibration(context: Context) {
        try {
            val vib = getVibratorInstance(context) ?: return
            val pattern = longArrayOf(0, 600, 300, 600, 300, 1000, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibration pattern", e)
        }
    }

    private fun requestAudioFocus(context: Context) {
        try {
            if (audioManager == null) {
                audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            Log.d(TAG, "Audio focus lost - stopping alarm audio")
                            stopPlaybackOnly(context)
                        }
                    }
                    .build()
                audioFocusRequest = focusReq
                audioManager?.requestAudioFocus(focusReq)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            stopPlaybackOnly(context)
                        }
                    },
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus", e)
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error abandoning audio focus", e)
        } finally {
            audioFocusRequest = null
        }
    }

    private fun stopAudioPlayer() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
    }

    /**
     * Stops audio and vibration without resetting the session details.
     */
    fun stopPlaybackOnly(context: Context) {
        safetyTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        safetyTimeoutRunnable = null
        stopAudioPlayer()
        try {
            getVibratorInstance(context)?.cancel()
        } catch (_: Exception) {}
        abandonAudioFocus()
    }

    /**
     * Completely stops active alarm playback, clears active session,
     * terminates foreground service, and dismisses active notification.
     */
    fun stopActiveAlarm(context: Context, reason: String = "User Stop") {
        Log.i(TAG, "stopActiveAlarm called. Reason: $reason")
        stopPlaybackOnly(context)

        val currentSession = _activeSession.value
        _activeSession.value = null

        // Stop Foreground Service
        AlarmRingtoneService.stopAlarm(context)

        // Dismiss notification
        currentSession?.let {
            NotificationHelper.cancelNotification(context, it.notificationId)
        }
    }

    /**
     * Snooze action: Stops audio immediately, reschedules dose reminder for +10 minutes,
     * and clears active session.
     */
    fun snoozeActiveAlarm(context: Context, snoozeMinutes: Int = 10) {
        val session = _activeSession.value ?: return
        Log.i(TAG, "Snoozing alarm for ${session.medicationName} ($snoozeMinutes min)")
        stopPlaybackOnly(context)
        _activeSession.value = null
        AlarmRingtoneService.stopAlarm(context)
        NotificationHelper.cancelNotification(context, session.notificationId)

        MedicationAlarmScheduler.scheduleSnoozeAlarm(
            context = context,
            medicationId = session.medicationId,
            medicationName = session.medicationName,
            dosage = session.dosage,
            form = session.form,
            scheduledTime = session.scheduledTime,
            instructions = session.instructions,
            snoozeMinutes = snoozeMinutes
        )
    }

    /**
     * Mark as Taken action: Stops audio, records dose in Room DB, decrements stock,
     * and dismisses notification.
     */
    fun markActiveAlarmTaken(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        val session = _activeSession.value ?: return
        Log.i(TAG, "Marking active alarm as TAKEN for ${session.medicationName}")
        stopPlaybackOnly(context)
        _activeSession.value = null
        AlarmRingtoneService.stopAlarm(context)
        NotificationHelper.cancelNotification(context, session.notificationId)

        scope.launch {
            try {
                val db = MedTrackDatabase.getDatabase(context)
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                db.medicationDao().decrementStock(session.medicationId)

                val existingLog = db.doseLogDao().getLogForDose(session.medicationId, todayDate, session.scheduledTime)
                if (existingLog != null) {
                    db.doseLogDao().updateLog(
                        existingLog.copy(
                            status = "TAKEN",
                            loggedTimestamp = System.currentTimeMillis(),
                            notes = "Taken via MedTrack alarm action"
                        )
                    )
                } else {
                    db.doseLogDao().insertLog(
                        DoseLogEntity(
                            medicationId = session.medicationId,
                            medicationName = session.medicationName,
                            scheduledTime = session.scheduledTime,
                            dateString = todayDate,
                            loggedTimestamp = System.currentTimeMillis(),
                            status = "TAKEN",
                            notes = "Taken via MedTrack alarm action"
                        )
                    )
                }
                Log.d(TAG, "Successfully recorded dose as TAKEN for ${session.medicationName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error recording dose as taken", e)
            }
        }
    }
}
