package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.Achievement
import com.example.data.model.AchievementIconType
import com.example.data.model.DoctorAppointmentEntity
import com.example.data.model.DoseLogEntity
import com.example.data.model.FamilyMember
import com.example.data.model.HealthDiaryEntity
import com.example.data.model.MedicationEntity
import com.example.data.model.StreakRewardInfo
import com.example.ui.components.MedTrackCapsuleLogo
import com.example.ui.components.MedicationDoseCalendarDialog
import com.example.ui.components.RechartsDashboardCard
import com.example.ui.components.StreakAchievementsSheet
import com.example.ui.theme.*
import com.example.ui.viewmodel.TodayDoseItem
import com.example.util.ChartShareHelper
import com.example.utils.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CareProfileScreen(
    currentProfile: String,
    onProfileSelected: (String) -> Unit,
    appointments: List<DoctorAppointmentEntity>,
    onToggleAppointment: (DoctorAppointmentEntity) -> Unit,
    onAddAppointment: (
        doctorName: String,
        specialty: String,
        dateTimeString: String,
        clinicLocation: String,
        reason: String,
        notes: String
    ) -> Unit,
    familyMembers: List<FamilyMember> = emptyList(),
    onVerifyFamilyMember: ((FamilyMember) -> Unit)? = null,
    onAddNewFamilyMemberClick: (() -> Unit)? = null,
    achievements: List<Achievement> = emptyList(),
    streakInfo: StreakRewardInfo? = null,
    onTriggerCelebration: (Achievement) -> Unit = {},
    diaryEntries: List<HealthDiaryEntity> = emptyList(),
    todayDoses: List<TodayDoseItem> = emptyList(),
    medications: List<MedicationEntity> = emptyList(),
    doseLogs: List<DoseLogEntity> = emptyList(),
    onTakeDose: (TodayDoseItem) -> Unit = {},
    onLogNewEntry: (
        mood: String,
        pain: Int,
        sys: Int,
        dia: Int,
        sugar: Int,
        weight: Float,
        symptoms: String,
        notes: String
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteEntry: (HealthDiaryEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddApptModal by remember { mutableStateOf(false) }
    var showStreakSheet by remember { mutableStateOf(false) }
    var showLogModal by remember { mutableStateOf(false) }
    var showDoseCalendar by remember { mutableStateOf(false) }
    var showExportModal by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<HealthDiaryEntity?>(null) }
    var selectedDayOption by remember { mutableStateOf("ALL_DAYS") }

    // Doctor appointment form state
    var doctorNameText by remember { mutableStateOf("") }
    var specialtyText by remember { mutableStateOf("") }
    var dateTimeText by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var questionsText by remember { mutableStateOf("") }
    var apptDateCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var apptLeadTime by remember { mutableStateOf("1 day before") }

    fun pickAppointmentDate() {
        val cal = apptDateCalendar
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                apptDateCalendar = cal
                val dateFmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(cal.time)
                val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
                dateTimeText = "$dateFmt • $timeFmt"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun pickAppointmentTime() {
        val cal = apptDateCalendar
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                apptDateCalendar = cal
                val dateFmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(cal.time)
                val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
                dateTimeText = "$dateFmt • $timeFmt"
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    val todayDateString = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val todayEntry = remember(diaryEntries, todayDateString) {
        diaryEntries.firstOrNull { it.dateString == todayDateString }
    }

    val displayedEntries = remember(diaryEntries, selectedDayOption) {
        if (selectedDayOption == "ALL_DAYS") {
            diaryEntries
        } else {
            diaryEntries.filter { it.dateString == selectedDayOption }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        containerColor = MedTealBg,
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                // Secondary FAB: Book Appointment
                SmallFloatingActionButton(
                    onClick = { showAddApptModal = true },
                    containerColor = Color.White,
                    contentColor = MedTealPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("care_add_appointment_fab")
                ) {
                    Icon(Icons.Default.Event, contentDescription = "Book Appointment", modifier = Modifier.size(20.dp))
                }

                // Primary FAB: Log Health Diary Entry
                ExtendedFloatingActionButton(
                    onClick = { showLogModal = true },
                    containerColor = MedTealPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(28.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = "Log Health Diary") },
                    text = {
                        Text(
                            text = if (todayEntry != null) "Update Health Diary" else "Log Health Diary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.testTag("care_log_health_diary_fab")
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    MedTrackCapsuleLogo(height = 34)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Care & Health Profiles",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MedTextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Doctor consultations, family care circles, vitals & daily health diary",
                        fontSize = 13.sp,
                        color = MedTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // SECTION 1: Family Profiles Circle Switcher & Verification Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .testTag("care_family_profiles_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Family Care Profiles",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedTextPrimary
                                )
                                Text(
                                    text = "HIPAA-protected multi-user care circles",
                                    fontSize = 11.sp,
                                    color = MedTextSecondary
                                )
                            }
                            if (onAddNewFamilyMemberClick != null) {
                                Button(
                                    onClick = onAddNewFamilyMemberClick,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).testTag("care_add_member_btn")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Family Members Profile Chips Row
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            familyMembers.forEach { member ->
                                val isSelected = currentProfile.equals(member.name, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MedTealSoft.copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) MedTealPrimary else MedBorderTeal)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onProfileSelected(member.name) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(if (member.isPrimary) MedTealSoft else Color(0xFFEFF6FF)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = member.name.take(1).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (member.isPrimary) MedTealPrimary else Color(0xFF1D4ED8),
                                                    fontSize = 15.sp
                                                )
                                            }
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = member.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MedTextPrimary
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (member.isPrimary) MedTealSoft else Color(0xFFDBEAFE)
                                                    ) {
                                                        Text(
                                                            text = member.relationship,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (member.isPrimary) MedTealDark else Color(0xFF1D4ED8),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = if (member.isVerified) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                                    ) {
                                                        Text(
                                                            text = if (member.isVerified) "Verified" else "Pending Device Check",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (member.isVerified) Color(0xFF166534) else Color(0xFFB45309),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    if (isSelected) {
                                                        Text("• Active Regimen", fontSize = 11.sp, color = MedTealPrimary, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        if (member.isPending && onVerifyFamilyMember != null) {
                                            Button(
                                                onClick = { onVerifyFamilyMember(member) },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp).testTag("care_verify_btn_${member.id}")
                                            ) {
                                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Verify", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: Adherence Streaks & Achievements Showcase
            item {
                val unlockedCount = achievements.count { it.isUnlocked }
                val totalCount = achievements.size.coerceAtLeast(1)
                val topAchievements = achievements.take(4)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clickable { showStreakSheet = true }
                        .testTag("care_profile_milestones_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Milestones & Achievements",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedTextPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    text = "$unlockedCount / $totalCount Unlocked",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D5FD6),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            topAchievements.forEach { ach ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .width(72.dp)
                                        .clickable {
                                            if (ach.isUnlocked) {
                                                onTriggerCelebration(ach)
                                            } else {
                                                showStreakSheet = true
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(if (ach.isUnlocked) Color(0xFFFEF3C7) else Color(0xFFF1F5F9))
                                            .border(
                                                1.dp,
                                                if (ach.isUnlocked) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val icon = when (ach.iconType) {
                                            AchievementIconType.WEEK_WARRIOR -> Icons.Default.Bolt
                                            AchievementIconType.THIRTY_DOSES -> Icons.Default.Medication
                                            AchievementIconType.FIRST_REFILL -> Icons.Default.Inventory2
                                            AchievementIconType.PERFECT_DAY -> Icons.Default.CheckCircle
                                            AchievementIconType.EARLY_BIRD -> Icons.Default.WbSunny
                                            AchievementIconType.CONSISTENCY_14 -> Icons.Default.Star
                                            AchievementIconType.HEALTH_DIARIST -> Icons.Default.MilitaryTech
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = ach.title,
                                            tint = if (ach.isUnlocked) Color(0xFFD97706) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text(
                                        text = ach.title,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ach.isUnlocked) MedTextPrimary else Color(0xFF94A3B8),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 12.sp,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: Upcoming Doctor Appointments
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Upcoming Doctor Appointments",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                        Text(
                            text = "${appointments.size} Scheduled",
                            fontSize = 12.sp,
                            color = MedTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = { showAddApptModal = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Book", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (appointments.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.EventNote, contentDescription = null, tint = MedTealPrimary, modifier = Modifier.size(32.dp))
                            Text("No upcoming appointments", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MedTextPrimary)
                            Text("Keep track of your clinical checkups and doctor notes here.", fontSize = 11.sp, color = MedTextSecondary)
                        }
                    }
                }
            } else {
                items(appointments, key = { it.id }) { appt ->
                    DoctorAppointmentCard(
                        appointment = appt,
                        onToggleComplete = { onToggleAppointment(appt) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }

            // =========================================================================
            // SUBMERGED DIARY CONTENT (SEAMLESSLY INTEGRATED BELOW CARE CONTENT)
            // =========================================================================
            item {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    color = MedBorderTeal,
                    thickness = 1.5.dp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Diary Section Header with quick actions
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MedTealSoft,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AutoStories,
                                            contentDescription = null,
                                            tint = MedTealPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Daily Health Diary & Vitals",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MedTextPrimary,
                                    letterSpacing = (-0.3).sp
                                )
                            }
                            Text(
                                text = "Log symptoms, mood, and vital metrics alongside your medication schedule",
                                fontSize = 11.sp,
                                color = MedTextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    ChartShareHelper.shareHealthTrendsChart(context, diaryEntries)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("care_share_chart_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Share", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val file = PdfGenerator.generateDiaryReport(context, diaryEntries, medications)
                                    file?.let {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Report"))
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MedTealPrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("care_export_pdf_button")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("PDF", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // Day Selector Dropdown Card
            item {
                DiaryDaysDropdownCard(
                    entries = diaryEntries,
                    selectedDayOption = selectedDayOption,
                    onSelectDayOption = { selectedDayOption = it },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Today's Medication Schedule Alongside Health Diary Card
            item {
                TodayMedicationScheduleDiaryCard(
                    todayDoses = todayDoses,
                    todayEntry = todayEntry,
                    onTakeDose = onTakeDose,
                    onOpenLogModal = { showLogModal = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // 30-Day Health Analytics Dashboard (Recharts + Trends)
            item {
                RechartsDashboardCard(
                    entries = diaryEntries,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Monthly Adherence Calendar Strip
            item {
                MonthlyAdherenceCalendarCard(
                    onOpenFullCalendar = { showDoseCalendar = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Medication Dose Schedule Calendar Card
            item {
                MedicationDoseCalendarSectionCard(
                    onOpenCalendar = { showDoseCalendar = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Blood Pressure & Sugar Trend Visual Chart
            item {
                VitalsTrendChartCard(
                    entries = diaryEntries,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Recent Diary Logs Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Health Diary Entries",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTextPrimary
                    )
                    Text(
                        text = "${displayedEntries.size} Recorded",
                        fontSize = 12.sp,
                        color = MedTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Diary Log Items (Filtered or All Days)
            if (displayedEntries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No records found for $selectedDayOption",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary
                            )
                            Button(
                                onClick = { selectedDayOption = "ALL_DAYS" },
                                colors = ButtonDefaults.buttonColors(containerColor = MedLightAccent),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Display All Days Data", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(displayedEntries, key = { it.id }) { entry ->
                    DiaryEntryCard(
                        entry = entry,
                        onDeleteClick = { entryToDelete = entry },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }

            // Bottom Action: View & Download Medication Dose Calendar
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF4EDE2)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFDCD2C4))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Medication Dose Calendar & Report",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2C2522)
                        )
                        Text(
                            text = "Access your full monthly medication calendar with daily Taken and Missed dose markers, and download the high-resolution image to save or share.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B5D56),
                            lineHeight = 16.sp
                        )
                        Button(
                            onClick = { showDoseCalendar = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB87355)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("open_dose_calendar_bottom_button_care")
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open & Download Dose Calendar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet: Add Doctor Appointment
    if (showAddApptModal) {
        AlertDialog(
            onDismissRequest = { showAddApptModal = false },
            title = {
                Text(
                    text = "Add Doctor Appointment",
                    fontWeight = FontWeight.Bold,
                    color = MedTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = doctorNameText,
                        onValueChange = { doctorNameText = it },
                        label = { Text("Doctor Name") },
                        placeholder = { Text("e.g. Dr. Emily Davis, MD") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = specialtyText,
                        onValueChange = { specialtyText = it },
                        label = { Text("Specialty") },
                        placeholder = { Text("e.g. Cardiology, Endocrinology") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Date & Time with Interactive Calendar and Clock
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = dateTimeText,
                            onValueChange = { dateTimeText = it },
                            label = { Text("Appointment Date & Time") },
                            placeholder = { Text("e.g. Wed, Nov 12, 2026 • 10:00 AM") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pickAppointmentDate() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp), tint = MedTealPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick Date", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedTealPrimary)
                            }
                            OutlinedButton(
                                onClick = { pickAppointmentTime() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MedTealPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick Time", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedTealPrimary)
                            }
                        }
                    }

                    // Push Reminder Lead Time Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Remind Me Before Appointment:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("1 day before", "12 hours before", "2 hours before").forEach { lead ->
                                val isSel = apptLeadTime == lead
                                FilterChip(
                                    selected = isSel,
                                    onClick = { apptLeadTime = lead },
                                    label = { Text(lead, fontSize = 10.sp, maxLines = 1) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = locationText,
                        onValueChange = { locationText = it },
                        label = { Text("Clinic Location") },
                        placeholder = { Text("e.g. City General Hospital, Suite 300") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("Reason for Visit") },
                        placeholder = { Text("e.g. Hypertension 6-month checkup") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = questionsText,
                        onValueChange = { questionsText = it },
                        label = { Text("Questions / Preparation Notes") },
                        placeholder = { Text("e.g. Ask about adjusting evening dosage") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = if (doctorNameText.isNotBlank()) doctorNameText.trim() else "Dr. Primary Physician"
                        val spec = if (specialtyText.isNotBlank()) specialtyText.trim() else "General Medicine"
                        val dt = if (dateTimeText.isNotBlank()) dateTimeText.trim() else "Upcoming Checkup"
                        val loc = if (locationText.isNotBlank()) locationText.trim() else "Clinic"
                        val rsn = if (reasonText.isNotBlank()) reasonText.trim() else "Routine Consultation"
                        onAddAppointment(name, spec, dt, loc, rsn, questionsText.trim())
                        showAddApptModal = false
                        doctorNameText = ""
                        specialtyText = ""
                        dateTimeText = ""
                        locationText = ""
                        reasonText = ""
                        questionsText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary)
                ) {
                    Text("Save Appointment", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddApptModal = false }) {
                    Text("Cancel", color = MedTextSecondary)
                }
            }
        )
    }

    // Modal Sheet: Log Health Entry (Symptoms, Mood, Notes & Vitals)
    if (showLogModal) {
        LogHealthEntryDialog(
            todayEntry = todayEntry,
            todayDoses = todayDoses,
            onDismiss = { showLogModal = false },
            onSaveEntry = onLogNewEntry
        )
    }

    // Modal Dialog: Delete Confirmation
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Diary Entry?",
                    fontWeight = FontWeight.Bold,
                    color = MedTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove the health entry for ${entryToDelete?.dateString}? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = MedTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        entryToDelete?.let { onDeleteEntry(it) }
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel", color = MedTextSecondary)
                }
            }
        )
    }

    // Modal Dialog: Medication Dose Calendar
    if (showDoseCalendar) {
        MedicationDoseCalendarDialog(
            profileName = currentProfile,
            medications = medications,
            doseLogs = doseLogs,
            onDismiss = { showDoseCalendar = false }
        )
    }

    // Streak and Achievements Bottom Sheet
    if (showStreakSheet && streakInfo != null) {
        StreakAchievementsSheet(
            streakInfo = streakInfo,
            achievements = achievements,
            onDismiss = { showStreakSheet = false },
            onTriggerCelebration = onTriggerCelebration
        )
    }
}

@Composable
fun DoctorAppointmentCard(
    appointment: DoctorAppointmentEntity,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (appointment.isCompleted) Color(0xFFF1F5F9) else MedTealSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = if (appointment.isCompleted) Color(0xFF94A3B8) else MedTealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = appointment.doctorName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (appointment.isCompleted) Color(0xFF64748B) else MedTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = appointment.specialty,
                            fontSize = 12.sp,
                            color = MedTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MedTealBg,
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
                ) {
                    Text(
                        text = appointment.dateTimeString,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }

            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MedTextSecondary, modifier = Modifier.size(15.dp))
                Text(appointment.clinicLocation, fontSize = 12.sp, color = MedTextSecondary)
            }

            // Reason & Notes for doctor
            Text(
                text = "Reason: ${appointment.reason}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MedTextPrimary
            )

            if (appointment.notes.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7).copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                        Text("Prep: ${appointment.notes}", fontSize = 11.sp, color = Color(0xFF92400E))
                    }
                }
            }

            // Complete Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onToggleComplete) {
                    Icon(
                        imageVector = if (appointment.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (appointment.isCompleted) MedSuccess else MedTealPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (appointment.isCompleted) "Completed" else "Mark Completed",
                        color = if (appointment.isCompleted) MedSuccess else MedTealPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
