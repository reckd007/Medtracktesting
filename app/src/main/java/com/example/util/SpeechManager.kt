package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Helper class for Text-To-Speech (TTS) answer playback in English, Hindi, and Urdu.
 */
class SpeechManager(context: Context) : TextToSpeech.OnInitListener {
    companion object {
        private const val TAG = "SpeechManager"
    }

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            Log.d(TAG, "TextToSpeech initialized successfully")
        } else {
            isInitialized = false
            Log.w(TAG, "TextToSpeech initialization failed with status $status")
        }
    }

    fun speak(text: String, languageCode: String = "en") {
        if (!isInitialized || tts == null) return

        val locale = when (languageCode) {
            "hi" -> Locale("hi", "IN")
            "ur" -> Locale("ur", "PK")
            else -> Locale.ENGLISH
        }

        try {
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default English if language data is missing
                tts?.language = Locale.ENGLISH
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "medico_speech_id")
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text: ${e.message}")
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS: ${e.message}")
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS: ${e.message}")
        }
    }

    fun isTtsAvailable(): Boolean = isInitialized
}
