package com.example.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.R
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

data class AlertRingtone(
    val id: String,
    val title: String,
    val uriString: String,
    val description: String,
    val isPreset: Boolean = true
)

object AlertRingtoneManager {
    private const val TAG = "AlertRingtoneManager"
    private const val PREF_NAME = "medtrack_ringtone_prefs"
    private const val KEY_RINGTONE_ID = "selected_ringtone_id"
    private const val KEY_RINGTONE_TITLE = "selected_ringtone_title"
    private const val KEY_RINGTONE_URI = "selected_ringtone_uri"
    private const val KEY_IS_PRESET = "selected_is_preset"

    const val ID_PRESET_GENTLE = "PRESET_GENTLE"
    const val ID_PRESET_CLASSIC = "PRESET_CLASSIC"
    const val ID_PRESET_URGENT = "PRESET_URGENT"
    const val ID_CUSTOM_UPLOADED = "CUSTOM_UPLOADED"

    private var previewMediaPlayer: MediaPlayer? = null
    private var currentlyPlayingUri: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var previewTimeoutRunnable: Runnable? = null
    private var onPreviewStoppedListener: (() -> Unit)? = null

    /**
     * Returns the 3 preset alarm ringtones.
     */
    fun getPresetRingtones(context: Context): List<AlertRingtone> {
        val pkg = context.packageName
        return listOf(
            AlertRingtone(
                id = ID_PRESET_GENTLE,
                title = "Gentle Reminder",
                uriString = "android.resource://$pkg/${R.raw.preset_gentle}",
                description = "Soft, soothing chime & bell arpeggio for calm reminders (15s)",
                isPreset = true
            ),
            AlertRingtone(
                id = ID_PRESET_CLASSIC,
                title = "Classic Alarm",
                uriString = "android.resource://$pkg/${R.raw.preset_classic}",
                description = "Crisp electronic dual-pulse alarm clock beeps (15s)",
                isPreset = true
            ),
            AlertRingtone(
                id = ID_PRESET_URGENT,
                title = "Urgent Medicine Reminder",
                uriString = "android.resource://$pkg/${R.raw.preset_urgent}",
                description = "Urgent medical telemetry alert pulses for critical doses (15s)",
                isPreset = true
            )
        )
    }

    /**
     * Gets the currently active selected ringtone (preset or uploaded custom file).
     */
    fun getSelectedRingtone(context: Context): AlertRingtone {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val presets = getPresetRingtones(context)

        val id = prefs.getString(KEY_RINGTONE_ID, ID_PRESET_GENTLE) ?: ID_PRESET_GENTLE
        val savedTitle = prefs.getString(KEY_RINGTONE_TITLE, null)
        val savedUri = prefs.getString(KEY_RINGTONE_URI, null)
        val isPreset = prefs.getBoolean(KEY_IS_PRESET, true)

        if (id == ID_CUSTOM_UPLOADED && savedUri != null) {
            // Verify if local file still exists
            try {
                val uri = Uri.parse(savedUri)
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: "")
                    if (!file.exists()) {
                        Log.w(TAG, "Custom ringtone file not found at $savedUri, falling back to Gentle Reminder")
                        return presets.first()
                    }
                }
                return AlertRingtone(
                    id = ID_CUSTOM_UPLOADED,
                    title = savedTitle ?: "Uploaded Ringtone",
                    uriString = savedUri,
                    description = "Custom uploaded local ringtone from device",
                    isPreset = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error validating custom ringtone URI", e)
            }
        }

