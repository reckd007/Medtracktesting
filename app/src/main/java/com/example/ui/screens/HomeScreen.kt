package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import com.example.data.model.Achievement
import com.example.data.model.MedicationEntity
import com.example.data.model.StreakRewardInfo
import com.example.ui.components.MedTrackHeader
import com.example.ui.components.StreakAchievementsSheet
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdherenceStats
import com.example.ui.viewmodel.TodayDoseItem

@Composable
fun HomeScreen(
    currentProfile: String,
    onProfileSelected: (String) -> Unit,
    todayDoses: List<TodayDoseItem>,
    adherenceStats: AdherenceStats,
    lowStockMeds: List<MedicationEntity> = emptyList(),
    selectedTimeFilter: String,
    onTimeFilterSelected: (String) -> Unit,
    onTakeDose: (TodayDoseItem, notes: String) -> Unit,
    onSnoozeDose: (TodayDoseItem) -> Unit,
    onSkipDose: (TodayDoseItem, reason: String) -> Unit,
    onRefillMedication: (Long, Int) -> Unit = { _, _ -> },
    onRunRefillService: () -> Unit = {},
    onAddMedicationClick: () -> Unit,
    familyMembers: List<com.example.data.model.FamilyMember> = emptyList(),
    isProfilePending: Boolean = false,
    onVerifyProfile: () -> Unit = {},
    onAddNewMemberClick: (() -> Unit)? = null,
    streakInfo: com.example.data.model.StreakRewardInfo? = null,
    achievements: List<com.example.data.model.Achievement> = emptyList(),
    onTriggerCelebration: (com.example.data.model.Achievement) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var doseForNoteDialog by remember { mutableStateOf<TodayDoseItem?>(null) }
    var doseNoteText by remember { mutableStateOf("") }

    var doseForSkipDialog by remember { mutableStateOf<TodayDoseItem?>(null) }
    var skipReasonText by remember { mutableStateOf("") }

    var showStreakSheet by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        containerColor = MedTealBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (isProfilePending) {
                        onVerifyProfile()
                    } else {
                        onAddMedicationClick()
                    }
                },
                containerColor = if (isProfilePending) Color(0xFFF59E0B) else MedTealPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                icon = {
                    Icon(
                        imageVector = if (isProfilePending) Icons.Default.Lock else Icons.Default.Add,
                        contentDescription = "Add medicine"
                    )
                },
                text = {
                    Text(
                        text = if (isProfilePending) "Verification Required" else "Add Medication",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .testTag("home_add_medication_fab")
                    .padding(bottom = 8.dp)
            )
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
                MedTrackHeader(
                    currentProfile = currentProfile,
                    onProfileSelected = onProfileSelected,
                    familyMembers = familyMembers,
                    onAddNewMemberClick = onAddNewMemberClick
                )
            }

            // Access Lockout Alert Banner for Pending Family Profiles
            if (isProfilePending) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFFBEB),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF59E0B))
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .testTag("home_access_lockout_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Access Lockout: Pending Verification",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Waiting for verification from $currentProfile's device. You cannot add or modify medicines for this profile until confirmed.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFB45309),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                            Button(
                                onClick = onVerifyProfile,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .testTag("home_verify_device_btn")
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify Device (Enter 6-digit Code / Approve)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Progress & Streak Hero Card
            item {
                AdherenceHeroCard(
                    stats = adherenceStats,
                    streakInfo = streakInfo,
                    onOpenStreakRewards = { showStreakSheet = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Low Stock Warning Alert Banner
            if (lowStockMeds.isNotEmpty()) {
                item {
                    HomeLowStockAlertBanner(
                        lowStockMeds = lowStockMeds,
                        onRefill = onRefillMedication,
                        onRunBackgroundCheck = onRunRefillService,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }

            // Doses List Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedTimeFilter == "All") "Today's Schedule" else "$selectedTimeFilter Schedule",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MedTealSoft,
                            border = CardDefaults.outlinedCardBorder().copy(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                            )
                        ) {
                            Text(
                                text = "${todayDoses.size} scheduled",
                                fontSize = 12.sp,
                                color = MedTealDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        text = "Filter by scheduled time of day:",
                        fontSize = 12.sp,
                        color = MedTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Timing Filter Chips Row
            item {
                TimeFilterChipsRow(
                    selected = selectedTimeFilter,
                    onSelect = onTimeFilterSelected,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Schedule Cards or Empty State
            if (todayDoses.isEmpty()) {
                item {
                    EmptyScheduleCard(
                        filter = selectedTimeFilter,
                        onAddClick = onAddMedicationClick,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                items(todayDoses, key = { "${it.medication.id}_${it.scheduledTime}" }) { dose ->
                    TodayMedicationCard(
                        dose = dose,
                        onTake = { onTakeDose(dose, "") },
                        onTakeWithNote = {
                            doseForNoteDialog = dose
                            doseNoteText = ""
                        },
                        onSnooze = { onSnoozeDose(dose) },
                        onSkip = {
                            doseForSkipDialog = dose
                            skipReasonText = ""
                        },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // Modal Dialog: Take with Dose Note
    val currentNoteDose = doseForNoteDialog
    if (currentNoteDose != null) {
        AlertDialog(
            onDismissRequest = { doseForNoteDialog = null },
            title = {
                Text(
                    text = "Confirm Dose & Add Note",
                    fontWeight = FontWeight.Bold,
                    color = MedTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${currentNoteDose.medication.name} (${currentNoteDose.medication.dosage}) at ${currentNoteDose.scheduledTime}",
                        fontSize = 14.sp,
                        color = MedTextSecondary
                    )
                    OutlinedTextField(
                        value = doseNoteText,
                        onValueChange = { doseNoteText = it },
                        placeholder = { Text("e.g. Taken after light meal, BP normal") },
                        label = { Text("Dose Note") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onTakeDose(currentNoteDose, doseNoteText)
                        doseForNoteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary)
                ) {
                    Text("Confirm Taken", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { doseForNoteDialog = null }) {
                    Text("Cancel", color = MedTextSecondary)
                }
            }
        )
    }

    // Modal Dialog: Skip Dose Reason
    val currentSkipDose = doseForSkipDialog
    if (currentSkipDose != null) {
        AlertDialog(
            onDismissRequest = { doseForSkipDialog = null },
            title = {
                Text(
                    text = "Skip Dose",
                    fontWeight = FontWeight.Bold,
                    color = MedTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Are you sure you want to skip ${currentSkipDose.medication.name}?",
                        fontSize = 14.sp,
                        color = MedTextSecondary
                    )
                    OutlinedTextField(
                        value = skipReasonText,
                        onValueChange = { skipReasonText = it },
                        label = { Text("Reason for skipping") },
                        placeholder = { Text("e.g. Feeling unwell, doctor advised pause") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSkipDose(currentSkipDose, skipReasonText)
                        doseForSkipDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedWarning)
                ) {
                    Text("Skip Dose", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { doseForSkipDialog = null }) {
                    Text("Cancel", color = MedTextSecondary)
                }
            }
        )
    }

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
fun AdherenceHeroCard(
    stats: AdherenceStats,
    streakInfo: StreakRewardInfo? = null,
    onOpenStreakRewards: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_flame")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .clickable { onOpenStreakRewards() }
            .testTag("adherence_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Today's Adherence",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MedTextSecondary
                        )
                        if (streakInfo?.streakLevel != null) {
                            Text(
                                text = "• ${streakInfo.streakLevel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                    Text(
                        text = "${stats.takenCount} of ${stats.totalCount} taken today",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MedTextPrimary
                    )
                }

                // Interactive Streak Badge with Fire Icon
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF3C7),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFDE68A))),
                    modifier = Modifier
                        .scale(flameScale)
                        .clickable { onOpenStreakRewards() }
                        .testTag("streak_counter_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "${stats.streakDays} Day Streak",
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Progress Bar with rounded ends
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (stats.percentage >= 100) "All doses complete! Streak extended!" else "Stay on track to keep streak alive",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (stats.percentage >= 100) MedSuccess else MedTextSecondary
                    )
                    Text(
                        text = "${stats.percentage}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTealPrimary
                    )
                }

                LinearProgressIndicator(
                    progress = { stats.percentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (stats.percentage >= 100) MedSuccess else MedTealPrimary,
                    trackColor = MedTealSoft
                )
            }

            // Mini 7-Day Consecutive Days Streak Roadmap Dots
            if (streakInfo != null && streakInfo.weekStatus.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        streakInfo.weekStatus.forEach { day ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = day.dayLabel,
                                    fontSize = 9.sp,
                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (day.isToday) Color(0xFF1D5FD6) else Color(0xFF64748B)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                day.isCompleted -> Color(0xFFDCFCE7)
                                                day.isToday -> Color(0xFFFEF3C7)
                                                else -> Color(0xFFF1F5F9)
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = when {
                                                day.isCompleted -> Color(0xFF22C55E)
                                                day.isToday -> Color(0xFFF59E0B)
                                                else -> Color(0xFFCBD5E1)
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (day.isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    } else if (day.isToday) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Open rewards hint button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.clickable { onOpenStreakRewards() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFF1D5FD6),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Rewards",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D5FD6)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeFilterChipsRow(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf("All", "Morning", "Afternoon", "Evening", "Bedtime")

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = selected.equals(filter, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        text = filter,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MedTealPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = MedTextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) MedTealPrimary else MedBorderTeal
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("filter_chip_$filter")
            )
        }
    }
}

@Composable
fun TodayMedicationCard(
    dose: TodayDoseItem,
    onTake: () -> Unit,
    onTakeWithNote: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isTaken = dose.status == "TAKEN"
    val isSnoozed = dose.status == "SNOOZED"
    val isSkipped = dose.status == "SKIPPED"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isTaken) 0.5.dp else 2.dp,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTaken) Color(0xFFFAFAFA) else Color.White
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isTaken) Color(0xFFE2E8F0) else MedBorderTeal
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pill Icon Badge
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(dose.medication.colorHex)).copy(alpha = 0.15f)
                                } catch (_: Exception) {
                                    MedTealSoft
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (dose.medication.form.lowercase()) {
                                "injection" -> Icons.Default.Vaccines
                                "liquid" -> Icons.Default.WaterDrop
                                else -> Icons.Default.Medication
                            },
                            contentDescription = dose.medication.form,
                            tint = try {
                                Color(android.graphics.Color.parseColor(dose.medication.colorHex))
                            } catch (_: Exception) {
                                MedTealPrimary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = dose.medication.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTaken) Color(0xFF64748B) else MedTextPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${dose.medication.dosage} • ${dose.medication.form}",
                                fontSize = 13.sp,
                                color = MedTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Time and options button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MedTealBg,
                        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
                    ) {
                        Text(
                            text = dose.scheduledTime,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More dose options",
                                tint = MedTextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Log with custom note") },
                                leadingIcon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onTakeWithNote()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Snooze for 15 mins") },
                                leadingIcon = { Icon(Icons.Outlined.Snooze, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onSnooze()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Skip this dose", color = MedWarning) },
                                leadingIcon = { Icon(Icons.Outlined.Cancel, contentDescription = null, tint = MedWarning) },
                                onClick = {
                                    menuExpanded = false
                                    onSkip()
                                }
                            )
                        }
                    }
                }
            }

            // Instructions snippet
            if (dose.medication.instructions.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MedTealBg.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MedTealPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = dose.medication.instructions,
                        fontSize = 12.sp,
                        color = MedTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Status feedback or Take Button
            when {
                isTaken -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MedSuccessBg)
                            .padding(vertical = 12.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Taken",
                                tint = MedSuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ALL GOOD • Taken${if (dose.loggedTime.isNotEmpty()) " at ${dose.loggedTime}" else ""}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedSuccessGreen
                            )
                        }
                        if (dose.notes.isNotEmpty()) {
                            Text(
                                text = "“${dose.notes}”",
                                fontSize = 12.sp,
                                color = MedSuccessGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                isSnoozed -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MedicoTheme.colors.warningBg)
                            .padding(vertical = 12.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = "Snoozed",
                                tint = MedicoTheme.colors.warningText,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "PLAN AHEAD • Snoozed (+15m)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedicoTheme.colors.warningText
                            )
                        }
                        Button(
                            onClick = onTake,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Take Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }

                isSkipped -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MedicoTheme.colors.alertBg)
                            .padding(vertical = 12.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Skipped",
                                tint = MedicoTheme.colors.alertText,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ACT NOW • Dose Skipped",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedicoTheme.colors.alertText
                            )
                        }
                        TextButton(onClick = onTake) {
                            Text("Undo & Take", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }

                else -> {
                    // Large, easy-to-tap Take Button
                    Button(
                        onClick = onTake,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("take_button_${dose.medication.id}_${dose.scheduledTime}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Take Dose",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyScheduleCard(
    filter: String,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MedTealSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircleOutline,
                    contentDescription = null,
                    tint = MedTealPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                text = if (filter == "All") "No medications scheduled today" else "No $filter doses found",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MedTextPrimary
            )
            Text(
                text = "Add a new prescription or vitamin to set up your personalized schedule and reminders.",
                fontSize = 13.sp,
                color = MedTextSecondary,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("Add New Medication", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HomeLowStockAlertBanner(
    lowStockMeds: List<MedicationEntity>,
    onRefill: (Long, Int) -> Unit,
    onRunBackgroundCheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .testTag("home_low_stock_alert_banner"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = lowStockSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(lowStockBorder)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = lowStockIconBg,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = "Warning",
                            tint = lowStockIcon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Low Stock Alert: ${lowStockMeds.size} Item${if (lowStockMeds.size > 1) "s" else ""} Below Threshold",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = lowStockText
                    )
                    Text(
                        text = "Supply is running low based on your custom refill threshold.",
                        fontSize = 12.sp,
                        color = lowStockSecondaryText
                    )
                }

                TextButton(
                    onClick = onRunBackgroundCheck,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("home_run_bg_check_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = lowStockIcon,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Check",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = lowStockText
                    )
                }
            }

            HorizontalDivider(color = lowStockBorder, thickness = 1.dp)

            // Individual low-stock medication items
            lowStockMeds.forEach { med ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(lowStockItemSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, lowStockBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${med.name} (${med.dosage})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = lowStockText
                        )
                        val unit = when (med.form.lowercase(java.util.Locale.getDefault())) {
                            "liquid", "syrup" -> "mL"
                            "inhaler" -> "puffs"
                            "injection" -> "doses"
                            else -> "pills"
                        }
                        Text(
                            text = "Remaining: ${med.stockRemaining} $unit • Alert Threshold: ${med.refillThreshold} $unit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = lowStockSecondaryText
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onRefill(med.id, 30) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 84.dp)
                            .height(32.dp)
                            .testTag("refill_home_btn_${med.id}")
                    ) {
                        Text("+ Refill (30)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}
