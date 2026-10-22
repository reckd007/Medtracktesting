package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InjectionSiteLogEntity
import com.example.data.model.MedicationEntity
import com.example.ui.components.AlertRingtoneSelectorCard
import com.example.ui.components.MedTrackCapsuleLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.TodayDoseItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RemindersScreen(
    todayDoses: List<TodayDoseItem>,
    lowStockMeds: List<MedicationEntity>,
    latestInjection: InjectionSiteLogEntity?,
    recommendedSite: String,
    onTakeDose: (TodayDoseItem) -> Unit,
    onSnoozeDose: (TodayDoseItem) -> Unit,
    onSkipDose: (TodayDoseItem) -> Unit,
    onLogInjection: (siteCode: String, siteLabel: String) -> Unit,
    onRefillMed: (Long) -> Unit,
    onTestAlarm: () -> Unit = {},
    onTestImmediateNotification: () -> Unit = {},
    onRescheduleAllAlarms: () -> Unit = {},
    onRunRefillService: () -> Unit = {},
    onTestRefillPush: () -> Unit = {},
    onTestWorkManager: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedSiteCode by remember { mutableStateOf("ABDOMEN_RIGHT") }
    var selectedSiteLabel by remember { mutableStateOf("Abdomen - Right Lower") }
    var injectionRecordedBanner by remember { mutableStateOf(false) }
    var alarmFeedbackBanner by remember { mutableStateOf<String?>(null) }

    if (onBackClick != null) {
        BackHandler { onBackClick() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        containerColor = MedTealBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (onBackClick != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = CardDefaults.outlinedCardBorder().copy(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                            ),
                            modifier = Modifier
                                .clickable { onBackClick() }
                                .testTag("reminders_back_to_settings_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MedTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Back to Settings",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedTealPrimary,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    MedTrackCapsuleLogo(height = 34)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Reminders & Alarms",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MedTextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Daily push reminders via AlarmManager, custom ringtones & refill alerts",
                        fontSize = 13.sp,
                        color = MedTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Section 1: Android AlarmManager Daily Push System Card
            item {
                DailyAlarmManagerCard(
                    scheduledAlarmsCount = todayDoses.size,
                    onTestAlarm = {
                        onTestAlarm()
                        alarmFeedbackBanner = "AlarmManager reminder scheduled for 3s from now! Keep an eye on your status bar."
                    },
                    onTestImmediateNotification = {
                        onTestImmediateNotification()
                        alarmFeedbackBanner = "Push notification dispatched! Swipe down from top of screen to inspect."
                    },
                    onRescheduleAll = {
                        onRescheduleAllAlarms()
                        alarmFeedbackBanner = "All daily medication intake alarms successfully resynchronized with AlarmManager."
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Section 1B: WorkManager Background Task System Card
            item {
                WorkManagerDoseTaskCard(
                    scheduledTasksCount = todayDoses.size,
                    onTestWorkManager = {
                        onTestWorkManager()
                        alarmFeedbackBanner = "WorkManager task enqueued for 5s delay! Check notifications soon."
                    },
                    onRescheduleWorkManager = {
                        onRescheduleAllAlarms()
                        alarmFeedbackBanner = "WorkManager periodic scanner and unique dose tasks resynchronized."
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Section 2: Custom Alert Ringtones & Audio Settings
            item {
                AlertRingtoneSelectorCard(
                    onTestNotificationRequested = {
                        onTestImmediateNotification()
                        alarmFeedbackBanner = "Test push notification sent using your selected custom alert ringtone!"
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Section 2B: Alarm Reliability & Permissions
            item {
                com.example.ui.components.AlarmReliabilityCard(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Section 3: Injection Site Rotation Tracker
            item {
                InjectionSiteRotationCard(
                    latestInjection = latestInjection,
                    recommendedSite = recommendedSite,
                    selectedSiteCode = selectedSiteCode,
                    selectedSiteLabel = selectedSiteLabel,
                    onSelectSite = { code, label ->
                        selectedSiteCode = code
                        selectedSiteLabel = label
                    },
                    onRecordInjection = {
                        onLogInjection(selectedSiteCode, selectedSiteLabel)
                        injectionRecordedBanner = true
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Section 2: Background Refill Service & Smart Refill Alarms
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .shadow(1.5.dp, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                    )
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Inventory,
                                        contentDescription = null,
                                        tint = MedWarning,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "Background Refill Monitor",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Room DB Service & Push Alerts",
                                        fontSize = 11.sp,
                                        color = MedTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (lowStockMeds.isNotEmpty()) Color(0xFFFEF2F2) else Color(0xFFDCFCE7),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    width = 1.dp,
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (lowStockMeds.isNotEmpty()) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                                    )
                                ),
                                modifier = Modifier.widthIn(min = 68.dp)
                            ) {
                                Text(
                                    text = if (lowStockMeds.isNotEmpty()) "${lowStockMeds.size} refills" else "All Stocked",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (lowStockMeds.isNotEmpty()) Color(0xFF991B1B) else Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    softWrap = false,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Text(
                            text = "A background service checks medication quantities in the Room database against your user-defined threshold and sends a push notification when a refill is required.",
                            fontSize = 13.sp,
                            color = MedTextSecondary,
                            lineHeight = 18.sp
                        )

                        // Trigger Background Service & Push Test Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onRunRefillService()
                                    alarmFeedbackBanner = "RefillCheckBackgroundService launched! Checking Room DB quantities against your thresholds and dispatching push notifications."
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(40.dp)
                                    .testTag("run_refill_service_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run Refill Service", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    onTestRefillPush()
                                    alarmFeedbackBanner = "Refill push notification dispatched! Check notification shade for 'Log Refill (+30)' action."
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(40.dp)
                                    .testTag("test_refill_push_btn")
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp), tint = MedWarning)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Push", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedTextPrimary)
                            }
                        }

                        // Display list of medications below threshold
                        if (lowStockMeds.isNotEmpty()) {
                            Divider(color = MedBorderTeal.copy(alpha = 0.5f), thickness = 0.8.dp)
                            Text(
                                text = "Medications at or below your alert threshold:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            lowStockMeds.forEach { med ->
                                val unitName = when (med.form.lowercase(java.util.Locale.getDefault())) {
                                    "liquid", "solution", "suspension", "syrup" -> "mL"
                                    "inhaler" -> "puffs"
                                    "injection" -> "doses"
                                    "capsule" -> "capsules"
                                    "tablet" -> "tablets"
                                    else -> "units"
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFFBEB),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        width = 1.dp,
                                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFDE68A))
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${med.name} (${med.dosage})",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                            Text(
                                                text = "Stock: ${med.stockRemaining} $unitName • Threshold: ${med.refillThreshold} $unitName",
                                                fontSize = 11.sp,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { onRefillMed(med.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .widthIn(min = 86.dp)
                                                .height(34.dp)
                                        ) {
                                            Text("Refill +30", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Today's Scheduled Dose Alarms
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scheduled Dose Alarms",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTextPrimary
                    )
                    Text(
                        text = "${todayDoses.size} Alarms Today",
                        fontSize = 13.sp,
                        color = MedTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (todayDoses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AlarmOn,
                                contentDescription = null,
                                tint = MedTealPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "All alarms for today cleared",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MedTextPrimary
                            )
                        }
                    }
                }
            } else {
                items(todayDoses, key = { "alarm_${it.medication.id}_${it.scheduledTime}" }) { dose ->
                    ReminderAlarmCard(
                        dose = dose,
                        onConfirmTaken = { onTakeDose(dose) },
                        onSnooze = { onSnoozeDose(dose) },
                        onSkip = { onSkipDose(dose) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    if (injectionRecordedBanner) {
        AlertDialog(
            onDismissRequest = { injectionRecordedBanner = false },
            title = { Text("Injection Recorded", fontWeight = FontWeight.Bold, color = MedTextPrimary) },
            text = {
                Text(
                    text = "Site successfully logged: $selectedSiteLabel. Your next recommended injection zone has been updated.",
                    fontSize = 14.sp,
                    color = MedTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { injectionRecordedBanner = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (alarmFeedbackBanner != null) {
        AlertDialog(
            onDismissRequest = { alarmFeedbackBanner = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MedTealPrimary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "AlarmManager Notification System",
                    fontWeight = FontWeight.Bold,
                    color = MedTextPrimary,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = alarmFeedbackBanner ?: "",
                    fontSize = 14.sp,
                    color = MedTextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { alarmFeedbackBanner = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun InjectionSiteRotationCard(
    latestInjection: InjectionSiteLogEntity?,
    recommendedSite: String,
    selectedSiteCode: String,
    selectedSiteLabel: String,
    onSelectSite: (String, String) -> Unit,
    onRecordInjection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sites = listOf(
        Pair("ABDOMEN_LEFT", "Abdomen - Left Lower"),
        Pair("ABDOMEN_RIGHT", "Abdomen - Right Lower"),
        Pair("THIGH_LEFT", "Thigh - Left Outer"),
        Pair("THIGH_RIGHT", "Thigh - Right Outer"),
        Pair("ARM_LEFT", "Upper Arm - Left"),
        Pair("ARM_RIGHT", "Upper Arm - Right")
    )

    val lastDateFormatted = remember(latestInjection) {
        if (latestInjection != null) {
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(latestInjection.timestamp))
        } else "None recorded"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vaccines,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Injection Site Rotation",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                        Text(
                            text = "Prevent lipohypertrophy & tissue scar buildup",
                            fontSize = 12.sp,
                            color = MedTextSecondary
                        )
                    }
                }
            }

            // Recommended Zone Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MedTealSoft,
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MedTealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Recommended Next Zone:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MedTealDark
                        )
                        Text(
                            text = recommendedSite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MedTextPrimary
                        )
                    }
                }
            }

            // Interactive anatomical site selector
            Text(
                text = "Select Zone to Log:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MedTextPrimary
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sites.chunked(2).forEach { rowSites ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowSites.forEach { (code, label) ->
                            val isSelected = selectedSiteCode == code
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MedTealPrimary else MedTealBg,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    width = 1.dp,
                                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) MedTealPrimary else MedBorderTeal)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectSite(code, label) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MedTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MedTextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Last recorded footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last: ${latestInjection?.siteLabel ?: "None"} ($lastDateFormatted)",
                    fontSize = 11.sp,
                    color = MedTextSecondary
                )
                Button(
                    onClick = onRecordInjection,
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("record_injection_button")
                ) {
                    Text("Record Dose Here", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ReminderAlarmCard(
    dose: TodayDoseItem,
    onConfirmTaken: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTaken = dose.status == "TAKEN"
    val isSnoozed = dose.status == "SNOOZED"
    val isSkipped = dose.status == "SKIPPED"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.5.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MedTealSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MedTealPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = dose.medication.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                        Text(
                            text = "${dose.medication.dosage} • ${dose.timingSlot} schedule",
                            fontSize = 12.sp,
                            color = MedTextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MedTealBg,
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
                ) {
                    Text(
                        text = dose.scheduledTime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MedTextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (dose.medication.instructions.isNotEmpty()) {
                Text(
                    text = "Instruction: ${dose.medication.instructions}",
                    fontSize = 12.sp,
                    color = MedTextSecondary
                )
            }

            // Action row: Confirm, Snooze, Skip
            if (isTaken) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MedSuccess, modifier = Modifier.size(18.dp))
                        Text("Dose confirmed & recorded", color = Color(0xFF166534), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConfirmTaken,
                        modifier = Modifier.weight(1.4f).height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirm", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }

                    OutlinedButton(
                        onClick = onSnooze,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MedWarning),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFDE68A)))
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Snooze", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }

                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Skip", color = MedTextSecondary, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
fun DailyAlarmManagerCard(
    scheduledAlarmsCount: Int,
    onTestAlarm: () -> Unit,
    onTestImmediateNotification: () -> Unit,
    onRescheduleAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MedTealSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MedTealPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Daily Push Reminders",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Android AlarmManager System",
                            fontSize = 12.sp,
                            color = MedTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFDCFCE7),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF86EFAC))
                    ),
                    modifier = Modifier.widthIn(min = 64.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MedSuccess)
                        )
                        Text(
                            text = "Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534),
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }

            // Summary Info
            Text(
                text = "Exact daily push notifications are registered with Android's AlarmManager. They ring with sound and vibration even when the app is closed or your device is asleep in Doze mode.",
                fontSize = 13.sp,
                color = MedTextSecondary,
                lineHeight = 18.sp
            )

            // Features Pill Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MedTealSoft
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "$scheduledAlarmsCount Alarms",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MedTealDark
                        )
                        Text(
                            text = "Daily recurrence",
                            fontSize = 11.sp,
                            color = MedTextSecondary
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Actions Ready",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Take / Snooze in push",
                            fontSize = 11.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }

            // Action Buttons Row: Test Alarm (3s), Send Push Now, Sync Alarms
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTestAlarm,
                    modifier = Modifier.weight(1.3f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Alarm (3s)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onTestImmediateNotification,
                    modifier = Modifier.weight(1.2f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = MedTealPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Instant Push", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedTealPrimary)
                }

                IconButton(
                    onClick = onRescheduleAll,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MedTealBg)
                        .border(1.dp, MedBorderTeal, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Resync Alarms", tint = MedTealPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun WorkManagerDoseTaskCard(
    scheduledTasksCount: Int,
    onTestWorkManager: () -> Unit,
    onRescheduleWorkManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(24.dp))
            .testTag("workmanager_task_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkHistory,
                            contentDescription = null,
                            tint = Color(0xFF1D5FD6),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Background Dose Reminders",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Checks upcoming doses in the background and sets alarms",
                            fontSize = 11.sp,
                            color = MedTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFDCFCE7),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF86EFAC))),
                    modifier = Modifier.widthIn(min = 64.dp)
                ) {
                    Text(
                        text = "Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF166534),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        softWrap = false,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Description Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MedTealBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MedTealPrimary, modifier = Modifier.size(16.dp))
                        Text("Background Scanner: Active (Regularly checks your schedule)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedTextPrimary)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MedTealPrimary, modifier = Modifier.size(16.dp))
                        Text("Scheduled Dose Reminders: $scheduledTasksCount doses queued", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MedTextPrimary)
                    }
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTestWorkManager,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D5FD6)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Reminder (5s)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onRescheduleWorkManager,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = MedTealPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync Tasks", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedTealPrimary)
                }
            }
        }
    }
}
