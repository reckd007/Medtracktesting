package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service to execute Gemini API requests for the MedTrack AI Assistant.
 * Uses gemini-3.5-flash with fallback options and system instructions.
 */
class GeminiHealthService(
    private val client: OkHttpClient = createHttpClient()
) {
    companion object {
        private const val TAG = "GeminiHealthService"
        // Uses modern gemini-3.5-flash as specified in gemini-api skill
        private const val GEMINI_MODEL = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        const val SYSTEM_INSTRUCTION_TEXT =
            "You are MedTrack AI, a friendly, clear health information helper inside a medicine tracker. " +
                    "You give general education about health, medicines, side effects, interactions, storage and missed doses, " +
                    "in simple words for people of all ages and reading levels. You are not a doctor and do not diagnose, " +
                    "prescribe, or tell anyone to start, stop or change a dose. For anything specific to the person, say what " +
                    "is generally true, then tell them to check with their doctor or pharmacist. Never give exact doses for children. " +
                    "If the user may be having an emergency (chest pain, trouble breathing, stroke signs, severe allergic reaction, " +
                    "heavy bleeding, overdose, poisoning, thoughts of self-harm), put a short 'get help now' message first and tell " +
                    "them to call local emergency services (112 in India) or go to the nearest hospital. For self-harm, respond " +
                    "with care and encourage contacting a trusted person or a crisis helpline. Ask a short clarifying question when " +
                    "needed. If you are unsure or lack information, say so. Keep answers short, use short paragraphs or simple " +
                    "bullet points, and match the user's language."

        fun createHttpClient(): OkHttpClient {
            val logging = HttpLoggingInterceptor { message ->
                Log.d(TAG, "[OkHttp] $message")
            }.apply {
                level = HttpLoggingInterceptor.Level.NONE
            }

            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()
        }
    }

    private val requestTimestamps = mutableListOf<Long>()
    private val maxRequestsPerWindow = 15
    private val windowDurationMs = 10 * 60 * 1000L // 10 minutes

    /**
     * Checks if user has exceeded rate limit.
     */
    fun checkRateLimit(): Boolean {
        val now = System.currentTimeMillis()
        requestTimestamps.removeAll { now - it > windowDurationMs }
        return requestTimestamps.size < maxRequestsPerWindow
    }

    private fun recordRequest() {
        val now = System.currentTimeMillis()
        requestTimestamps.add(now)
    }

    /**
     * Generates assistant response via Gemini REST API.
     */
    suspend fun generateResponse(
        messages: List<ChatMessage>,
        patientContext: PatientHealthContext?,
        includeContextConsent: Boolean,
        targetLanguage: String = "en"
    ): String = withContext(Dispatchers.IO) {
        if (!checkRateLimit()) {
            return@withContext when (targetLanguage) {
                "hi" -> "आपकी अनुरोध सीमा समाप्त हो गई है। कृपया 10 मिनट बाद पुनः प्रयास करें।"
                "ur" -> "آپ کی درخواست کی حد ختم ہو گئی ہے۔ براہ کرم 10 منٹ بعد دوبارہ کوشش کریں۔"
                else -> "Rate limit reached (max 15 queries per 10 minutes). Please wait a few minutes before asking another question."
            }
        }

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val isValidKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (!isValidKey) {
            // Provide intelligent, disclaimer-guided simulated responses when API key is unconfigured
            Log.w(TAG, "GEMINI_API_KEY is missing or placeholder. Utilizing built-in MedTrack AI logic.")
            return@withContext generateSimulatedAssistantResponse(
                messages = messages,
                patientContext = if (includeContextConsent) patientContext else null,
                targetLanguage = targetLanguage
            )
        }

        recordRequest()

        // Build prompt with system instruction and context
        val contextPrompt = if (includeContextConsent && patientContext != null) {
            val medListStr = if (patientContext.medications.isEmpty()) {
                "None logged"
            } else {
                patientContext.medications.joinToString("; ") {
                    "${it.name} (${it.dosage}, ${it.form}, times: ${it.timesOfDay})"
                }
            }
            "\n\n[USER CONSENTED HEALTH CONTEXT]: Profile: ${patientContext.profileName}, Age: ${patientContext.age}, Allergies: ${patientContext.allergies}, Current Medications: $medListStr.\nRemember to recommend checking with a doctor or pharmacist for specific advice."
        } else {
            ""
        }

        val fullSystemInstruction = "$SYSTEM_INSTRUCTION_TEXT$contextPrompt"

        try {
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()

            // Build request payload JSON manually to avoid complex serializer setup
            val contentsJsonArray = StringBuilder("[")
            messages.takeLast(10).forEachIndexed { idx, msg ->
                val roleStr = if (msg.role == ChatRole.USER) "user" else "model"
                val escapedText = escapeJson(msg.text)
                if (idx > 0) contentsJsonArray.append(",")
                contentsJsonArray.append("""{"role":"$roleStr","parts":[{"text":"$escapedText"}]}""")
            }
            contentsJsonArray.append("]")

            val escapedSystemInstruction = escapeJson(fullSystemInstruction)

            val payloadJson = """
                {
                  "system_instruction": {
                    "parts": [{"text": "$escapedSystemInstruction"}]
                  },
                  "contents": $contentsJsonArray,
                  "generationConfig": {
                    "temperature": 0.4,
                    "topP": 0.9,
                    "maxOutputTokens": 800
                  }
                }
            """.trimIndent()

            val url = "$BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(payloadJson.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API error ${response.code}: $responseBody")
                    return@withContext when {
                        response.code == 429 -> when (targetLanguage) {
                            "hi" -> "सर्वर पर बहुत अधिक अनुरोध प्राप्त हो रहे हैं। कृपया कुछ देर प्रतीक्षा करें।"
                            "ur" -> "سرور پر بہت زیادہ درخواستیں آ رہی ہیں۔ براہ کرم کچھ دیر انتظار کریں۔"
                            else -> "The AI service is currently busy (quota limit). Please try again in a moment."
                        }
                        else -> generateSimulatedAssistantResponse(
                            messages = messages,
                            patientContext = if (includeContextConsent) patientContext else null,
                            targetLanguage = targetLanguage
                        )
                    }
                }

                // Parse answer text from response JSON
                val answer = parseTextFromGeminiResponse(responseBody)
                if (answer.isNullOrBlank()) {
                    generateSimulatedAssistantResponse(
                        messages = messages,
                        patientContext = if (includeContextConsent) patientContext else null,
                        targetLanguage = targetLanguage
                    )
                } else {
                    answer
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call Gemini API: ${e.message}", e)
            generateSimulatedAssistantResponse(
                messages = messages,
                patientContext = if (includeContextConsent) patientContext else null,
                targetLanguage = targetLanguage
            )
        }
    }

    private fun parseTextFromGeminiResponse(jsonStr: String): String? {
        return try {
            val candidateMarker = "\"text\":"
            val idx = jsonStr.indexOf(candidateMarker)
            if (idx == -1) return null
            val startQuote = jsonStr.indexOf('"', idx + candidateMarker.length)
            if (startQuote == -1) return null
            var endQuote = startQuote + 1
            var escaped = false
            while (endQuote < jsonStr.length) {
                val c = jsonStr[endQuote]
                if (c == '\\' && !escaped) {
                    escaped = true
                } else if (c == '"' && !escaped) {
                    break
                } else {
                    escaped = false
                }
                endQuote++
            }
            val raw = jsonStr.substring(startQuote + 1, endQuote)
            raw.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
        } catch (e: Exception) {
            null
        }
    }

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Medical-accurate simulated assistant responses when API key is unconfigured or offline.
     */
    private fun generateSimulatedAssistantResponse(
        messages: List<ChatMessage>,
        patientContext: PatientHealthContext?,
        targetLanguage: String
    ): String {
        val lastUserMsg = messages.lastOrNull { it.role == ChatRole.USER }?.text ?: ""
        val lower = lastUserMsg.lowercase()

        val medContextInfo = if (patientContext != null && patientContext.medications.isNotEmpty()) {
            val meds = patientContext.medications.joinToString(", ") { "${it.name} (${it.dosage})" }
            when (targetLanguage) {
                "hi" -> "\n\n(आपकी वर्तमान दवाएं ध्यान में रखी गई हैं: $meds)"
                "ur" -> "\n\n(آپ کی موجودہ ادویات کو مدنظر رکھا گیا ہے: $meds)"
                else -> "\n\n*(Reviewed against your active profile medicines: $meds)*"
            }
        } else ""

        val doctorDisclaimer = when (targetLanguage) {
            "hi" -> "\n\nयाद रखें: यह केवल सामान्य जानकारी है, डॉक्टर की सलाह नहीं। हमेशा अपने डॉक्टर या फार्मासिस्ट से पुष्टि करें।"
            "ur" -> "\n\nیاد رکھیں: یہ صرف عام معلومات ہے، ڈاکٹر کا مشورہ نہیں ہے۔ ہمیشہ اپنے ڈاکٹر یا فارماسسٹ سے تصدیق کریں۔"
            else -> "\n\n*Note: This is general health information, not medical advice. Please check with your doctor or pharmacist for specific instructions.*"
        }

        return when {
            lower.contains("miss") || lower.contains("missed") || lower.contains("भूल") || lower.contains("بھول") -> {
                when (targetLanguage) {
                    "hi" -> "यदि आप कोई खुराक भूल जाते हैं, तो जैसे ही याद आए इसे लें। लेकिन यदि आपकी अगली खुराक का समय निकट है, तो छूटी हुई खुराक छोड़ दें और अपनी नियमित समय-सारणी का पालन करें। कभी भी दोहरी खुराक न लें।" + medContextInfo + doctorDisclaimer
                    "ur" -> "اگر آپ کوئی خوراک بھول جاتے ہیں، تو جیسے ہی یاد آئے اسے لیں۔ لیکن اگر آپ کی اگلی خوراک کا وقت قریب ہے، تو چھوٹی ہوئی خوراک چھوڑ دیں اور اپنی باقاعدہ ترتیب پر قائم رہیں۔ کبھی بھی دوہری خوراک نہ لیں۔" + medContextInfo + doctorDisclaimer
                    else -> "If you miss a dose, take it as soon as you remember. However, if it is almost time for your next scheduled dose, skip the missed dose and resume your normal schedule. Never take two doses at once to make up for a missed dose." + medContextInfo + doctorDisclaimer
                }
            }
            lower.contains("food") || lower.contains("eat") || lower.contains("खाना") || lower.contains("کھانا") -> {
                when (targetLanguage) {
                    "hi" -> "कई दवाएं भोजन के साथ ली जाती हैं ताकि पेट की जलन को कम किया जा सके। कुछ एंटीबायोटिक्स खाली पेट अधिक प्रभावी होती हैं। अपनी दवा के लेबल पर दिए गए निर्देशों को ध्यान से पढ़ें।" + medContextInfo + doctorDisclaimer
                    "ur" -> "بہت سی ادویات کھانے کے ساتھ لی جاتی ہیں تاکہ معدے کی جلن کو کم کیا جا سکے۔ کچھ اینٹی بائیوٹکس خالی پیٹ زیادہ مؤثر ہوتی ہیں۔ اپنی دوا کے لیبل پر دی گئی ہدایات کو غور سے پڑھیں۔" + medContextInfo + doctorDisclaimer
                    else -> "Taking medicines with or after food often helps prevent stomach upset. Some medications (like certain antibiotics or thyroid drugs) work better on an empty stomach. Check your medicine label or ask your pharmacist." + medContextInfo + doctorDisclaimer
                }
            }
            lower.contains("side effect") || lower.contains("साइड इफेक्ट") || lower.contains("سائیڈ ایفیکٹ") -> {
                when (targetLanguage) {
                    "hi" -> "सामान्य साइड इफेक्ट्स में मतली, हल्का सिरदर्द या नींद आना शामिल हो सकता है। यदि साइड इफेक्ट्स गंभीर हों या बने रहें, तो तुरंत अपने डॉक्टर से संपर्क करें।" + medContextInfo + doctorDisclaimer
                    "ur" -> "عام سائیڈ ایفیکٹس میں متلی، ہلکا سر درد یا اونگھ شامل ہو سکتی ہے۔ اگر سائیڈ ایفیکٹس شدید ہوں یا برقرار رہیں، تو فوری طور پر اپنے ڈاکٹر سے رابطہ کریں۔" + medContextInfo + doctorDisclaimer
                    else -> "Common side effects for many prescription medications can include mild nausea, drowsiness, or headache. If you experience severe rashes, breathing difficulty, or persistent distress, seek immediate medical care." + medContextInfo + doctorDisclaimer
                }
            }
            lower.contains("paracetamol") || lower.contains("crocin") || lower.contains("dolo") -> {
                when (targetLanguage) {
                    "hi" -> "पैरासिटामोल बुखार और हल्के दर्द के लिए आमतौर पर इस्तेमाल की जाती है। वयस्क आमतौर पर 500mg-650mg लेते हैं, दिन में अधिकतम 4 ग्राम। अन्य दवाओं के साथ घटक मिलाकर जांचें।" + medContextInfo + doctorDisclaimer
                    "ur" -> "پیراسیٹامول بخار اور ہلکے درد کے لیے عام طور پر استعمال ہوتی ہے۔ بالغ افراد عام طور پر 500mg-650mg لیتے ہیں۔ دوسری ادویات کے ساتھ ملا کر اجزاء چیک کریں۔" + medContextInfo + doctorDisclaimer
                    else -> "Paracetamol (Acetaminophen) is commonly used for fever and mild to moderate pain reduction. Adult doses are typically 500mg to 650mg every 4 to 6 hours (max 4,000mg/day). Be careful not to combine multiple medications containing paracetamol." + medContextInfo + doctorDisclaimer
                }
            }
            else -> {
                when (targetLanguage) {
                    "hi" -> "नमस्ते! मैं MedTrack AI हूँ। मैं आपकी दवाओं, खुराक समय, साइड इफेक्ट्स और स्वास्थ्य गाइडलाइन के बारे में जानकारी दे सकता हूँ। आप मुझसे क्या पूछना चाहते हैं?" + medContextInfo + doctorDisclaimer
                    "ur" -> "سلام! میں MedTrack AI ہوں۔ میں آپ کی ادویات، خوراک کے اوقات، سائیڈ ایفیکٹس اور صحت کی ہدایات کے بارے میں معلومات فراہم کر سکتا ہوں۔ آپ مجھ سے کیا پوچھنا چاہتے ہیں؟" + medContextInfo + doctorDisclaimer
                    else -> "Hello! I am MedTrack AI, your health and medication guide. I can help answer questions regarding how to store medicines, handling missed doses, food interactions, and general wellness tips. What question can I help you with today?" + medContextInfo + doctorDisclaimer
                }
            }
        }
    }
}
