package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.RxNormDrugProduct
import com.example.data.api.RxNormService
import com.example.data.db.MedTrackDatabase
import com.example.data.medicine.MedicineSearchResult
import com.example.data.medicine.MedicineSearchState
import com.example.data.medicine.MedicineSearchRepository
import kotlinx.coroutines.delay
import com.example.data.model.*
import com.example.data.repository.MedTrackRepository
import com.example.notification.MedicationAlarmScheduler
import com.example.notification.MedicationWorkScheduler
import com.example.notification.NotificationHelper
import com.example.data.api.ChatMessage
import com.example.data.api.ChatRole
import com.example.data.api.GeminiHealthService
import com.example.data.api.PatientHealthContext
import com.example.data.api.PatientMedicationContext
import com.example.data.auth.*
import com.example.util.SpeechManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class TodayDoseItem(
    val medication: MedicationEntity,
    val scheduledTime: String,
    val timingSlot: String, // "Morning", "Afternoon", "Evening", "Bedtime"
    val status: String, // "TAKEN", "SNOOZED", "SKIPPED", "PENDING"
    val loggedTime: String = "",
    val notes: String = ""
)

data class AdherenceStats(
    val takenCount: Int = 0,
    val totalCount: Int = 0,
    val percentage: Int = 0,
    val streakDays: Int = 7
)

class MedTrackViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MedTrackRepository
    val authRepository = AuthRepository.getInstance(application)
    val authSession: StateFlow<AuthSession?> = authRepository.currentSession

    init {
        val db = MedTrackDatabase.getDatabase(application)
        repository = MedTrackRepository(
            db.medicationDao(),
            db.doseLogDao(),
            db.healthDiaryDao(),
            db.doctorAppointmentDao(),
            db.injectionSiteDao()
        )
        NotificationHelper.createNotificationChannel(application)
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkAndSeedDatabase(db)
            val allMeds = repository.getAllMedicationsSync()
            MedicationAlarmScheduler.rescheduleAllAlarms(application, allMeds)
            MedicationWorkScheduler.rescheduleAllMedicationWork(application, allMeds)
        }

        // Initialize user account from session if active
        val currentSession = authSession.value
        if (currentSession != null && currentSession.isEmailVerified) {
            val user = currentSession.userProfile
            if (user.fullName.isNotBlank()) {
                val currentAcc = loadUserAccount()
                if (currentAcc.fullName.isBlank()) {
                    updateUserAccount(currentAcc.copy(fullName = user.fullName, email = currentSession.email))
                }
            }
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDateString: String = dateFormat.format(Date())

    private val settingsPrefs = application.getSharedPreferences("medtrack_settings_prefs", Context.MODE_PRIVATE)
    val supabaseFamilyRepo = com.example.data.supabase.SupabaseFamilyRepository(application)
    val achievementManager = com.example.data.repository.AchievementManager(application)

    private val _newlyUnlockedAchievement = MutableStateFlow<Achievement?>(null)
    val newlyUnlockedAchievement: StateFlow<Achievement?> = _newlyUnlockedAchievement.asStateFlow()

    // Speech & Gemini Assistant Services
    val speechManager = SpeechManager(application)
    private val geminiService = GeminiHealthService()
    private var generationJob: Job? = null

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isGeneratingAnswer = MutableStateFlow(false)
    val isGeneratingAnswer: StateFlow<Boolean> = _isGeneratingAnswer.asStateFlow()

    private val _includePatientContextConsent = MutableStateFlow(
        settingsPrefs.getBoolean("pref_ai_see_medicines", false)
    )
    val includePatientContextConsent: StateFlow<Boolean> = _includePatientContextConsent.asStateFlow()

    private val _selectedAssistantLanguage = MutableStateFlow("en")
    val selectedAssistantLanguage: StateFlow<String> = _selectedAssistantLanguage.asStateFlow()

    // Profile filter
    private val _currentProfile = MutableStateFlow("Self")
    val currentProfile: StateFlow<String> = _currentProfile.asStateFlow()

    // Family Members
    private val _familyMembers = MutableStateFlow<List<FamilyMember>>(loadFamilyMembers())
    val familyMembers: StateFlow<List<FamilyMember>> = _familyMembers.asStateFlow()

    // Pending verification dialog target
    private val _pendingVerificationMember = MutableStateFlow<FamilyMember?>(null)
    val pendingVerificationMember: StateFlow<FamilyMember?> = _pendingVerificationMember.asStateFlow()

    // Lockout warning message when attempting actions on unverified dependent
    private val _medicationLockoutWarning = MutableStateFlow<String?>(null)
    val medicationLockoutWarning: StateFlow<String?> = _medicationLockoutWarning.asStateFlow()

    // Checks if the active profile is verified (Self is always verified; dependents require patient phone verification)
    val isCurrentProfileVerified: StateFlow<Boolean> = combine(
        _currentProfile,
        _familyMembers
    ) { profile, members ->
        if (profile == "Self") true
        else {
            val member = members.find { it.id == profile || it.name == profile }
            member?.isVerified ?: true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // User Account Info
    private val _userAccount = MutableStateFlow(loadUserAccount())
    val userAccount: StateFlow<UserAccountInfo> = _userAccount.asStateFlow()

    // App Preferences
    private val _appPreferences = MutableStateFlow(loadAppPreferences())
    val appPreferences: StateFlow<AppPreferences> = _appPreferences.asStateFlow()

    // Time of day filter for Today screen
    private val _timeFilter = MutableStateFlow("All")
    val timeFilter: StateFlow<String> = _timeFilter.asStateFlow()

    // Raw streams
    val allMedications: StateFlow<List<MedicationEntity>> = repository.allMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayLogs: StateFlow<List<DoseLogEntity>> = repository.getLogsForDate(todayDateString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDoseLogs: StateFlow<List<DoseLogEntity>> = repository.allDoseLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentHealthEntries: StateFlow<List<HealthDiaryEntity>> = repository.recentHealthEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAppointments: StateFlow<List<DoctorAppointmentEntity>> = repository.allAppointments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestInjection: StateFlow<InjectionSiteLogEntity?> = repository.latestInjectionLog
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- NIH RxNorm Search & Autocomplete (Debounced 300ms) ---
    private val rxNormService = RxNormService()

    private val _rxNormQuery = MutableStateFlow("")
    val rxNormQuery: StateFlow<String> = _rxNormQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val rxNormSuggestions: StateFlow<List<String>> = rxNormService
        .observeSpellingSuggestions(_rxNormQuery)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _rxNormProducts = MutableStateFlow<List<RxNormDrugProduct>>(emptyList())
    val rxNormProducts: StateFlow<List<RxNormDrugProduct>> = _rxNormProducts.asStateFlow()

    private val _isLoadingRxNorm = MutableStateFlow(false)
    val isLoadingRxNorm: StateFlow<Boolean> = _isLoadingRxNorm.asStateFlow()

    fun updateRxNormQuery(query: String) {
        _rxNormQuery.value = query
        if (query.isBlank()) {
            _rxNormProducts.value = emptyList()
        }
    }

    fun selectDrugForConcepts(drugName: String) {
        _rxNormQuery.value = drugName
        viewModelScope.launch {
            _isLoadingRxNorm.value = true
            val products = rxNormService.fetchDrugProducts(drugName)
            _rxNormProducts.value = products
            _isLoadingRxNorm.value = false
        }
    }

    fun clearRxNorm() {
        _rxNormQuery.value = ""
        _rxNormProducts.value = emptyList()
    }

    // --- Searchable Medicine Dropdown (Real Online DB + Indian Directory, Debounced 350ms) ---
    private val medicineSearchRepo = MedicineSearchRepository()
    private val _medicineSearchQuery = MutableStateFlow("")
    val medicineSearchQuery: StateFlow<String> = _medicineSearchQuery.asStateFlow()

    private val _medicineSearchState = MutableStateFlow<MedicineSearchState>(MedicineSearchState.Idle)
    val medicineSearchState: StateFlow<MedicineSearchState> = _medicineSearchState.asStateFlow()

    private var medicineSearchJob: Job? = null

    fun updateMedicineSearchQuery(query: String) {
        _medicineSearchQuery.value = query
        medicineSearchJob?.cancel()
        val clean = query.trim()
        if (clean.length < 2) {
            _medicineSearchState.value = MedicineSearchState.Idle
            return
        }

        medicineSearchJob = viewModelScope.launch {
            delay(350L) // 350ms debounce window (within 300-500ms requirement)
            _medicineSearchState.value = MedicineSearchState.Loading
            val result = medicineSearchRepo.searchMedicines(clean, maxResults = 8)
            result.fold(
                onSuccess = { list ->
                    if (list.isEmpty()) {
                        _medicineSearchState.value = MedicineSearchState.Empty(clean)
                    } else {
                        _medicineSearchState.value = MedicineSearchState.Success(list, clean)
                    }
                },
                onFailure = { error ->
                    _medicineSearchState.value = MedicineSearchState.Error(
                        message = error.localizedMessage ?: "Unable to connect to online medicine database.",
                        query = clean,
                        canRetry = true
                    )
                }
            )
        }
    }

    fun retryMedicineSearch() {
        val q = _medicineSearchQuery.value
        if (q.trim().length >= 2) {
            updateMedicineSearchQuery(q)
        }
    }

    fun clearMedicineSearch() {
        medicineSearchJob?.cancel()
        _medicineSearchQuery.value = ""
        _medicineSearchState.value = MedicineSearchState.Idle
    }


    // --- On-Device ML Kit OCR Text Recognition ---
    private val _extractedOcrDetails = MutableStateFlow<com.example.ui.scanner.ExtractedMedicineDetails?>(null)
    val extractedOcrDetails: StateFlow<com.example.ui.scanner.ExtractedMedicineDetails?> = _extractedOcrDetails.asStateFlow()

    fun onMedicineTextExtracted(details: com.example.ui.scanner.ExtractedMedicineDetails) {
        _extractedOcrDetails.value = details
    }

    fun clearOcrExtracted() {
        _extractedOcrDetails.value = null
    }

    /**
     * Directly commits OCR extracted medication to Room database and schedules daily reminders.
     */
    fun commitOcrMedicineDirectly(
        details: com.example.ui.scanner.ExtractedMedicineDetails,
        profileName: String = _currentProfile.value,
        onSuccess: (Long) -> Unit = {}
    ) {
        if (!canAddMedicationForProfile(profileName)) {
            _medicationLockoutWarning.value = "Waiting for verification from $profileName's device. You cannot schedule medicines until verified."
            return
        }
        viewModelScope.launch {
            val entity = MedicationEntity(
                name = details.name,
                dosage = details.dosage,
                form = details.formFactor,
                instructions = "Take as directed",
                timesOfDay = "08:00,20:00",
                timingSlots = "Morning,Evening",
                frequencyPerDay = 2,
                frequencyDaysPerWeek = 7,
                courseDurationDays = 0,
                stockRemaining = 30,
                refillThreshold = 5,
                profileName = profileName,
                notes = "Auto-extracted via medicine label scanner",
                rxcui = ""
            )
            val newId = repository.insertMedication(entity)
            val insertedMed = repository.getMedicationById(newId)
            if (insertedMed != null) {
                MedicationAlarmScheduler.scheduleMedicationAlarms(getApplication(), insertedMed)
            }
            onSuccess(newId)
        }
    }

    // Today scheduled dose list combined with logs and profile
    val todayDoses: StateFlow<List<TodayDoseItem>> = combine(
        allMedications,
        todayLogs,
        _currentProfile,
        _timeFilter
    ) { meds, logs, profile, filter ->
        val filteredMeds = meds.filter { it.profileName == profile }
        val items = mutableListOf<TodayDoseItem>()

        for (med in filteredMeds) {
            val times = med.timesOfDay.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val slots = med.timingSlots.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            times.forEachIndexed { index, time ->
                val slot = slots.getOrElse(index) {
                    when {
                        time < "12:00" -> "Morning"
                        time < "17:00" -> "Afternoon"
                        time < "21:00" -> "Evening"
                        else -> "Bedtime"
                    }
                }

                val log = logs.firstOrNull { it.medicationId == med.id && it.scheduledTime == time }
                val status = log?.status ?: "PENDING"
                val loggedTimeStr = if (log != null && log.status == "TAKEN") {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(log.loggedTimestamp))
                } else ""

                if (filter == "All" || filter.equals(slot, ignoreCase = true)) {
                    items.add(
                        TodayDoseItem(
                            medication = med,
                            scheduledTime = time,
                            timingSlot = slot,
                            status = status,
                            loggedTime = loggedTimeStr,
                            notes = log?.notes ?: ""
                        )
                    )
                }
            }
        }
        items.sortedBy { it.scheduledTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Consecutive days streak and reward calculation from historical dose logs
    val streakRewardInfo: StateFlow<StreakRewardInfo> = combine(
        allDoseLogs,
        allMedications,
        _currentProfile,
        todayLogs
    ) { logs, meds, profile, todayDoseLogs ->
        val (streakDays, weekStatus) = achievementManager.calculateStreak(logs, meds, profile)
        val totalOnTime = logs.count { it.status == "TAKEN" }
        val profileMeds = meds.filter { it.profileName == profile }
        var totalScheduled = 0
        profileMeds.forEach { med ->
            totalScheduled += med.timesOfDay.split(",").filter { it.trim().isNotEmpty() }.size
        }
        val todayTaken = todayDoseLogs.count { it.status == "TAKEN" }
        val todayCompleted = (totalScheduled > 0 && todayTaken >= totalScheduled)

        achievementManager.buildStreakRewardInfo(
            streakDays = streakDays,
            totalOnTimeDoses = totalOnTime,
            weekStatus = weekStatus,
            todayCompleted = todayCompleted
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        achievementManager.buildStreakRewardInfo(7, 28, emptyList(), false)
    )

    // Dynamic Achievements & Milestones evaluation
    val achievements: StateFlow<List<Achievement>> = combine(
        allDoseLogs,
        allMedications,
        todayLogs,
        recentHealthEntries,
        _currentProfile
    ) { logs, meds, tLogs, diary, profile ->
        val (streakDays, _) = achievementManager.calculateStreak(logs, meds, profile)
        val totalTakenDoses = logs.count { it.status == "TAKEN" }
        val totalRefills = achievementManager.getRefillCount()

        val profileMeds = meds.filter { it.profileName == profile }
        var totalScheduled = 0
        profileMeds.forEach { med ->
            totalScheduled += med.timesOfDay.split(",").filter { it.trim().isNotEmpty() }.size
        }
        val todayTaken = tLogs.count { it.status == "TAKEN" }
        val hasEarly = logs.any { it.status == "TAKEN" && (it.scheduledTime.startsWith("06") || it.scheduledTime.startsWith("07") || it.scheduledTime.startsWith("08")) }
        val hasDiary = diary.isNotEmpty()

        val (achList, newUnlocks) = achievementManager.buildAchievements(
            streakDays = streakDays,
            totalTakenDoses = totalTakenDoses,
            totalRefills = totalRefills,
            todayTakenCount = todayTaken,
            todayTotalCount = totalScheduled,
            hasEarlyMorningDose = hasEarly,
            hasHealthDiaryEntry = hasDiary
        )

        if (newUnlocks.isNotEmpty() && _newlyUnlockedAchievement.value == null) {
            _newlyUnlockedAchievement.value = newUnlocks.first()
        }

        achList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today adherence calculations
    val adherenceStats: StateFlow<AdherenceStats> = combine(
        allMedications,
        todayLogs,
        allDoseLogs,
        _currentProfile
    ) { meds, logs, allLogs, profile ->
        val profileMeds = meds.filter { it.profileName == profile }
        var totalScheduled = 0
        profileMeds.forEach { med ->
            totalScheduled += med.timesOfDay.split(",").filter { it.trim().isNotEmpty() }.size
        }

        val taken = logs.count { it.status == "TAKEN" }
        val pct = if (totalScheduled > 0) ((taken.toFloat() / totalScheduled) * 100).toInt() else 0
        val (streakDays, _) = achievementManager.calculateStreak(allLogs, meds, profile)

        AdherenceStats(
            takenCount = taken,
            totalCount = totalScheduled,
            percentage = pct,
            streakDays = streakDays
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdherenceStats(3, 6, 50, 7))

    // Low stock medications monitored in real-time from Room database
    val lowStockMeds: StateFlow<List<MedicationEntity>> = repository.lowStockMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recommended next injection site
    val recommendedInjectionSite: StateFlow<String> = latestInjection.map { last ->
        when (last?.siteCode) {
            "ABDOMEN_LEFT" -> "Abdomen - Right Lower"
            "ABDOMEN_RIGHT" -> "Thigh - Left Outer"
            "THIGH_LEFT" -> "Thigh - Right Outer"
            "THIGH_RIGHT" -> "Upper Arm - Left"
            "ARM_LEFT" -> "Upper Arm - Right"
            else -> "Abdomen - Left Lower"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Abdomen - Right Lower")

    // --- User Actions ---

    fun setProfile(profile: String) {
        _currentProfile.value = profile
    }

    fun setTimeFilter(filter: String) {
        _timeFilter.value = filter
    }

    fun takeDose(dose: TodayDoseItem, notes: String = "") {
        viewModelScope.launch {
            repository.recordDoseStatus(
                medicationId = dose.medication.id,
                medicationName = dose.medication.name,
                scheduledTime = dose.scheduledTime,
                dateString = todayDateString,
                status = "TAKEN",
                notes = notes
            )
        }
    }

    fun snoozeDose(dose: TodayDoseItem) {
        viewModelScope.launch {
            repository.recordDoseStatus(
                medicationId = dose.medication.id,
                medicationName = dose.medication.name,
                scheduledTime = dose.scheduledTime,
                dateString = todayDateString,
                status = "SNOOZED",
                notes = "Snoozed for 15 minutes"
            )
        }
    }

    fun skipDose(dose: TodayDoseItem, reason: String = "Patient opted to skip") {
        viewModelScope.launch {
            repository.recordDoseStatus(
                medicationId = dose.medication.id,
                medicationName = dose.medication.name,
                scheduledTime = dose.scheduledTime,
                dateString = todayDateString,
                status = "SKIPPED",
                notes = reason
            )
        }
    }

    fun refillMedication(medicationId: Long, amount: Int = 30) {
        viewModelScope.launch {
            repository.refillStock(medicationId, amount)
            val newCount = achievementManager.incrementRefillCount()
            if (newCount == 1) {
                achievementManager.markAchievementUnlocked(com.example.data.repository.AchievementManager.ID_FIRST_REFILL)
                val ach = achievements.value.firstOrNull { it.id == com.example.data.repository.AchievementManager.ID_FIRST_REFILL }
                    ?: Achievement(
                        id = com.example.data.repository.AchievementManager.ID_FIRST_REFILL,
                        title = "First Refill",
                        description = "Refill a medication in your medicine cabinet before running low",
                        iconType = AchievementIconType.FIRST_REFILL,
                        currentProgress = 1,
                        maxProgress = 1,
                        isUnlocked = true,
                        rewardPoints = 100,
                        rewardBadge = "Bronze",
                        category = "Supply"
                    )
                _newlyUnlockedAchievement.value = ach
            }
        }
    }

    fun dismissAchievementCelebration() {
        _newlyUnlockedAchievement.value = null
    }

    fun triggerAchievementCelebration(achievement: Achievement) {
        _newlyUnlockedAchievement.value = achievement
    }

    fun saveMedication(medication: MedicationEntity) {
        if (!canAddMedicationForProfile(medication.profileName)) {
            _medicationLockoutWarning.value = "Access Lockout: Waiting for verification from ${medication.profileName}'s device. Medication scheduling is locked until verified."
            return
        }
        viewModelScope.launch {
            val newId = repository.insertMedication(medication)
            val updated = if (medication.id == 0L) medication.copy(id = newId) else medication
            MedicationAlarmScheduler.scheduleMedicationAlarms(getApplication(), updated)
            val times = updated.timesOfDay.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            times.forEachIndexed { idx, tStr ->
                MedicationWorkScheduler.scheduleDoseWork(getApplication(), updated, idx, tStr)
            }
        }
    }

    fun deleteMedication(medication: MedicationEntity) {
        viewModelScope.launch {
            MedicationAlarmScheduler.cancelMedicationAlarms(getApplication(), medication)
            MedicationWorkScheduler.cancelDoseWork(getApplication(), medication.id)
            repository.deleteMedication(medication)
        }
    }

    /**
     * Schedules a quick test WorkManager task firing in specified delaySeconds (e.g., 5 seconds).
     */
    fun scheduleTestWorkManagerTask(delaySeconds: Int = 5) {
        MedicationWorkScheduler.scheduleTestWorkTask(getApplication(), delaySeconds)
    }

    /**
     * Schedules a quick test reminder using AlarmManager to verify background trigger.
     */
    fun scheduleTestAlarm(delaySeconds: Int = 3) {
        MedicationAlarmScheduler.scheduleQuickTestAlarm(getApplication(), delaySeconds)
    }

    /**
     * Sends an immediate push notification test to verify channel and notification UI.
     */
    fun sendImmediateTestNotification() {
        NotificationHelper.showTestNotification(getApplication())
    }

    /**
     * Reschedules daily alarms for all medications currently stored in the database.
     */
    fun rescheduleAllAlarms() {
        viewModelScope.launch(Dispatchers.IO) {
            val meds = repository.getAllMedicationsSync()
            MedicationAlarmScheduler.rescheduleAllAlarms(getApplication(), meds)
        }
    }

    /**
     * Evaluates medication quantities in Room database against defined threshold
     * and triggers a low-stock alert push notification for each item dropping below threshold.
     */
    fun checkLowStockAndAlert() {
        viewModelScope.launch {
            repository.checkAndTriggerLowStockAlerts(getApplication())
        }
    }

    /**
     * Triggers the RefillCheckBackgroundService to check medication quantities in the Room database
     * against user-defined thresholds and dispatch push notifications for low-stock items.
     */
    fun triggerRefillCheckService() {
        com.example.notification.RefillScheduler.triggerRefillCheckService(getApplication())
    }

    /**
     * Sends an immediate test push notification for a low-stock medication refill alert.
     */
    fun sendTestRefillNotification() {
        NotificationHelper.showTestRefillNotification(getApplication())
    }

    fun logHealthEntry(
        mood: String,
        painLevel: Int,
        systolic: Int,
        diastolic: Int,
        bloodSugar: Int,
        weight: Float,
        symptoms: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.logHealthEntry(
                HealthDiaryEntity(
                    dateString = todayDateString,
                    mood = mood,
                    painLevel = painLevel,
                    systolicBp = systolic,
                    diastolicBp = diastolic,
                    bloodSugar = bloodSugar,
                    weightKg = weight,
                    symptoms = symptoms,
                    notes = notes
                )
            )
        }
    }

    fun deleteHealthEntry(entry: HealthDiaryEntity) {
        viewModelScope.launch {
            repository.deleteHealthEntry(entry)
        }
    }

    fun addDoctorAppointment(
        doctorName: String,
        specialty: String,
        dateTimeString: String,
        clinicLocation: String,
        reason: String,
        notes: String,
        reminderLeadTime: String = "1 day before"
    ) {
        viewModelScope.launch {
            val appt = DoctorAppointmentEntity(
                doctorName = doctorName,
                specialty = specialty,
                dateTimeString = dateTimeString,
                clinicLocation = clinicLocation,
                reason = reason,
                notes = notes,
                reminderLeadTime = reminderLeadTime
            )
            val insertedId = repository.insertAppointment(appt)
            val scheduledAppt = appt.copy(id = insertedId)
            com.example.notification.AppointmentAlarmScheduler.scheduleAppointmentReminder(getApplication(), scheduledAppt)
        }
    }

    fun toggleAppointmentCompleted(appointment: DoctorAppointmentEntity) {
        viewModelScope.launch {
            repository.updateAppointment(appointment.copy(isCompleted = !appointment.isCompleted))
        }
    }

    fun logInjection(siteCode: String, siteLabel: String) {
        viewModelScope.launch {
            repository.logInjection(siteCode, siteLabel)
        }
    }

    fun addFamilyMember(
        name: String,
        relationship: String,
        patientPhone: String = "",
        ageOrBirth: String = "",
        bloodType: String = "",
        allergies: String = "",
        emergencyContact: String = ""
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val initial = cleanName.first().uppercase()
        val profileId = if (relationship.isNotBlank() && relationship != "Self" && relationship != "Other") {
            "$cleanName ($relationship)"
        } else {
            cleanName
        }
        val verificationCode = (100000..999999).random().toString()
        val newMember = FamilyMember(
            id = profileId,
            name = cleanName,
            relationship = relationship.ifBlank { "Dependent" },
            initial = initial,
            ageOrBirth = ageOrBirth.trim(),
            bloodType = bloodType.trim(),
            allergies = allergies.trim(),
            emergencyContact = emergencyContact.trim().ifBlank { patientPhone.trim() },
            isPrimary = false,
            patientPhone = patientPhone.trim(),
            verificationStatus = "pending",
            verificationCode = verificationCode
        )
        val updated = _familyMembers.value.filter { it.id != profileId } + newMember
        _familyMembers.value = updated
        saveFamilyMembers(updated)

        // Trigger Supabase remote sync
        viewModelScope.launch {
            supabaseFamilyRepo.addFamilyMemberWithVerification(
                patientName = cleanName,
                relationship = relationship,
                patientPhone = patientPhone
            )
        }

        // Immediately transition caregiver to "Waiting for Verification" dialog
        _pendingVerificationMember.value = newMember
    }

    fun verifyFamilyMember(
        memberId: String,
        code: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val member = _familyMembers.value.find { it.id == memberId }
            if (member == null) {
                onComplete(false, "Member profile not found.")
                return@launch
            }

            val isLocalMatch = member.verificationCode.trim() == code.trim() || member.verificationCode.isBlank()
            if (isLocalMatch) {
                val updatedList = _familyMembers.value.map {
                    if (it.id == memberId) it.copy(verificationStatus = "verified") else it
                }
                _familyMembers.value = updatedList
                saveFamilyMembers(updatedList)

                // Sync to local Supabase PostgREST
                supabaseFamilyRepo.verifyFamilyMember(memberId, code)

                _pendingVerificationMember.value = null
                _medicationLockoutWarning.value = null
                onComplete(true, "Device verified for ${member.name}! Medication schedule access unlocked.")
            } else {
                val remoteRes = supabaseFamilyRepo.verifyFamilyMember(memberId, code)
                if (remoteRes.isSuccess) {
                    val updatedList = _familyMembers.value.map {
                        if (it.id == memberId) it.copy(verificationStatus = "verified") else it
                    }
                    _familyMembers.value = updatedList
                    saveFamilyMembers(updatedList)
                    _pendingVerificationMember.value = null
                    _medicationLockoutWarning.value = null
                    onComplete(true, "Device verified for ${member.name} via Supabase! Access unlocked.")
                } else {
                    onComplete(false, "Invalid verification code. Please check the code on the patient's device.")
                }
            }
        }
    }

    fun simulatePatientApproval(
        memberId: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val member = _familyMembers.value.find { it.id == memberId }
        val code = member?.verificationCode?.takeIf { it.isNotBlank() } ?: "739104"
        verifyFamilyMember(memberId, code, onComplete)
    }

    fun openVerificationDialog(member: FamilyMember) {
        _pendingVerificationMember.value = member
    }

    fun dismissVerificationDialog() {
        _pendingVerificationMember.value = null
    }

    fun dismissLockoutWarning() {
        _medicationLockoutWarning.value = null
    }

    fun canAddMedicationForProfile(profileName: String = _currentProfile.value): Boolean {
        if (profileName == "Self") return true
        val member = _familyMembers.value.find { it.id == profileName || it.name == profileName }
        return member?.isVerified ?: true
    }

    fun removeFamilyMember(memberId: String) {
        if (memberId == "Self") return
        val updated = _familyMembers.value.filter { it.id != memberId }
        _familyMembers.value = updated
        saveFamilyMembers(updated)
        viewModelScope.launch {
            supabaseFamilyRepo.removeProfile(memberId)
        }
        if (_currentProfile.value == memberId) {
            _currentProfile.value = "Self"
        }
    }

    fun updateUserAccount(info: UserAccountInfo) {
        _userAccount.value = info
        saveUserAccount(info)
        val currentList = _familyMembers.value
        val updated = currentList.map {
            if (it.isPrimary) it.copy(
                name = info.fullName,
                initial = info.fullName.firstOrNull()?.uppercase() ?: "A",
                bloodType = info.bloodType,
                emergencyContact = info.phone
            ) else it
        }
        _familyMembers.value = updated
        saveFamilyMembers(updated)
    }

    fun updateAppPreferences(prefs: AppPreferences) {
        _appPreferences.value = prefs
        saveAppPreferences(prefs)
    }

    private fun loadFamilyMembers(): List<FamilyMember> {
        val jsonStr = settingsPrefs.getString("family_members_json", null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<FamilyMember>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        FamilyMember(
                            id = obj.optString("id", "Self"),
                            name = obj.optString("name", "Alex Henderson"),
                            relationship = obj.optString("relationship", "Self"),
                            initial = obj.optString("initial", "A"),
                            ageOrBirth = obj.optString("ageOrBirth", ""),
                            bloodType = obj.optString("bloodType", ""),
                            allergies = obj.optString("allergies", ""),
                            emergencyContact = obj.optString("emergencyContact", ""),
                            isPrimary = obj.optBoolean("isPrimary", false),
                            patientPhone = obj.optString("patientPhone", ""),
                            verificationStatus = obj.optString("verificationStatus", "verified"),
                            verificationCode = obj.optString("verificationCode", "")
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                android.util.Log.e("MedTrackViewModel", "Error loading family members json", e)
            }
        }
        return listOf(
            FamilyMember(
                id = "Self",
                name = "Alex Henderson",
                relationship = "Self",
                initial = "A",
                ageOrBirth = "34",
                bloodType = "O+",
                allergies = "Penicillin",
                emergencyContact = "+1 (555) 234-5678",
                isPrimary = true,
                patientPhone = "+1 (555) 234-5678",
                verificationStatus = "verified",
                verificationCode = ""
            ),
            FamilyMember(
                id = "Dad",
                name = "Robert Henderson",
                relationship = "Parent",
                initial = "R",
                ageOrBirth = "68",
                bloodType = "A+",
                allergies = "None",
                emergencyContact = "+1 (555) 876-5432",
                isPrimary = false,
                patientPhone = "+1 (555) 876-5432",
                verificationStatus = "verified",
                verificationCode = "849201"
            ),
            FamilyMember(
                id = "Uncle Arthur (Uncle)",
                name = "Arthur Miller",
                relationship = "Uncle",
                initial = "A",
                ageOrBirth = "62",
                bloodType = "B+",
                allergies = "Sulfa",
                emergencyContact = "+1 (555) 432-9876",
                isPrimary = false,
                patientPhone = "+1 (555) 432-9876",
                verificationStatus = "pending",
                verificationCode = "739104"
            )
        )
    }

    private fun saveFamilyMembers(members: List<FamilyMember>) {
        try {
            val array = JSONArray()
            for (m in members) {
                val obj = JSONObject().apply {
                    put("id", m.id)
                    put("name", m.name)
                    put("relationship", m.relationship)
                    put("initial", m.initial)
                    put("ageOrBirth", m.ageOrBirth)
                    put("bloodType", m.bloodType)
                    put("allergies", m.allergies)
                    put("emergencyContact", m.emergencyContact)
                    put("isPrimary", m.isPrimary)
                    put("patientPhone", m.patientPhone)
                    put("verificationStatus", m.verificationStatus)
                    put("verificationCode", m.verificationCode)
                }
                array.put(obj)
            }
            settingsPrefs.edit().putString("family_members_json", array.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("MedTrackViewModel", "Error saving family members json", e)
        }
    }

    private fun loadUserAccount(): UserAccountInfo {
        return UserAccountInfo(
            fullName = settingsPrefs.getString("user_fullname", "") ?: "",
            email = settingsPrefs.getString("user_email", "") ?: "",
            phone = settingsPrefs.getString("user_phone", "") ?: "",
            bloodType = settingsPrefs.getString("user_blood_type", "") ?: "",
            primaryDoctor = settingsPrefs.getString("user_primary_doc", "") ?: "",
            insuranceProvider = settingsPrefs.getString("user_insurance", "") ?: "",
            preferredPharmacy = settingsPrefs.getString("user_pharmacy", "") ?: "",
            isGuest = settingsPrefs.getBoolean("user_is_guest", false)
        )
    }

    private fun saveUserAccount(info: UserAccountInfo) {
        settingsPrefs.edit()
            .putString("user_fullname", info.fullName)
            .putString("user_email", info.email)
            .putString("user_phone", info.phone)
            .putString("user_blood_type", info.bloodType)
            .putString("user_primary_doc", info.primaryDoctor)
            .putString("user_insurance", info.insuranceProvider)
            .putString("user_pharmacy", info.preferredPharmacy)
            .putBoolean("user_is_guest", info.isGuest)
            .apply()
    }

    private fun loadAppPreferences(): AppPreferences {
        return AppPreferences(
            pushNotificationsEnabled = settingsPrefs.getBoolean("pref_notif_enabled", true),
            alertSoundEnabled = settingsPrefs.getBoolean("pref_sound_enabled", true),
            alertVibrationEnabled = settingsPrefs.getBoolean("pref_vibration_enabled", true),
            defaultLowStockThreshold = settingsPrefs.getInt("pref_low_stock_thresh", 10),
            snoozeMinutes = settingsPrefs.getInt("pref_snooze_mins", 15),
            bloodSugarUnit = settingsPrefs.getString("pref_sugar_unit", "mg/dL") ?: "mg/dL",
            weightUnit = settingsPrefs.getString("pref_weight_unit", "lbs") ?: "lbs",
            bloodPressureUnit = settingsPrefs.getString("pref_bp_unit", "mmHg") ?: "mmHg",
            textSize = settingsPrefs.getString("pref_text_size", "NORMAL") ?: "NORMAL",
            colorPalette = settingsPrefs.getString("pref_color_palette", "ROYAL") ?: "ROYAL",
            appearance = "LIGHT",
            aiSeeMedicinesEnabled = settingsPrefs.getBoolean("pref_ai_see_medicines", false),
            aiSeenMedPrompt = settingsPrefs.getBoolean("pref_ai_seen_med_prompt", false),
            aiHideDisclaimer = settingsPrefs.getBoolean("pref_ai_hide_disclaimer", false)
        )
    }

    private fun saveAppPreferences(prefs: AppPreferences) {
        settingsPrefs.edit()
            .putBoolean("pref_notif_enabled", prefs.pushNotificationsEnabled)
            .putBoolean("pref_sound_enabled", prefs.alertSoundEnabled)
            .putBoolean("pref_vibration_enabled", prefs.alertVibrationEnabled)
            .putInt("pref_low_stock_thresh", prefs.defaultLowStockThreshold)
            .putInt("pref_snooze_mins", prefs.snoozeMinutes)
            .putString("pref_sugar_unit", prefs.bloodSugarUnit)
            .putString("pref_weight_unit", prefs.weightUnit)
            .putString("pref_bp_unit", prefs.bloodPressureUnit)
            .putString("pref_text_size", prefs.textSize)
            .putString("pref_color_palette", prefs.colorPalette)
            .putString("pref_appearance", prefs.appearance)
            .putBoolean("pref_ai_see_medicines", prefs.aiSeeMedicinesEnabled)
            .putBoolean("pref_ai_seen_med_prompt", prefs.aiSeenMedPrompt)
            .putBoolean("pref_ai_hide_disclaimer", prefs.aiHideDisclaimer)
            .apply()
    }

    fun updateColorPalette(paletteId: String) {
        val updated = _appPreferences.value.copy(colorPalette = paletteId)
        updateAppPreferences(updated)
    }

    fun updateAppearance(appearanceMode: String) {
        val updated = _appPreferences.value.copy(appearance = appearanceMode)
        updateAppPreferences(updated)
    }

    // Active Profile Health Context compiled for consented AI responses
    val activePatientHealthContext: StateFlow<PatientHealthContext> = combine(
        _currentProfile,
        allMedications,
        _familyMembers
    ) { profile, meds, members ->
        val profileMeds = meds.filter { it.profileName == profile }
            .map {
                PatientMedicationContext(
                    name = it.name,
                    dosage = it.dosage,
                    form = it.form,
                    timesOfDay = it.timesOfDay,
                    instructions = it.instructions
                )
            }
        val member = members.find { it.id == profile || it.name == profile }
        PatientHealthContext(
            profileName = profile,
            medications = profileMeds,
            allergies = member?.allergies ?: "None specified",
            age = member?.ageOrBirth ?: "Not specified"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PatientHealthContext("Self"))

    fun setIncludePatientContextConsent(enabled: Boolean) {
        _includePatientContextConsent.value = enabled
        val updated = _appPreferences.value.copy(aiSeeMedicinesEnabled = enabled, aiSeenMedPrompt = true)
        updateAppPreferences(updated)
    }

    fun dismissAiMedPrompt() {
        val updated = _appPreferences.value.copy(aiSeenMedPrompt = true)
        updateAppPreferences(updated)
    }

    fun setAiDisclaimerHidden(hidden: Boolean) {
        val updated = _appPreferences.value.copy(aiHideDisclaimer = hidden)
        updateAppPreferences(updated)
    }

    fun setAssistantLanguage(langCode: String) {
        _selectedAssistantLanguage.value = langCode
    }

    fun clearChatHistory() {
        _chatMessages.value = emptyList()
        speechManager.stop()
    }

    fun stopAnswerGeneration() {
        generationJob?.cancel()
        _isGeneratingAnswer.value = false
    }

    fun sendChatMessage(userQuery: String) {
        val query = userQuery.trim()
        if (query.isBlank()) return

        val lang = _selectedAssistantLanguage.value
        val userMsg = ChatMessage(
            role = ChatRole.USER,
            text = query,
            languageCode = lang
        )

        _chatMessages.update { it + userMsg }
        _isGeneratingAnswer.value = true

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val updatedMessages = _chatMessages.value
            val context = if (_includePatientContextConsent.value) activePatientHealthContext.value else null

            val answerText = geminiService.generateResponse(
                messages = updatedMessages,
                patientContext = context,
                includeContextConsent = _includePatientContextConsent.value,
                targetLanguage = lang
            )

            val assistantMsg = ChatMessage(
                role = ChatRole.ASSISTANT,
                text = answerText,
                languageCode = lang
            )

            _chatMessages.update { it + assistantMsg }
            _isGeneratingAnswer.value = false
        }
    }

    /**
     * Requirement 5: Connects onboarding preferences and authenticated profile
     * to the app's existing settings, primary family member, and notification schedules.
     */
    fun applyOnboardingProfile(session: AuthSession) {
        val currentAcc = _userAccount.value
        val updatedUser = currentAcc.copy(
            fullName = session.userProfile.fullName.ifBlank { currentAcc.fullName },
            email = session.email.ifBlank { currentAcc.email },
            isGuest = session.isGuest
        )
        updateUserAccount(updatedUser)

        // Update Primary family member profile name
        val members = _familyMembers.value.toMutableList()
        val primaryIdx = members.indexOfFirst { it.isPrimary }
        if (primaryIdx != -1) {
            val primary = members[primaryIdx]
            val newName = session.userProfile.fullName.ifBlank { primary.name }
            val newInitial = session.userProfile.firstName.take(1).uppercase().ifBlank { primary.initial }
            members[primaryIdx] = primary.copy(
                name = newName,
                initial = newInitial,
                ageOrBirth = session.userProfile.dateOfBirth.ifBlank { primary.ageOrBirth }
            )
            _familyMembers.value = members
            saveFamilyMembers(members)
        }

        // Apply onboarding preferences: notifications, reminders, interactions
        val currentPrefs = _appPreferences.value
        val updatedPrefs = currentPrefs.copy(
            pushNotificationsEnabled = session.preferences.getReminders,
            alertSoundEnabled = session.preferences.getReminders,
            aiSeeMedicinesEnabled = session.preferences.checkInteractions
        )
        updateAppPreferences(updatedPrefs)

        // Reschedule alarms if reminders enabled
        if (session.preferences.getReminders) {
            viewModelScope.launch(Dispatchers.IO) {
                val allMeds = repository.getAllMedicationsSync()
                MedicationAlarmScheduler.rescheduleAllAlarms(getApplication(), allMeds)
                MedicationWorkScheduler.rescheduleAllMedicationWork(getApplication(), allMeds)
            }
        }
    }

    /**
     * Requirement 6: Logs out user session and purges authenticated tokens.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            settingsPrefs.edit().remove("user_is_guest").apply()
            updateUserAccount(UserAccountInfo())
        }
    }

    /**
     * Requirement 6: Completely resets the user session and account information.
     */
    fun deleteAccount() {
        viewModelScope.launch {
            authRepository.deleteAccount()
            settingsPrefs.edit().remove("user_is_guest").apply()
            updateUserAccount(UserAccountInfo())
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.shutdown()
    }
}
