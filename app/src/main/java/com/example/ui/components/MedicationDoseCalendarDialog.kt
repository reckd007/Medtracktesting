package com.example.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DoseLogEntity
import com.example.data.model.MedicationEntity
import com.example.ui.theme.*
import com.example.util.CalendarDayDoseInfo
import com.example.util.CalendarImageExportHelper
import com.example.util.DayDoseStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Interactive Monthly Medication Dose Calendar Dialog.
 * Visualizes upcoming and past medication doses, marking days with 'taken' or 'missed' statuses.
 * Includes prominent action to download/save the entire calendar grid as a high-resolution image.
 */
@Composable
fun MedicationDoseCalendarDialog(
    profileName: String = "Self",
    medications: List<MedicationEntity>,
    doseLogs: List<DoseLogEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentMonthCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    var selectedDateString by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }

    var isExportingImage by remember { mutableStateOf(false) }

    // Compute month days and their dose statuses
    val calendarDays = remember(currentMonthCalendar, medications, doseLogs) {
        computeMonthDoseInfo(currentMonthCalendar, medications, doseLogs)
    }

    val selectedDayInfo = remember(selectedDateString, calendarDays) {
        calendarDays.find { it.dateString == selectedDateString }
            ?: calendarDays.find { it.isToday }
            ?: calendarDays.firstOrNull { it.isCurrentMonth }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("medication_dose_calendar_dialog"),
            color = Color(0xFFFDFBF7), // Warm Sand
            border = CardDefaults.outlinedCardBorder().copy(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFDCD2C4))
            ),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar: Title + Download Image Action + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Medication Dose Calendar",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2C2522)
                        )
                        Text(
                            text = "Past taken & missed doses and upcoming schedule",
                            fontSize = 12.sp,
                            color = Color(0xFF6B5D56)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Download as Image Button
                        Button(
                            onClick = {
                                isExportingImage = true
                                CalendarImageExportHelper.exportAndDownloadCalendarImage(
                                    context = context,
                                    calendarMonth = currentMonthCalendar,
                                    days = calendarDays,
                                    profileName = profileName,
                                    medications = medications,
                                    onSuccess = { isExportingImage = false }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB87355)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("download_calendar_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download as Image",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Download Image",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Share Image Button
                        IconButton(
                            onClick = {
                                CalendarImageExportHelper.shareCalendarImage(
                                    context = context,
                                    calendarMonth = currentMonthCalendar,
                                    days = calendarDays,
                                    profileName = profileName,
                                    medications = medications
                                )
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF4EDE2))
                                .border(1.dp, Color(0xFFDCD2C4), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Calendar Image",
                                tint = Color(0xFFB87355),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Close Dialog Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF4EDE2))
                                .border(1.dp, Color(0xFFDCD2C4), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF2C2522),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFDCD2C4), thickness = 1.dp)

                // Month Navigation Strip: < Month Year >
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newCal = Calendar.getInstance().apply {
                                time = currentMonthCalendar.time
                                add(Calendar.MONTH, -1)
                            }
                            currentMonthCalendar = newCal
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF4EDE2))
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color(0xFF2C2522))
                    }

                    val monthTitleFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    Text(
                        text = monthTitleFormat.format(currentMonthCalendar.time),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C2522)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                currentMonthCalendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
                                selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB87355))
                        }

                        IconButton(
                            onClick = {
                                val newCal = Calendar.getInstance().apply {
                                    time = currentMonthCalendar.time
                                    add(Calendar.MONTH, 1)
                                }
                                currentMonthCalendar = newCal
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF4EDE2))
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color(0xFF2C2522))
                        }
                    }
                }

                // Month Status Summary Row (Taken, Missed, Upcoming, Adherence)
                val currentMonthDays = calendarDays.filter { it.isCurrentMonth }
                val takenDaysCount = currentMonthDays.count { it.status == DayDoseStatus.ALL_TAKEN }
                val missedDaysCount = currentMonthDays.count { it.status == DayDoseStatus.MISSED }
                val upcomingDaysCount = currentMonthDays.count { it.status == DayDoseStatus.UPCOMING }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalendarSummaryStatBadge(
                        label = "Taken",
                        count = "$takenDaysCount Days",
                        color = Color(0xFF4D7C0F),
                        bgColor = Color(0xFFECFCCB),
                        modifier = Modifier.weight(1f)
                    )
                    CalendarSummaryStatBadge(
                        label = "Missed",
                        count = "$missedDaysCount Days",
                        color = Color(0xFFB91C1C),
                        bgColor = Color(0xFFFEE2E2),
                        modifier = Modifier.weight(1f)
                    )
                    CalendarSummaryStatBadge(
                        label = "Upcoming",
                        count = "$upcomingDaysCount Days",
                        color = Color(0xFF0D9488),
                        bgColor = Color(0xFFCCFBF1),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Days of week header (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
                    dayNames.forEach { name ->
                        Text(
                            text = name,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B5D56)
                        )
                    }
                }

                // Monthly Calendar Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(calendarDays) { dayInfo ->
                        val isSelected = dayInfo.dateString == selectedDateString
                        CalendarDayCell(
                            dayInfo = dayInfo,
                            isSelected = isSelected,
                            onClick = {
                                selectedDateString = dayInfo.dateString
                            }
                        )
                    }
                }

                // Selected Date Detail Card
                if (selectedDayInfo != null) {
                    SelectedDayDetailCard(dayInfo = selectedDayInfo, medications = medications)
                }
            }
        }
    }
}

