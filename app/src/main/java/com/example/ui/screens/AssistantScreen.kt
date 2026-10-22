package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.ChatMessage
import com.example.data.api.ChatRole
import com.example.data.api.EmergencyDetector
import com.example.data.api.PatientHealthContext
import com.example.ui.theme.*
import com.example.util.SpeechManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    includePatientContext: Boolean,
    patientContext: PatientHealthContext?,
    selectedLanguage: String, // "en", "hi", "ur"
    onLanguageSelected: (String) -> Unit,
    onTogglePatientContext: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onClearChat: () -> Unit,
    speechManager: SpeechManager?,
    hasSeenMedPrompt: Boolean = false,
    hideDisclaimer: Boolean = false,
    onDismissMedPrompt: () -> Unit = {},
    onHideDisclaimer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }
    var emergencyTriggeredText by remember { mutableStateOf<String?>(null) }
    var localHideDisclaimer by rememberSaveable(hideDisclaimer) { mutableStateOf(hideDisclaimer) }
    var localSeenMedPrompt by rememberSaveable(hasSeenMedPrompt) { mutableStateOf(hasSeenMedPrompt) }

    // Scroll to bottom when messages change or while generating
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice recognition speech-to-text launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
            }
        }
    }

    fun launchVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                val langLocale = when (selectedLanguage) {
                    "hi" -> "hi-IN"
                    "ur" -> "ur-PK"
                    else -> "en-US"
                }
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langLocale)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your health question...")
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice recognition not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    // Determine layout direction for RTL support in Urdu
    val layoutDirection = if (selectedLanguage == "ur") LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = modifier
                .fillMaxSize()
                .testTag("assistant_screen_root"),
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MedTealSoft)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MedTealPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "MedTrack AI",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = MedTextPrimary
                                    )
                                    Text(
                                        text = if (isGenerating) "Typing answer..." else "AI Health & Medicine Guide",
                                        fontSize = 11.sp,
                                        color = if (isGenerating) MedTealPrimary else MedTextSecondary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Language Selector Dropdown
                                var langMenuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    Surface(
                                        onClick = { langMenuExpanded = true },
                                        shape = RoundedCornerShape(20.dp),
                                        color = MedBgGray,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MedBorderTeal)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Translate,
                                                contentDescription = "Language",
                                                tint = MedTealPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = when (selectedLanguage) {
                                                    "hi" -> "हिंदी"
                                                    "ur" -> "اردو"
                                                    else -> "EN"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MedTealDark
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = MedTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = langMenuExpanded,
                                        onDismissRequest = { langMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("English (EN)", fontWeight = if (selectedLanguage == "en") FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                onLanguageSelected("en")
                                                langMenuExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("हिंदी (Hindi)", fontWeight = if (selectedLanguage == "hi") FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                onLanguageSelected("hi")
                                                langMenuExpanded = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("اردو (Urdu - RTL)", fontWeight = if (selectedLanguage == "ur") FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                onLanguageSelected("ur")
                                                langMenuExpanded = false
                                            }
                                        )
                                    }
                                }

                                IconButton(onClick = { showInfoDialog = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Help,
                                        contentDescription = "Info",
                                        tint = MedTextSecondary
                                    )
                                }

                                IconButton(onClick = onClearChat) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Clear Chat",
                                        tint = MedTextSecondary
                                    )
                                }
                            }
                        }

                        // Disclaimer Banner with hide/cross button
                        if (!localHideDisclaimer && !hideDisclaimer) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF1D5FD6),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = when (selectedLanguage) {
                                            "hi" -> "केवल एआई जानकारी, डॉक्टर की सलाह नहीं। अपने डॉक्टर या फार्मासिस्ट से जांचें।"
                                            "ur" -> "صرف AI معلومات، ڈاکٹر کی ہدایت نہیں ہے۔ اپنے ڈاکٹر یا فارماسسٹ سے تصدیق کریں۔"
                                            else -> "AI information only, not a doctor. Check with your doctor or pharmacist."
                                        },
                                        fontSize = 11.sp,
                                        color = Color(0xFF1E40AF),
                                        lineHeight = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            localHideDisclaimer = true
                                            onHideDisclaimer()
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hide disclaimer",
                                            tint = Color(0xFF1D5FD6),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MedBgGray)
            ) {
                // One-time prompt for "Let assistant see my medicines"
                if (!localSeenMedPrompt && !hasSeenMedPrompt) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Visibility,
                                        contentDescription = null,
                                        tint = if (includePatientContext) MedTealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = when (selectedLanguage) {
                                                "hi" -> "MedTrack AI को मेरी दवाएं देखने दें"
                                                "ur" -> "MedTrack AI کو میری ادویات دیکھنے دیں"
                                                else -> "Let AI assistant see my medicines"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = when (selectedLanguage) {
                                                "hi" -> "सक्रिय प्रोफ़ाइल की दवाएं शामिल करता है (सेटिंग्स में बदलें)"
                                                "ur" -> "ایکٹو پروفائل کی ادویات شامل کی جاتی ہیں (سیٹنگز میں تبدیل کریں)"
                                                else -> "Includes active profile's meds & allergies (manage in Settings)"
                                            },
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Switch(
                                        checked = includePatientContext,
                                        onCheckedChange = onTogglePatientContext,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = MedTealPrimary
                                        ),
                                        modifier = Modifier.testTag("consent_toggle_switch")
                                    )
                                    IconButton(
                                        onClick = {
                                            localSeenMedPrompt = true
                                            onDismissMedPrompt()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (includePatientContext && patientContext != null && patientContext.medications.isNotEmpty()) {
                    // Subtle non-intrusive status pill when enabled
                    Surface(
                        color = MedTealSoft,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MedTealDark, modifier = Modifier.size(12.dp))
                            Text(
                                text = "MedTrack AI Context (${patientContext.profileName}): ${patientContext.medications.size} medicines linked • Change in Settings",
                                fontSize = 10.sp,
                                color = MedTealDark,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Divider(color = MedBorderTeal, thickness = 1.dp)

                // Emergency Warning Banner if detected
                AnimatedVisibility(visible = emergencyTriggeredText != null) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = when (selectedLanguage) {
                                        "hi" -> "आपातकालीन अलर्ट - तुरंत मदद लें!"
                                        "ur" -> "ہنگامی الارم - فوری مدد حاصل کریں!"
                                        else -> "MEDICAL EMERGENCY DETECTED"
                                    },
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF991B1B),
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when (selectedLanguage) {
                                    "hi" -> "यदि आप सीने में दर्द, सांस की तकलीफ, स्ट्रोक या गंभीर स्थिति महसूस कर रहे हैं, तो तुरंत आपातकालीन सेवाओं (भारत में 112) पर कॉल करें या निकटतम अस्पताल जाएँ!"
                                    "ur" -> "اگر آپ سینے میں درد، سانس لینے میں دشواری، یا شدید حالت محسوس کر رہے ہیں، تو فوری طور پر ہنگامی خدمات (112) کو کال کریں یا قریب ترین ہسپتال جائیں!"
                                    else -> "If you are experiencing chest pain, trouble breathing, severe bleeding, poisoning, or severe symptoms, CALL LOCAL EMERGENCY SERVICES IMMEDIATELY (112 in India) or go to the nearest emergency room!"
                                },
                                fontSize = 12.sp,
                                color = Color(0xFF7F1D1D),
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = { emergencyTriggeredText = null }) {
                                    Text("Dismiss Alert", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Messages List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MedTealSoft)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = MedTealPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when (selectedLanguage) {
                                    "hi" -> "दवा और स्वास्थ्य प्रश्न पूछें"
                                    "ur" -> "ادویات اور صحت کے سوالات پوچھیں"
                                    else -> "Ask MedTrack AI"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MedTextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (selectedLanguage) {
                                    "hi" -> "आप खुराक के समय, साइड इफेक्ट्स, खाद्य प्रतिक्रियाएं और स्वास्थ्य सुझावों के बारे में पूछ सकते हैं।"
                                    "ur" -> "آپ خوراک کے اوقات، سائیڈ ایفیکٹس، خوراک کا تعامل اور صحت کے مشورے کے بارے میں پوچھ سکتے ہیں۔"
                                    else -> "Ask about dosage times, food interactions, side effects, storage, or missed dose guidelines."
                                },
                                fontSize = 13.sp,
                                color = MedTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                ChatBubbleItem(
                                    message = msg,
                                    speechManager = speechManager,
                                    onCopy = { text ->
                                        clipboardManager.setText(AnnotatedString(text))
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            if (isGenerating) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MedTealPrimary
                                        )
                                        Text(
                                            text = when (selectedLanguage) {
                                                "hi" -> "एआई उत्तर तैयार कर रहा है..."
                                                "ur" -> "AI جواب تیار کر رہا ہے..."
                                                else -> "Assistant is thinking..."
                                            },
                                            fontSize = 12.sp,
                                            color = MedTextSecondary
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        OutlinedButton(
                                            onClick = onStopGeneration,
                                            shape = RoundedCornerShape(16.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                        ) {
                                            Text("Stop", fontSize = 11.sp, color = Color(0xFFDC2626))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Suggested Questions Chips (Scrollable row so chips never squash)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = when (selectedLanguage) {
                            "hi" -> "सुझाए गए प्रश्न:"
                            "ur" -> "تجویز کردہ سوالات:"
                            else -> "Suggested Questions:"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )

                    val questions = when (selectedLanguage) {
                        "hi" -> listOf(
                            "यह दवा किस लिए है?",
                            "क्या मैं इसे भोजन के साथ ले सकता हूँ?",
                            "यदि खुराक छूट जाए तो क्या करें?",
                            "सामान्य साइड इफेक्ट्स क्या हैं?"
                        )
                        "ur" -> listOf(
                            "یہ دوا کس مقصد کے لیے ہے؟",
                            "کیا میں اسے کھانے کے ساتھ لے سکتا ہوں؟",
                            "اگر خوراک چھوٹ جائے تو کیا کریں؟",
                            "عام سائیڈ ایفیکٹس کیا ہیں؟"
                        )
                        else -> listOf(
                            "What is this medicine for?",
                            "Can I take this with food?",
                            "What if I miss a dose?",
                            "Common side effects?"
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        questions.forEach { q ->
                            Surface(
                                onClick = {
                                    if (EmergencyDetector.checkEmergency(q)) {
                                        emergencyTriggeredText = q
                                    }
                                    onSendMessage(q)
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.widthIn(min = 110.dp)
                            ) {
                                Text(
                                    text = q,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 1.dp)

                // Input Bar with Textfield, Mic, and Send (imePadding, no bottom gap)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = when (selectedLanguage) {
                                        "hi" -> "यहाँ प्रश्न लिखें..."
                                        "ur" -> "یہاں سوال لکھیں..."
                                        else -> "Ask a health question..."
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            ),
                            maxLines = 3,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("assistant_input_textfield")
                        )

                        // Speech-To-Text Mic Button
                        IconButton(
                            onClick = { launchVoiceInput() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Send Button
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val query = inputText.trim()
                                    inputText = ""
                                    if (EmergencyDetector.checkEmergency(query)) {
                                        emergencyTriggeredText = query
                                    }
                                    onSendMessage(query)
                                }
                            },
                            enabled = inputText.isNotBlank() && !isGenerating,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                .testTag("assistant_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // About this Assistant Modal (User-facing explanation)
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "About MedTrack AI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "• What it can do: Answer questions on medicine schedules, dosage timing, food interactions, and storage guidelines.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "• Not a doctor: MedTrack AI provides general information only. Always consult your doctor or pharmacist for medical decisions.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "• What is shared: When 'Let AI assistant see my medicines' is on, only your active profile's medicine names, dosages, and recorded allergies are shared to give tailored guidance.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "• How to turn off: You can toggle 'Let MedTrack AI see my medicines' at any time in Settings > MedTrack AI Settings.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    speechManager: SpeechManager?,
    onCopy: (String) -> Unit
) {
    val isUser = message.role == ChatRole.USER
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Row(
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isUser) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MedTealSoft)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MedTealPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Surface(
                color = if (isUser) MedTealPrimary else Color.White,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                shadowElevation = if (isUser) 0.dp else 1.dp,
                border = if (isUser) null else androidx.compose.foundation.BorderStroke(1.dp, MedBorderTeal)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SelectionContainer {
                        Text(
                            text = message.text,
                            color = if (isUser) Color.White else MedTextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        )
                    }

                    if (!isUser) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Speaker read-aloud button
                            IconButton(
                                onClick = {
                                    speechManager?.speak(message.text, message.languageCode)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VolumeUp,
                                    contentDescription = "Read aloud",
                                    tint = MedTealPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Copy text button
                            IconButton(
                                onClick = { onCopy(message.text) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy text",
                                    tint = MedTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formattedTime,
                fontSize = 10.sp,
                color = MedTextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