        return presets.firstOrNull { it.id == id } ?: presets.first()
    }

    /**
     * Retrieves the playable Uri for the selected ringtone.
     * Guaranteed never to return a null or crashing Uri.
     */
    fun getSelectedRingtoneUri(context: Context): Uri {
        val selected = getSelectedRingtone(context)
        if (selected.isPreset) {
            val resId = when (selected.id) {
                ID_PRESET_CLASSIC -> R.raw.preset_classic
                ID_PRESET_URGENT -> R.raw.preset_urgent
                else -> R.raw.preset_gentle
            }
            return Uri.parse("android.resource://${context.packageName}/$resId")
        }
        return try {
            val uri = Uri.parse(selected.uriString)
            if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (!f.exists()) {
                    Uri.parse("android.resource://${context.packageName}/${R.raw.preset_gentle}")
                } else {
                    uri
                }
            } else {
                uri
            }
        } catch (e: Exception) {
            Uri.parse("android.resource://${context.packageName}/${R.raw.preset_gentle}")
        }
    }

    /**
     * Persistently saves the user's selected preset ringtone.
     */
    fun setSelectedRingtone(context: Context, ringtone: AlertRingtone) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_RINGTONE_ID, ringtone.id)
            .putString(KEY_RINGTONE_TITLE, ringtone.title)
            .putString(KEY_RINGTONE_URI, ringtone.uriString)
            .putBoolean(KEY_IS_PRESET, ringtone.isPreset)
            .apply()

        Log.d(TAG, "Selected alarm ringtone persistently saved: ${ringtone.title} (${ringtone.uriString})")

        // Update notification channel sound
        NotificationHelper.createNotificationChannel(context, Uri.parse(ringtone.uriString))
    }

    /**
     * Handles local audio file upload from device storage (MP3, WAV, M4A, AAC).
     * Copies the picked stream into durable app-private storage so it is never lost.
     */
    fun saveUploadedRingtone(context: Context, sourceUri: Uri, originalFilename: String?): AlertRingtone? {
        return try {
            val ringtoneDir = File(context.filesDir, "custom_ringtones")
            if (!ringtoneDir.exists()) {
                ringtoneDir.mkdirs()
            }

            val sanitizedTitle = originalFilename?.substringBeforeLast(".")?.take(30) ?: "Uploaded Ringtone"
            val extension = originalFilename?.substringAfterLast(".", "mp3") ?: "mp3"
            val destFile = File(ringtoneDir, "ringtone_${System.currentTimeMillis()}.$extension")

            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                inputStream = context.contentResolver.openInputStream(sourceUri)
                if (inputStream == null) {
                    Log.e(TAG, "Cannot open input stream for picked audio URI: $sourceUri")
                    return null
                }
                outputStream = FileOutputStream(destFile)
                inputStream.copyTo(outputStream)
            } finally {
                inputStream?.close()
                outputStream?.close()
            }

            val durableUri = Uri.fromFile(destFile).toString()
            val customRingtone = AlertRingtone(
                id = ID_CUSTOM_UPLOADED,
                title = sanitizedTitle,
                uriString = durableUri,
                description = "Uploaded audio file (${destFile.name})",
                isPreset = false
            )

            setSelectedRingtone(context, customRingtone)
            Log.d(TAG, "Successfully copied and set custom ringtone: $sanitizedTitle to ${destFile.absolutePath}")
            customRingtone
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import custom ringtone from $sourceUri", e)
            null
        }
    }

    /**
     * Plays a preview of the given ringtone URI.
     * Enforces:
     * 1. Stops any currently playing preview immediately.
     * 2. Automatically stops after exactly 15 seconds (15,000 ms).
     * 3. Allows manual stopping via stopPreview().
     */
    fun playPreview(context: Context, uriString: String, onStopped: (() -> Unit)? = null): Boolean {
        stopPreview()
        onPreviewStoppedListener = onStopped

        return try {
            val uri = Uri.parse(uriString)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context.applicationContext, uri)
                isLooping = true
                prepare()
                start()
            }

            previewMediaPlayer = mp
            currentlyPlayingUri = uriString

            // Automatically stop after exactly 15 seconds
            val runnable = Runnable {
                Log.d(TAG, "15-second preview timer elapsed for $uriString - auto-stopping preview")
                stopPreview()
            }
            previewTimeoutRunnable = runnable
            handler.postDelayed(runnable, 15000L)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ringtone preview for $uriString", e)
            stopPreview()
            false
        }
    }

    /**
     * Stops the active preview player and cancels the 15-second timer.
     */
    fun stopPreview() {
        previewTimeoutRunnable?.let { handler.removeCallbacks(it) }
        previewTimeoutRunnable = null

        try {
            previewMediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping preview MediaPlayer", e)
        } finally {
            previewMediaPlayer = null
            currentlyPlayingUri = null
            onPreviewStoppedListener?.invoke()
            onPreviewStoppedListener = null
        }
    }

    fun isPlayingUri(uriString: String): Boolean {
        return previewMediaPlayer?.isPlaying == true && currentlyPlayingUri == uriString
    }

    fun getCurrentlyPlayingUri(): String? = currentlyPlayingUri
}