@Composable
private fun CalendarSummaryStatBadge(
    label: String,
    count: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.3f)))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = count, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayInfo: CalendarDayDoseInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cellBg = when {
        isSelected -> Color(0xFFF4EDE2)
        dayInfo.isToday -> Color(0xFFFFFBEB)
        dayInfo.isCurrentMonth -> Color(0xFFFDFBF7)
        else -> Color(0xFFF5EFE6).copy(alpha = 0.6f)
    }

    val borderColor = when {
        isSelected -> Color(0xFFB87355)
        dayInfo.isToday -> Color(0xFFD97706)
        else -> Color(0xFFDCD2C4).copy(alpha = 0.7f)
    }

    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = dayInfo.isCurrentMonth) { onClick() },
        color = cellBg,
        border = CardDefaults.outlinedCardBorder().copy(
            width = if (isSelected || dayInfo.isToday) 2.dp else 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(borderColor)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${dayInfo.dayNumber}",
                fontSize = 11.sp,
                fontWeight = if (dayInfo.isToday || isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (dayInfo.isCurrentMonth) Color(0xFF2C2522) else Color(0xFFA89680)
            )

            if (dayInfo.isCurrentMonth) {
                // Status Pill / Dot
                val (statusColor, statusText) = when (dayInfo.status) {
                    DayDoseStatus.ALL_TAKEN -> Pair(Color(0xFF4D7C0F), "T")
                    DayDoseStatus.MISSED -> Pair(Color(0xFFB91C1C), "M")
                    DayDoseStatus.PARTIAL -> Pair(Color(0xFFD97706), "P")
                    DayDoseStatus.UPCOMING -> Pair(Color(0xFF0D9488), "U")
                    DayDoseStatus.NO_DOSES -> Pair(Color.Transparent, "")
                }

                if (dayInfo.status != DayDoseStatus.NO_DOSES) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = statusText,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(16.dp))
                }
            } else {
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SelectedDayDetailCard(
    dayInfo: CalendarDayDoseInfo,
    medications: List<MedicationEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4EDE2)),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFDCD2C4)))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                Text(
                    text = displayDateFormat.format(dayInfo.date),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C2522)
                )

                val (badgeBg, badgeTextColor, badgeLabel) = when (dayInfo.status) {
                    DayDoseStatus.ALL_TAKEN -> Triple(Color(0xFFECFCCB), Color(0xFF4D7C0F), "ALL TAKEN")
                    DayDoseStatus.MISSED -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "MISSED")
                    DayDoseStatus.PARTIAL -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "PARTIAL")
                    DayDoseStatus.UPCOMING -> Triple(Color(0xFFCCFBF1), Color(0xFF0D9488), "UPCOMING")
                    DayDoseStatus.NO_DOSES -> Triple(Color(0xFFF1F5F9), Color(0xFF6B5D56), "NO DOSES")
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg,
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(badgeTextColor.copy(alpha = 0.4f)))
                ) {
                    Text(
                        text = badgeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Dose items breakdown
            if (dayInfo.takenDoses.isNotEmpty() || dayInfo.missedDoses.isNotEmpty() || dayInfo.upcomingDoses.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayInfo.takenDoses.forEach { doseText ->
                        DoseStatusRow(label = doseText, statusColor = Color(0xFF4D7C0F), statusLabel = "TAKEN")
                    }
                    dayInfo.missedDoses.forEach { doseText ->
                        DoseStatusRow(label = doseText, statusColor = Color(0xFFB91C1C), statusLabel = "MISSED")
                    }
                    dayInfo.upcomingDoses.forEach { doseText ->
                        DoseStatusRow(label = doseText, statusColor = Color(0xFF0D9488), statusLabel = "UPCOMING")
                    }
                }
            } else {
                Text(
                    text = "No scheduled medication doses on this day.",
                    fontSize = 12.sp,
                    color = Color(0xFF6B5D56)
                )
            }
        }
    }
}

@Composable
private fun DoseStatusRow(label: String, statusColor: Color, statusLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2C2522))
        }

        Text(
            text = statusLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}

