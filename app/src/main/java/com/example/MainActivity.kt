package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notification.NotificationHelper
import com.example.ui.screens.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.AchievementCelebrationOverlay
import com.example.ui.components.FamilyMemberVerificationDialog
import com.example.ui.components.MedTrackSplashScreen
import com.example.data.auth.*
import com.example.ui.auth.*
import com.example.ui.theme.MedBorderTeal
import com.example.ui.theme.MedTealPrimary
import com.example.ui.theme.MedTealSoft
import com.example.ui.theme.MedTextSecondary
import com.example.ui.theme.MedTrackTheme
import com.example.ui.viewmodel.MedTrackViewModel

enum class AppScreen {
    SPLASH,
    WELCOME,
    LOGIN,
    REGISTRATION,
    DASHBOARD
}

enum class MedTrackTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TODAY("Today", Icons.Filled.Today, Icons.Outlined.Today),
    CABINET("Cabinet", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    CARE("Care", Icons.Filled.LocalHospital, Icons.Outlined.LocalHospital),
    ASSISTANT("Ask AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    STREAKS("Streaks", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        NotificationHelper.createNotificationChannel(this)
        com.example.notification.RefillScheduler.schedulePeriodicRefillCheck(this)

        val targetTabExtra = intent?.getStringExtra("target_tab")
        val initialTab = when (targetTabExtra) {
            "SETTINGS", "REMINDERS" -> MedTrackTab.SETTINGS
            "CABINET" -> MedTrackTab.CABINET
            "DIARY", "CARE" -> MedTrackTab.CARE
            else -> MedTrackTab.TODAY
        }
        setContent {
            val viewModel: MedTrackViewModel = viewModel()
            val appPreferences by viewModel.appPreferences.collectAsStateWithLifecycle()

            MedTrackTheme(
                paletteName = appPreferences.colorPalette,
                appearance = appPreferences.appearance,
                textSizeScale = appPreferences.textSizeScale
            ) {
                MainNavigationRoot(
                    initialTab = initialTab,
                    initialShowReminders = (targetTabExtra == "REMINDERS"),
                    viewModel = viewModel
                )
            }
        }
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                android.view.KeyEvent.KEYCODE_VOLUME_DOWN,
                android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                    if (com.example.notification.AlarmRingtoneService.isRinging) {
                        android.util.Log.d("MainActivity", "Volume button pressed while alarm is ringing: silencing alarm")
                        com.example.notification.AlarmRingtoneService.stopAlarm(this)
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
fun MainNavigationRoot(
    initialTab: MedTrackTab = MedTrackTab.TODAY,
    initialShowReminders: Boolean = false,
    viewModel: MedTrackViewModel
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    var pendingVerificationDraft by remember { mutableStateOf(RegistrationDraft()) }
    var showNotificationRationaleDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    when (currentScreen) {
        AppScreen.SPLASH -> {
            MedTrackSplashScreen(
                onAnimationFinish = {
                    currentScreen = if (viewModel.authRepository.isLoggedIn) {
                        AppScreen.DASHBOARD
                    } else {
                        AppScreen.WELCOME
                    }
                }
            )
        }

        AppScreen.WELCOME -> {
            WelcomeScreen(
                onGetStartedClick = {
                    pendingVerificationDraft = RegistrationDraft()
                    currentScreen = AppScreen.REGISTRATION
                },
                onLoginClick = { currentScreen = AppScreen.LOGIN }
            )
        }

        AppScreen.LOGIN -> {
            LoginScreen(
                onBack = { currentScreen = AppScreen.WELCOME },
                onLoginSuccess = { session ->
                    viewModel.applyOnboardingProfile(session)
                    currentScreen = AppScreen.DASHBOARD
                },
                onNavigateToRegister = {
                    pendingVerificationDraft = RegistrationDraft()
                    currentScreen = AppScreen.REGISTRATION
                },
                onNavigateToVerify = { email, _ ->
                    pendingVerificationDraft = RegistrationDraft(email = email)
                    currentScreen = AppScreen.REGISTRATION
                },
                onPerformLogin = { email, password ->
                    viewModel.authRepository.login(email, password)
                },
                onPerformPasswordRecovery = { email ->
                    viewModel.authRepository.sendPasswordRecovery(email)
                },
                onContinueOffline = { email ->
                    val draft = RegistrationDraft(
                        firstName = "Caregiver",
                        lastName = "User",
                        email = email
                    )
                    val session = viewModel.authRepository.createLocalOfflineSession(draft)
                    viewModel.applyOnboardingProfile(session)
                    currentScreen = AppScreen.DASHBOARD
                }
            )
        }

        AppScreen.REGISTRATION -> {
            RegistrationFlowScreen(
                initialDraft = pendingVerificationDraft,
                onCancelRegistration = { currentScreen = AppScreen.WELCOME },
                onNavigateToLogin = { currentScreen = AppScreen.LOGIN },
                onRegistrationCompleted = { session ->
                    viewModel.applyOnboardingProfile(session)
                    if (session.preferences.getReminders) {
                        showNotificationRationaleDialog = true
                    } else {
                        currentScreen = AppScreen.DASHBOARD
                    }
                },
                onPerformRegistration = { draft ->
                    viewModel.authRepository.registerUser(draft)
                },
                onPerformVerifyCode = { email, code, draft ->
                    viewModel.authRepository.verifyEmailCode(email, code, draft)
                },
                onPerformResendCode = { email ->
                    viewModel.authRepository.resendVerificationCode(email)
                },
                onContinueOffline = { draft ->
                    val session = viewModel.authRepository.createLocalOfflineSession(draft)
                    viewModel.applyOnboardingProfile(session)
                    if (draft.selectedPreferences.contains(RegistrationDraft.PREF_GET_REMINDERS)) {
                        showNotificationRationaleDialog = true
                    } else {
                        currentScreen = AppScreen.DASHBOARD
                    }
                }
            )
        }

        AppScreen.DASHBOARD -> {
            MedTrackApp(
                initialTab = initialTab,
                initialShowReminders = initialShowReminders,
                viewModel = viewModel,
                onLogout = { currentScreen = AppScreen.WELCOME }
            )
        }
    }

    if (showNotificationRationaleDialog) {
        NotificationPermissionPromptDialog(
            onEnableClick = {
                showNotificationRationaleDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                currentScreen = AppScreen.DASHBOARD
            },
            onSkipClick = {
                showNotificationRationaleDialog = false
                currentScreen = AppScreen.DASHBOARD
            }
        )
    }
}

@Composable
fun MedTrackApp(
    initialTab: MedTrackTab = MedTrackTab.TODAY,
    initialShowReminders: Boolean = false,
    viewModel: MedTrackViewModel,
    onLogout: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var showAddMedModal by remember { mutableStateOf(false) }
    var editingMedication by remember { mutableStateOf<com.example.data.model.MedicationEntity?>(null) }
    var showSplashScreen by remember { mutableStateOf(false) }
    var showRemindersInSettings by remember { mutableStateOf(initialShowReminders) }

    val context = LocalContext.current

    // Request notification permission on Android 13+ (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // State from ViewModel
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val todayDoses by viewModel.todayDoses.collectAsStateWithLifecycle()
    val adherenceStats by viewModel.adherenceStats.collectAsStateWithLifecycle()
    val streakInfo by viewModel.streakRewardInfo.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val newlyUnlockedAchievement by viewModel.newlyUnlockedAchievement.collectAsStateWithLifecycle()
    val allMedications by viewModel.allMedications.collectAsStateWithLifecycle()
    val allDoseLogs by viewModel.allDoseLogs.collectAsStateWithLifecycle()
    val timeFilter by viewModel.timeFilter.collectAsStateWithLifecycle()
    val recentHealthEntries by viewModel.recentHealthEntries.collectAsStateWithLifecycle()
    val allAppointments by viewModel.allAppointments.collectAsStateWithLifecycle()
    val lowStockMeds by viewModel.lowStockMeds.collectAsStateWithLifecycle()
    val latestInjection by viewModel.latestInjection.collectAsStateWithLifecycle()
    val recommendedSite by viewModel.recommendedInjectionSite.collectAsStateWithLifecycle()
    val rxNormSuggestions by viewModel.rxNormSuggestions.collectAsStateWithLifecycle()
    val rxNormProducts by viewModel.rxNormProducts.collectAsStateWithLifecycle()
    val isLoadingRxNorm by viewModel.isLoadingRxNorm.collectAsStateWithLifecycle()
    val medicineSearchQuery by viewModel.medicineSearchQuery.collectAsStateWithLifecycle()
    val medicineSearchState by viewModel.medicineSearchState.collectAsStateWithLifecycle()
    val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
    val familyMembers by viewModel.familyMembers.collectAsStateWithLifecycle()
    val appPreferences by viewModel.appPreferences.collectAsStateWithLifecycle()
    val pendingVerificationMember by viewModel.pendingVerificationMember.collectAsStateWithLifecycle()
    val medicationLockoutWarning by viewModel.medicationLockoutWarning.collectAsStateWithLifecycle()
    val isCurrentProfilePending = !viewModel.canAddMedicationForProfile(currentProfile)
    val currentMember = familyMembers.find { it.id == currentProfile || it.name == currentProfile }

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isGeneratingAnswer by viewModel.isGeneratingAnswer.collectAsStateWithLifecycle()
    val includePatientContext by viewModel.includePatientContextConsent.collectAsStateWithLifecycle()
    val patientContext by viewModel.activePatientHealthContext.collectAsStateWithLifecycle()
    val selectedAssistantLanguage by viewModel.selectedAssistantLanguage.collectAsStateWithLifecycle()

    // Handle back button: dismiss modal first if open, or return from reminders subscreen, or return to Today screen
    BackHandler(enabled = showAddMedModal || editingMedication != null) {
        viewModel.clearRxNorm()
        viewModel.clearMedicineSearch()
        viewModel.clearOcrExtracted()
        showAddMedModal = false
        editingMedication = null
    }

    BackHandler(enabled = !showAddMedModal && editingMedication == null && showRemindersInSettings) {
        showRemindersInSettings = false
    }

    BackHandler(enabled = !showAddMedModal && editingMedication == null && !showRemindersInSettings && selectedTab != MedTrackTab.TODAY) {
        selectedTab = MedTrackTab.TODAY
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    color = Color(0xFFE2E8F0),
                    thickness = 0.8.dp
                )
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medtrack_bottom_nav_bar")
                ) {
                MedTrackTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (tab != MedTrackTab.SETTINGS) {
                                showRemindersInSettings = false
                            }
                            selectedTab = tab
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MedTealPrimary,
                            selectedTextColor = MedTealPrimary,
                            indicatorColor = MedTealSoft,
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MedTrackTab.TODAY -> {
                    HomeScreen(
                        currentProfile = currentProfile,
                        onProfileSelected = { viewModel.setProfile(it) },
                        todayDoses = todayDoses,
                        adherenceStats = adherenceStats,
                        lowStockMeds = lowStockMeds,
                        selectedTimeFilter = timeFilter,
                        onTimeFilterSelected = { viewModel.setTimeFilter(it) },
                        onTakeDose = { dose, notes -> viewModel.takeDose(dose, notes) },
                        onSnoozeDose = { viewModel.snoozeDose(it) },
                        onSkipDose = { dose, reason -> viewModel.skipDose(dose, reason) },
                        onRefillMedication = { id, amount -> viewModel.refillMedication(id, amount) },
                        onRunRefillService = { viewModel.triggerRefillCheckService() },
                        onAddMedicationClick = { showAddMedModal = true },
                        familyMembers = familyMembers,
                        isProfilePending = isCurrentProfilePending,
                        onVerifyProfile = {
                            currentMember?.let { viewModel.openVerificationDialog(it) }
                        },
                        onAddNewMemberClick = {
                            selectedTab = MedTrackTab.SETTINGS
                        },
                        streakInfo = streakInfo,
                        achievements = achievements,
                        onTriggerCelebration = { viewModel.triggerAchievementCelebration(it) }
                    )
                }

                MedTrackTab.STREAKS -> {
                    AchievementsScreen(
                        streakInfo = streakInfo,
                        achievements = achievements,
                        onTriggerCelebration = { viewModel.triggerAchievementCelebration(it) }
                    )
                }

                MedTrackTab.ASSISTANT -> {
                    AssistantScreen(
                        messages = chatMessages,
                        isGenerating = isGeneratingAnswer,
                        includePatientContext = includePatientContext,
                        patientContext = patientContext,
                        selectedLanguage = selectedAssistantLanguage,
                        onLanguageSelected = { viewModel.setAssistantLanguage(it) },
                        onTogglePatientContext = { viewModel.setIncludePatientContextConsent(it) },
                        onSendMessage = { viewModel.sendChatMessage(it) },
                        onStopGeneration = { viewModel.stopAnswerGeneration() },
                        onClearChat = { viewModel.clearChatHistory() },
                        speechManager = viewModel.speechManager,
                        hasSeenMedPrompt = appPreferences.aiSeenMedPrompt,
                        hideDisclaimer = appPreferences.aiHideDisclaimer,
                        onDismissMedPrompt = { viewModel.dismissAiMedPrompt() },
                        onHideDisclaimer = { viewModel.setAiDisclaimerHidden(true) }
                    )
                }

                MedTrackTab.CABINET -> {
                    CabinetScreen(
                        medications = allMedications,
                        onRefillMedication = { id, amount -> viewModel.refillMedication(id, amount) },
                        onDeleteMedication = { viewModel.deleteMedication(it) },
                        onAddMedicationClick = { showAddMedModal = true },
                        onEditMedication = { medToEdit -> editingMedication = medToEdit },
                        onRunRefillService = { viewModel.triggerRefillCheckService() },
                        currentProfile = currentProfile,
                        isProfilePending = isCurrentProfilePending,
                        onVerifyProfile = {
                            currentMember?.let { viewModel.openVerificationDialog(it) }
                        }
                    )
                }

                MedTrackTab.SETTINGS -> {
                    if (showRemindersInSettings) {
                        RemindersScreen(
                            todayDoses = todayDoses,
                            lowStockMeds = lowStockMeds,
                            latestInjection = latestInjection,
                            recommendedSite = recommendedSite,
                            onTakeDose = { viewModel.takeDose(it) },
                            onSnoozeDose = { viewModel.snoozeDose(it) },
                            onSkipDose = { viewModel.skipDose(it) },
                            onLogInjection = { code, label -> viewModel.logInjection(code, label) },
                            onRefillMed = { viewModel.refillMedication(it) },
                            onTestAlarm = { viewModel.scheduleTestAlarm(3) },
                            onTestImmediateNotification = { viewModel.sendImmediateTestNotification() },
                            onRescheduleAllAlarms = { viewModel.rescheduleAllAlarms() },
                            onRunRefillService = { viewModel.triggerRefillCheckService() },
                            onTestRefillPush = { viewModel.sendTestRefillNotification() },
                            onTestWorkManager = { viewModel.scheduleTestWorkManagerTask(5) },
                            onBackClick = { showRemindersInSettings = false }
                        )
                    } else {
                        SettingsScreen(
                            userAccount = userAccount,
                            familyMembers = familyMembers,
                            appPreferences = appPreferences,
                            onUpdateAccount = { viewModel.updateUserAccount(it) },
                            onAddFamilyMember = { name, rel, age, blood, allergies, phone ->
                                viewModel.addFamilyMember(name, rel, age, blood, allergies, phone)
                            },
                            onRemoveFamilyMember = { viewModel.removeFamilyMember(it) },
                            onUpdatePreferences = { viewModel.updateAppPreferences(it) },
                            onOpenRemindersAndAlarms = { showRemindersInSettings = true },
                            onVerifyFamilyMember = { viewModel.openVerificationDialog(it) },
                            onLogout = {
                                viewModel.logout()
                                onLogout?.invoke()
                            },
                            onDeleteAccount = {
                                viewModel.deleteAccount()
                                onLogout?.invoke()
                            }
                        )
                    }
                }

                MedTrackTab.CARE -> {
                    CareProfileScreen(
                        currentProfile = currentProfile,
                        onProfileSelected = { viewModel.setProfile(it) },
                        appointments = allAppointments,
                        onToggleAppointment = { viewModel.toggleAppointmentCompleted(it) },
                        onAddAppointment = { name, specialty, dt, loc, reason, notes ->
                            viewModel.addDoctorAppointment(name, specialty, dt, loc, reason, notes)
                        },
                        familyMembers = familyMembers,
                        onVerifyFamilyMember = { viewModel.openVerificationDialog(it) },
                        onAddNewFamilyMemberClick = { selectedTab = MedTrackTab.SETTINGS },
                        streakInfo = streakInfo,
                        achievements = achievements,
                        onTriggerCelebration = { viewModel.triggerAchievementCelebration(it) },
                        diaryEntries = recentHealthEntries,
                        todayDoses = todayDoses,
                        medications = allMedications,
                        doseLogs = allDoseLogs,
                        onTakeDose = { viewModel.takeDose(it) },
                        onLogNewEntry = { mood, pain, sys, dia, sugar, weight, symptoms, notes ->
                            viewModel.logHealthEntry(mood, pain, sys, dia, sugar, weight, symptoms, notes)
                        },
                        onDeleteEntry = { viewModel.deleteHealthEntry(it) }
                    )
                }
            }
        }
    }

    // Modal: Add Medication
    if (showAddMedModal) {
        AddMedicationModal(
            currentProfile = currentProfile,
            medicineSearchQuery = medicineSearchQuery,
            medicineSearchState = medicineSearchState,
            onSearchQueryChange = { viewModel.updateMedicineSearchQuery(it) },
            onRetrySearch = { viewModel.retryMedicineSearch() },
            rxNormSuggestions = rxNormSuggestions,
            rxNormProducts = rxNormProducts,
            isLoadingRxNorm = isLoadingRxNorm,
            onQueryRxNorm = { viewModel.updateRxNormQuery(it) },
            onSelectRxNormSuggestion = { viewModel.selectDrugForConcepts(it) },
            onMedicineOcrCaptured = { details ->
                viewModel.onMedicineTextExtracted(details)
            },
            onQuickCommitOcrMedicine = { details ->
                viewModel.commitOcrMedicineDirectly(details) {
                    showAddMedModal = false
                }
            },
            onDismiss = {
                viewModel.clearRxNorm()
                viewModel.clearMedicineSearch()
                viewModel.clearOcrExtracted()
                showAddMedModal = false
            },
            onSaveMedication = { newMed ->
                viewModel.saveMedication(newMed)
            },
            isProfilePending = isCurrentProfilePending,
            onVerifyProfileClick = {
                currentMember?.let { viewModel.openVerificationDialog(it) }
            }
        )
    }

    // Modal: Edit Medication
    val currentEditingMed = editingMedication
    if (currentEditingMed != null) {
        AddMedicationModal(
            currentProfile = currentEditingMed.profileName,
            medicationToEdit = currentEditingMed,
            medicineSearchQuery = medicineSearchQuery,
            medicineSearchState = medicineSearchState,
            onSearchQueryChange = { viewModel.updateMedicineSearchQuery(it) },
            onRetrySearch = { viewModel.retryMedicineSearch() },
            rxNormSuggestions = rxNormSuggestions,
            rxNormProducts = rxNormProducts,
            isLoadingRxNorm = isLoadingRxNorm,
            onQueryRxNorm = { viewModel.updateRxNormQuery(it) },
            onSelectRxNormSuggestion = { viewModel.selectDrugForConcepts(it) },
            onMedicineOcrCaptured = { details ->
                viewModel.onMedicineTextExtracted(details)
            },
            onQuickCommitOcrMedicine = { details ->
                viewModel.commitOcrMedicineDirectly(details) {
                    editingMedication = null
                }
            },
            onDismiss = {
                viewModel.clearRxNorm()
                viewModel.clearMedicineSearch()
                viewModel.clearOcrExtracted()
                editingMedication = null
            },
            onSaveMedication = { updatedMed ->
                viewModel.saveMedication(updatedMed)
                editingMedication = null
            },
            isProfilePending = isCurrentProfilePending,
            onVerifyProfileClick = {
                currentMember?.let { viewModel.openVerificationDialog(it) }
            }
        )
    }

    // Family Member Patient-Side Verification Dialog
    val currentPendingMember = pendingVerificationMember
    if (currentPendingMember != null) {
        FamilyMemberVerificationDialog(
            member = currentPendingMember,
            onVerifyCode = { code ->
                viewModel.verifyFamilyMember(currentPendingMember.id, code)
            },
            onSimulateApproval = {
                viewModel.simulatePatientApproval(currentPendingMember.id)
            },
            onDismiss = {
                viewModel.dismissVerificationDialog()
            }
        )
    }

    // Access Lockout Warning Dialog
    val currentLockoutWarning = medicationLockoutWarning
    if (currentLockoutWarning != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLockoutWarning() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD97706))
                    Text("Access Lockout", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = currentLockoutWarning,
                    fontSize = 13.sp,
                    color = MedTextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = familyMembers.find { it.id == currentProfile || it.name == currentProfile }
                        viewModel.dismissLockoutWarning()
                        if (target != null) {
                            viewModel.openVerificationDialog(target)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Verify Patient Device Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLockoutWarning() }) {
                    Text("Dismiss", color = MedTextSecondary)
                }
            }
        )
    }

    // Short celebration animation overlay when user completes achievements
    if (newlyUnlockedAchievement != null) {
        AchievementCelebrationOverlay(
            achievement = newlyUnlockedAchievement,
            onDismiss = { viewModel.dismissAchievementCelebration() }
        )
    }

    // Logo opening animation: 0.5-0.75 seconds with logo in center on green background and Medtrack written below
    AnimatedVisibility(
        visible = showSplashScreen,
        enter = EnterTransition.None,
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
    ) {
        MedTrackSplashScreen(
            onAnimationFinish = { showSplashScreen = false }
        )
    }
}
}