/**
 * Computes calendar days for a 6-week (42-day) grid covering the given month,
 * evaluating dose logs and active medication schedules for taken, missed, and upcoming statuses.
 */
private fun computeMonthDoseInfo(
    monthCal: Calendar,
    medications: List<MedicationEntity>,
    doseLogs: List<DoseLogEntity>
): List<CalendarDayDoseInfo> {
    val result = mutableListOf<CalendarDayDoseInfo>()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayCal = Calendar.getInstance()
    val todayStr = dateFormat.format(todayCal.time)

    val cal = Calendar.getInstance().apply {
        time = monthCal.time
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val currentMonth = cal.get(Calendar.MONTH)
    val currentYear = cal.get(Calendar.YEAR)

    // Adjust to Monday start
    var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
    if (dayOfWeek < 0) dayOfWeek += 7

    cal.add(Calendar.DAY_OF_MONTH, -dayOfWeek)

    for (i in 0 until 42) {
        val date = cal.time
        val dateString = dateFormat.format(date)
        val isCurrentMonth = cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        val isToday = dateString == todayStr
        val isPast = cal.before(todayCal) && !isToday
        val isFuture = cal.after(todayCal) && !isToday

        // Check logs for this date
        val logsForDate = doseLogs.filter { it.dateString == dateString }

        val takenDoses = mutableListOf<String>()
        val missedDoses = mutableListOf<String>()
        val upcomingDoses = mutableListOf<String>()

        if (isPast || isToday) {
            logsForDate.forEach { log ->
                val desc = "${log.medicationName} (${log.scheduledTime})"
                if (log.status == "TAKEN") {
                    takenDoses.add(desc)
                } else if (log.status == "SKIPPED") {
                    missedDoses.add(desc)
                }
            }

            // If in current month and past, check if active medications had scheduled doses but were not logged
            if (isPast && medications.isNotEmpty()) {
                medications.forEach { med ->
                    val times = med.timesOfDay.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    times.forEach { t ->
                        val matchingLog = logsForDate.find { it.medicationId == med.id && it.scheduledTime == t }
                        if (matchingLog == null) {
                            // Unlogged dose in past is considered missed
                            missedDoses.add("${med.name} ($t)")
                        }
                    }
                }
            }
        }

        if (isToday) {
            // For today, also list pending scheduled doses
            medications.forEach { med ->
                val times = med.timesOfDay.split(",").map { it.trim() }.filter { it.isNotBlank() }
                times.forEach { t ->
                    val logged = logsForDate.find { it.medicationId == med.id && it.scheduledTime == t }
                    if (logged == null) {
                        upcomingDoses.add("${med.name} ($t)")
                    }
                }
            }
        }

        if (isFuture && isCurrentMonth) {
            medications.forEach { med ->
                val times = med.timesOfDay.split(",").map { it.trim() }.filter { it.isNotBlank() }
                times.forEach { t ->
                    upcomingDoses.add("${med.name} ($t)")
                }
            }
        }

        val status = when {
            !isCurrentMonth -> DayDoseStatus.NO_DOSES
            isFuture -> if (upcomingDoses.isNotEmpty()) DayDoseStatus.UPCOMING else DayDoseStatus.NO_DOSES
            isPast -> {
                when {
                    missedDoses.isEmpty() && takenDoses.isNotEmpty() -> DayDoseStatus.ALL_TAKEN
                    takenDoses.isNotEmpty() && missedDoses.isNotEmpty() -> DayDoseStatus.PARTIAL
                    missedDoses.isNotEmpty() && takenDoses.isEmpty() -> DayDoseStatus.MISSED
                    else -> DayDoseStatus.NO_DOSES
                }
            }
            isToday -> {
                when {
                    missedDoses.isNotEmpty() -> DayDoseStatus.PARTIAL
                    takenDoses.isNotEmpty() && upcomingDoses.isEmpty() -> DayDoseStatus.ALL_TAKEN
                    takenDoses.isNotEmpty() && upcomingDoses.isNotEmpty() -> DayDoseStatus.PARTIAL
                    upcomingDoses.isNotEmpty() -> DayDoseStatus.UPCOMING
                    else -> DayDoseStatus.NO_DOSES
                }
            }
            else -> DayDoseStatus.NO_DOSES
        }

        result.add(
            CalendarDayDoseInfo(
                date = date,
                dayNumber = cal.get(Calendar.DAY_OF_MONTH),
                dateString = dateString,
                isCurrentMonth = isCurrentMonth,
                isToday = isToday,
                isPast = isPast,
                isFuture = isFuture,
                status = status,
                takenDoses = takenDoses,
                missedDoses = missedDoses,
                upcomingDoses = upcomingDoses
            )
        )

        cal.add(Calendar.DAY_OF_MONTH, 1)
    }

    return result
}
