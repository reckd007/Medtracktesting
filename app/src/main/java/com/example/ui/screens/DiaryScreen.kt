package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DoseLogEntity
import com.example.data.model.HealthDiaryEntity
import com.example.data.model.MedicationEntity
import com.example.ui.components.MedTrackCapsuleLogo
import com.example.ui.components.MedicationDoseCalendarDialog
import com.example.ui.components.RechartsDashboardCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.TodayDoseItem
import com.example.util.ChartShareHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DiaryScreen(
    entries: List<HealthDiaryEntity>,
    todayDoses: List<TodayDoseItem> = emptyList(),
    medications: List<MedicationEntity> = emptyList(),
    doseLogs: List<DoseLogEntity> = emptyList(),
    profileName: String = "Self",
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
    ) -> Unit,
    onDeleteEntry: (HealthDiaryEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLogModal by remember { mutableStateOf(false) }
    var showExportModal by remember { mutableStateOf(false) }
    var showDoseCalendar by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<HealthDiaryEntity?>(null) }

    val todayDateString = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val todayEntry = remember(entries, todayDateString) {
        entries.firstOrNull { it.dateString == todayDateString }
    }

    // Dropdown day selection filter ("ALL_DAYS" or specific date string)
    var selectedDayOption by remember { mutableStateOf("ALL_DAYS") }

    val displayedEntries = remember(entries, selectedDayOption) {
        if (selectedDayOption == "ALL_DAYS") {
            entries
        } else {
            entries.filter { it.dateString == selectedDayOption }
        }
    }

    // Form inputs for logging
    var moodSelected by remember { mutableStateOf("Great") }
    var painSliderValue by remember { mutableFloatStateOf(2f) }
    var sysBpText by remember { mutableStateOf("") }
    var diaBpText by remember { mutableStateOf("") }
    var bloodSugarText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var symptomsText by remember { mutableStateOf("") }
    var diaryNotesText by remember { mutableStateOf("") }

    fun openLogModalPrePopulated(entry: HealthDiaryEntity?) {
        if (entry != null) {
            moodSelected = entry.mood
            painSliderValue = entry.painLevel.toFloat()
            sysBpText = if (entry.systolicBp > 0) entry.systolicBp.toString() else ""
            diaBpText = if (entry.diastolicBp > 0) entry.diastolicBp.toString() else ""
            bloodSugarText = if (entry.bloodSugar > 0) entry.bloodSugar.toString() else ""
            weightText = if (entry.weightKg > 0) entry.weightKg.toString() else ""
            symptomsText = entry.symptoms
            diaryNotesText = entry.notes
        } else {
            moodSelected = "Great"
            painSliderValue = 0f
            sysBpText = ""
            diaBpText = ""
            bloodSugarText = ""
            weightText = ""
            symptomsText = ""
            diaryNotesText = ""
        }
        showLogModal = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MedTealBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { openLogModalPrePopulated(todayEntry) },
                containerColor = MedTealPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = "Log Vitals") },
                text = {
                    Text(
                        if (todayEntry != null) "Update Today's Diary" else "Log Today's Health",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .testTag("diary_log_vitals_fab")
                    .padding(bottom = 8.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MedTrackCapsuleLogo(height = 34)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    ChartShareHelper.shareHealthTrendsChart(context, entries)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("download_share_chart_top_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Chart", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val file = com.example.utils.PdfGenerator.generateDiaryReport(context, entries, medications)
                                    file?.let {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Share Report"))
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MedTealPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("export_pdf_button")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Daily Health Diary",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MedTextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Log symptoms, mood, and personal notes alongside your daily medication intake schedule",
                        fontSize = 13.sp,
                        color = MedTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // SECTION: Day Selector Dropdown Menu displaying All Days Data
            item {
                DiaryDaysDropdownCard(
                    entries = entries,
                    selectedDayOption = selectedDayOption,
                    onSelectDayOption = { selectedDayOption = it },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // SECTION: Today's Medication Schedule Alongside Health Diary
            item {
                TodayMedicationScheduleDiaryCard(
                    todayDoses = todayDoses,
                    todayEntry = todayEntry,
                    onTakeDose = onTakeDose,
                    onOpenLogModal = { openLogModalPrePopulated(todayEntry) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }



            // SECTION: 30-Day Health Analytics Dashboard (Recharts + Native Trends & Share)
            item {
                RechartsDashboardCard(
                    entries = entries,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Monthly Adherence Calendar Strip
            item {
                MonthlyAdherenceCalendarCard(
                    onOpenFullCalendar = { showDoseCalendar = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // SECTION: Medication Dose Schedule Calendar Card
            item {
                MedicationDoseCalendarSectionCard(
                    onOpenCalendar = { showDoseCalendar = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Blood Pressure & Sugar Trend Visual Chart
            item {
                VitalsTrendChartCard(
                    entries = entries,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Recent Logs Section Header with Dropdown Indicator
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (selectedDayOption == "ALL_DAYS") "All Days Health History" else "History for $selectedDayOption",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                        if (selectedDayOption != "ALL_DAYS") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MedLightAccentTint,
                                modifier = Modifier.clickable { selectedDayOption = "ALL_DAYS" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Show All Days",
                                        tint = MedLightAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Show All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedLightAccent
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "${displayedEntries.size} of ${entries.size} Days",
                        fontSize = 13.sp,
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
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No records found for $selectedDayOption",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary
                            )
                            Button(
                                onClick = { selectedDayOption = "ALL_DAYS" },
                                colors = ButtonDefaults.buttonColors(containerColor = MedLightAccent)
                            ) {
                                Text("Display All Days Data", fontWeight = FontWeight.Bold)
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
                                .testTag("open_dose_calendar_bottom_button")
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

    // Modal Sheet: Log Symptoms, Mood, Notes & Vitals
    if (showLogModal) {
        val quickSymptoms = listOf(
            "Feeling Great",
            "Headache",
            "Nausea",
            "Fatigue",
            "Dizziness",
            "Drowsiness",
            "Dry Mouth",
            "Stomach Ache",
            "Insomnia",
            "Joint Pain",
            "Brain Fog"
        )

        AlertDialog(
            onDismissRequest = { showLogModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MedTealSoft,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MedTealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (todayEntry != null) "Update Today's Diary" else "Log Today's Health Diary",
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Record mood, symptoms & notes with your meds",
                            fontSize = 12.sp,
                            color = MedTextSecondary
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Context: Medication Schedule for Today
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MedTealSoft.copy(alpha = 0.5f)),
                            border = CardDefaults.outlinedCardBorder().copy(
                                width = 1.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Medication,
                                        contentDescription = null,
                                        tint = MedTealPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Today's Medication Schedule Context",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MedTealPrimary
                                    )
                                }

                                if (todayDoses.isEmpty()) {
                                    Text(
                                        text = "No medications scheduled for today",
                                        fontSize = 12.sp,
                                        color = MedTextSecondary
                                    )
                                } else {
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(todayDoses.take(4)) { dose ->
                                            val isTaken = dose.status == "TAKEN"
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isTaken) Color(0xFFDCFCE7) else Color.White,
                                                border = CardDefaults.outlinedCardBorder().copy(
                                                    width = 1.dp,
                                                    brush = androidx.compose.ui.graphics.SolidColor(
                                                        if (isTaken) Color(0xFF86EFAC) else MedBorderTeal
                                                    )
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                                        contentDescription = null,
                                                        tint = if (isTaken) MedSuccess else MedWarning,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = "${dose.medication.name} (${dose.scheduledTime})",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MedTextPrimary,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Mood Selector
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "1. How are you feeling today? (Mood)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(
                                    Pair("Great", "5/5"),
                                    Pair("Good", "4/5"),
                                    Pair("Neutral", "3/5"),
                                    Pair("Low", "2/5"),
                                    Pair("Rough", "1/5")
                                ).forEach { (mood, score) ->
                                    val isSelected = moodSelected == mood
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MedTealSoft else Color(0xFFF8FAFC))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) MedTealPrimary else Color(0xFFE2E8F0),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { moodSelected = mood }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            score,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) MedTealPrimary else MedTextPrimary
                                        )
                                        Text(
                                            mood,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MedTealPrimary else MedTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Symptoms & Side Effects
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "2. Symptoms & Side Effects",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedTextPrimary
                                )
                                Text(
                                    "Tap to toggle quick tags",
                                    fontSize = 11.sp,
                                    color = MedTextSecondary
                                )
                            }

                            // Quick symptom tags
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(quickSymptoms) { symptom ->
                                    val isContained = symptomsText.contains(symptom, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isContained) MedTealSoft else Color.White,
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            width = 1.dp,
                                            brush = androidx.compose.ui.graphics.SolidColor(
                                                if (isContained) MedTealPrimary else MedBorderTeal
                                            )
                                        ),
                                        modifier = Modifier.clickable {
                                            if (symptom == "Feeling Great") {
                                                symptomsText = "No side effects, feeling great"
                                            } else if (isContained) {
                                                symptomsText = symptomsText
                                                    .replace(", $symptom", "")
                                                    .replace("$symptom, ", "")
                                                    .replace(symptom, "")
                                                    .trim()
                                                if (symptomsText.isEmpty()) {
                                                    symptomsText = "None"
                                                }
                                            } else {
                                                symptomsText = if (symptomsText.isBlank() || symptomsText == "None" || symptomsText == "Well rested, no side effects") {
                                                    symptom
                                                } else {
                                                    "$symptomsText, $symptom"
                                                }
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = symptom,
                                            fontSize = 11.sp,
                                            fontWeight = if (isContained) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isContained) MedTealDark else MedTextPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = symptomsText,
                                onValueChange = { symptomsText = it },
                                label = { Text("Describe Symptoms / Medication Effects") },
                                placeholder = { Text("e.g. Mild headache 30 min after morning pill") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Personal Notes & Reflections
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "3. Daily Notes & Reflections",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary
                            )
                            OutlinedTextField(
                                value = diaryNotesText,
                                onValueChange = { diaryNotesText = it },
                                label = { Text("Personal Notes, Triggers, Diet & Routine") },
                                placeholder = { Text("e.g. Slept 8 hours, ate balanced meals, feeling well.") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Pain Scale Slider (0 to 10)
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "4. Pain Scale:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedTextPrimary
                                )
                                val painLevelInt = painSliderValue.toInt()
                                val painDescriptor = when (painLevelInt) {
                                    0 -> "0/10 (None)"
                                    in 1..3 -> "$painLevelInt/10 (Mild)"
                                    in 4..6 -> "$painLevelInt/10 (Moderate)"
                                    else -> "$painLevelInt/10 (Severe)"
                                }
                                Text(
                                    text = painDescriptor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (painLevelInt <= 2) MedSuccess else if (painLevelInt <= 5) MedWarning else Color(0xFFDC2626)
                                )
                            }
                            Slider(
                                value = painSliderValue,
                                onValueChange = { painSliderValue = it },
                                valueRange = 0f..10f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = MedTealPrimary,
                                    activeTrackColor = MedTealPrimary
                                )
                            )
                        }
                    }

                    // Vitals Row: Blood Pressure, Sugar, Weight
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "5. Clinical Vitals",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = sysBpText,
                                    onValueChange = { sysBpText = it },
                                    label = { Text("Systolic BP") },
                                    placeholder = { Text("e.g. 120 mmHg") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = diaBpText,
                                    onValueChange = { diaBpText = it },
                                    label = { Text("Diastolic BP") },
                                    placeholder = { Text("e.g. 80 mmHg") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = bloodSugarText,
                                    onValueChange = { bloodSugarText = it },
                                    label = { Text("Sugar (mg/dL)") },
                                    placeholder = { Text("e.g. 95 mg/dL") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = weightText,
                                    onValueChange = { weightText = it },
                                    label = { Text("Weight (kg)") },
                                    placeholder = { Text("e.g. 72.0 kg") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sys = sysBpText.toIntOrNull() ?: 120
                        val dia = diaBpText.toIntOrNull() ?: 80
                        val sugar = bloodSugarText.toIntOrNull() ?: 95
                        val weight = weightText.toFloatOrNull() ?: 70.0f
                        onLogNewEntry(
                            moodSelected,
                            painSliderValue.toInt(),
                            sys,
                            dia,
                            sugar,
                            weight,
                            symptomsText.trim(),
                            diaryNotesText.trim()
                        )
                        showLogModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Health Entry", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogModal = false }) {
                    Text("Cancel", color = MedTextSecondary)
                }
            }
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

    // Modal Dialog: Export Report PDF Preview
    if (showExportModal) {
        AlertDialog(
            onDismissRequest = { showExportModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MedTealPrimary)
                    Text("Monthly Clinical Report Export", fontWeight = FontWeight.Bold, color = MedTextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MedTealBg, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MEDTRACK PATIENT CLINICAL SUMMARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MedTealPrimary,
                        letterSpacing = 1.sp
                    )
                    HorizontalDivider(color = MedBorderTeal)
                    Text("• Overall 30-Day Medication Adherence: 94.2%", fontSize = 12.sp, color = MedTextPrimary)
                    Text("• Mean Blood Pressure: 121 / 79 mmHg (Normal Range)", fontSize = 12.sp, color = MedTextPrimary)
                    Text("• Mean Fasting Blood Glucose: 96 mg/dL", fontSize = 12.sp, color = MedTextPrimary)
                    Text("• Average Pain Score: 1.8 / 10", fontSize = 12.sp, color = MedTextPrimary)
                    Text("• Active Regimen: Amoxicillin, Metformin, Lisinopril, Vitamin D3", fontSize = 12.sp, color = MedTextPrimary)
                    Text("• Symptoms Logged: Stable, zero adverse reactions noted", fontSize = 12.sp, color = MedTextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ready to download or share directly with your physician.",
                        fontSize = 11.sp,
                        color = MedTextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExportModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary)
                ) {
                    Text("Download PDF", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportModal = false }) {
                    Text("Close", color = MedTextSecondary)
                }
            }
        )
    }

    // Modal Dialog: Medication Dose Calendar with 'Taken'/'Missed' statuses and image download
    if (showDoseCalendar) {
        MedicationDoseCalendarDialog(
            profileName = profileName,
            medications = medications,
            doseLogs = doseLogs,
            onDismiss = { showDoseCalendar = false }
        )
    }
}

/**
 * Dedicated Card displaying today's medication intake schedule alongside today's health diary status.
 */
@Composable
fun TodayMedicationScheduleDiaryCard(
    todayDoses: List<TodayDoseItem>,
    todayEntry: HealthDiaryEntity?,
    onTakeDose: (TodayDoseItem) -> Unit,
    onOpenLogModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayDateFormatted = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    val takenCount = todayDoses.count { it.status == "TAKEN" }
    val totalCount = todayDoses.size
    val progressPct = if (totalCount > 0) ((takenCount.toFloat() / totalCount) * 100).toInt() else 100

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Date & Schedule Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Medication & Health",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTextPrimary
                    )
                    Text(
                        text = todayDateFormatted,
                        fontSize = 12.sp,
                        color = MedTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (progressPct == 100 && totalCount > 0) Color(0xFFDCFCE7) else MedTealSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (progressPct == 100 && totalCount > 0) Icons.Default.CheckCircle else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (progressPct == 100 && totalCount > 0) MedSuccess else MedTealPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (totalCount > 0) "$takenCount/$totalCount Taken" else "No Doses",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (progressPct == 100 && totalCount > 0) Color(0xFF166534) else MedTealDark
                        )
                    }
                }
            }

            // Doses strip for today
            if (todayDoses.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Scheduled Intake Today:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTextPrimary
                    )

                    todayDoses.forEach { dose ->
                        val isTaken = dose.status == "TAKEN"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isTaken) Color(0xFFF0FDF4) else Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isTaken) Color(0xFFBBF7D0) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isTaken) Color(0xFFDCFCE7) else MedTealSoft,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isTaken) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Taken",
                                                tint = MedSuccess,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                text = dose.scheduledTime.take(2),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MedTealPrimary
                                            )
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${dose.medication.name} • ${dose.medication.dosage}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "${dose.scheduledTime} (${dose.timingSlot}) • ${dose.medication.instructions}",
                                        fontSize = 11.sp,
                                        color = MedTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            if (!isTaken) {
                                Button(
                                    onClick = { onTakeDose(dose) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .widthIn(min = 72.dp)
                                        .height(34.dp)
                                ) {
                                    Text(
                                        text = "Take",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFDCFCE7),
                                    modifier = Modifier
                                        .widthIn(min = 72.dp)
                                        .height(30.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Taken",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534),
                                            textAlign = TextAlign.Center,
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

            HorizontalDivider(color = MedBorderTeal.copy(alpha = 0.6f))

            // Health Diary Status for Today
            if (todayEntry != null) {
                // Today's entry is logged
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MedTealSoft.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (todayEntry.mood) {
                                    "Great" -> Color(0xFFDCFCE7)
                                    "Good" -> Color(0xFFE0F2FE)
                                    "Neutral" -> Color(0xFFF1F5F9)
                                    "Low" -> Color(0xFFFEF3C7)
                                    else -> Color(0xFFFEE2E2)
                                }
                            ) {
                                Text(
                                    text = when (todayEntry.mood) {
                                        "Great" -> "Score 5"
                                        "Good" -> "Score 4"
                                        "Neutral" -> "Score 3"
                                        "Low" -> "Score 2"
                                        else -> "Score 1"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (todayEntry.mood) {
                                        "Great" -> Color(0xFF166534)
                                        "Good" -> Color(0xFF0369A1)
                                        "Neutral" -> Color(0xFF334155)
                                        "Low" -> Color(0xFF92400E)
                                        else -> Color(0xFF991B1B)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "Today's Logged Health: ${todayEntry.mood}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary
                            )
                        }
                        TextButton(
                            onClick = onOpenLogModal,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MedTealPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedTealPrimary)
                        }
                    }

                    // Symptoms & Notes snippet
                    if (todayEntry.symptoms.isNotEmpty()) {
                        Text(
                            text = "Symptoms: ${todayEntry.symptoms}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MedTextPrimary
                        )
                    }

                    if (todayEntry.notes.isNotEmpty()) {
                        Text(
                            text = "Notes: \"${todayEntry.notes}\"",
                            fontSize = 12.sp,
                            color = MedTextSecondary
                        )
                    }

                    // Mini vitals chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color.White) {
                            Text(
                                text = "BP: ${todayEntry.systolicBp}/${todayEntry.diastolicBp}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = Color.White) {
                            Text(
                                text = "Pain: ${todayEntry.painLevel}/10",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (todayEntry.painLevel <= 2) MedSuccess else MedWarning,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = Color.White) {
                            Text(
                                text = "Sugar: ${todayEntry.bloodSugar} mg/dL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            } else {
                // Today's entry not logged yet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "No health log recorded today",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Track how you feel alongside your medications",
                            fontSize = 11.sp,
                            color = Color(0xFFB45309)
                        )
                    }

                    Button(
                        onClick = onOpenLogModal,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Log Diary", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyAdherenceCalendarCard(
    onOpenFullCalendar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(22.dp)),
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
                Text(
                    text = "Weekly Adherence & Health",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedTextPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MedSuccess))
                    Text("Adherence Complete", fontSize = 11.sp, color = MedTextSecondary)
                }
            }

            // 7-day calendar representation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                val dates = listOf("8", "9", "10", "11", "12", "13", "14")
                days.forEachIndexed { idx, day ->
                    val isToday = idx == 6
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isToday) MedTealSoft else Color.Transparent)
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                    ) {
                        Text(day, fontSize = 11.sp, color = MedTextSecondary, fontWeight = FontWeight.Bold)
                        Text(
                            dates[idx],
                            fontSize = 13.sp,
                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isToday) MedTealPrimary else MedTextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (idx < 5) MedSuccess else if (idx == 5) MedWarning else MedTealPrimary)
                        )
                    }
                }
            }

            HorizontalDivider(color = MedBorderTeal.copy(alpha = 0.5f), thickness = 1.dp)

            Button(
                onClick = onOpenFullCalendar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("open_dose_calendar_from_strip_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB87355)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Medication Dose Calendar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MedicationDoseCalendarSectionCard(
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF7)),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFDCD2C4))
        )
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Medication Dose Calendar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2C2522)
                    )
                    Text(
                        text = "Visualizes upcoming & past doses with Taken / Missed statuses",
                        fontSize = 12.sp,
                        color = Color(0xFF6B5D56)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF4EDE2),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFDCD2C4))
                    )
                ) {
                    Text(
                        text = "Interactive",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB87355),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Status Legend Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFECFCCB),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Taken [T]", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4D7C0F))
                        Text("Completed", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4D7C0F))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Missed [M]", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB91C1C))
                        Text("Skipped/Late", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFFB91C1C))
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFCCFBF1),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Upcoming [U]", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D9488))
                        Text("Scheduled", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0D9488))
                    }
                }
            }

            Button(
                onClick = onOpenCalendar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("open_dose_calendar_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB87355))
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open Medication Dose Calendar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VitalsTrendChartCard(
    entries: List<HealthDiaryEntity>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
        )
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
                        text = "Vitals Trend Over Time",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTextPrimary
                    )
                    Text(
                        text = "Systolic BP (Teal) & Blood Glucose (Blue)",
                        fontSize = 12.sp,
                        color = MedTextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MedTealSoft
                ) {
                    Text(
                        text = "Last 7 Days",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTealDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Canvas Chart
            val dataPointsBp = remember(entries) {
                if (entries.isEmpty()) listOf(120, 118, 122, 119, 121, 118, 120)
                else entries.take(7).reversed().map { it.systolicBp }
            }
            val dataPointsSugar = remember(entries) {
                if (entries.isEmpty()) listOf(95, 98, 92, 94, 96, 95, 93)
                else entries.take(7).reversed().map { it.bloodSugar }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MedTealBg.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Draw baseline grid lines
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, height * 0.25f),
                        end = Offset(width, height * 0.25f),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(0f, height * 0.75f),
                        end = Offset(width, height * 0.75f),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw Systolic BP line (Teal)
                    if (dataPointsBp.size > 1) {
                        val pathBp = Path()
                        val stepX = width / (dataPointsBp.size - 1)
                        dataPointsBp.forEachIndexed { i, value ->
                            val normY = 1f - ((value - 110f) / 25f).coerceIn(0.1f, 0.9f)
                            val y = normY * height
                            val x = i * stepX
                            if (i == 0) pathBp.moveTo(x, y) else pathBp.lineTo(x, y)
                            drawCircle(color = Color(0xFF0D9488), radius = 3.5.dp.toPx(), center = Offset(x, y))
                        }
                        drawPath(path = pathBp, color = Color(0xFF0D9488), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                    }

                    // Draw Blood Sugar line (Blue)
                    if (dataPointsSugar.size > 1) {
                        val pathSugar = Path()
                        val stepX = width / (dataPointsSugar.size - 1)
                        dataPointsSugar.forEachIndexed { i, value ->
                            val normY = 1f - ((value - 80f) / 35f).coerceIn(0.1f, 0.9f)
                            val y = normY * height
                            val x = i * stepX
                            if (i == 0) pathSugar.moveTo(x, y) else pathSugar.lineTo(x, y)
                            drawCircle(color = Color(0xFF0284C7), radius = 3.5.dp.toPx(), center = Offset(x, y))
                        }
                        drawPath(path = pathSugar, color = Color(0xFF0284C7), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0D9488)))
                    Text("BP: ~120 mmHg", fontSize = 11.sp, color = MedTextSecondary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0284C7)))
                    Text("Glucose: ~95 mg/dL", fontSize = 11.sp, color = MedTextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Enhanced DiaryEntryCard displaying Mood, Pain score, Vitals, Symptoms, Notes, and Delete action.
 */
@Composable
fun DiaryEntryCard(
    entry: HealthDiaryEntity,
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Mood + Date + Pain Badge + Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (entry.mood) {
                            "Great" -> Color(0xFFDCFCE7)
                            "Good" -> Color(0xFFE0F2FE)
                            "Neutral" -> Color(0xFFF1F5F9)
                            "Low" -> Color(0xFFFEF3C7)
                            else -> Color(0xFFFEE2E2)
                        }
                    ) {
                        Text(
                            text = when (entry.mood) {
                                "Great" -> "5/5"
                                "Good" -> "4/5"
                                "Neutral" -> "3/5"
                                "Low" -> "2/5"
                                else -> "1/5"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when (entry.mood) {
                                "Great" -> Color(0xFF166534)
                                "Good" -> Color(0xFF0369A1)
                                "Neutral" -> Color(0xFF334155)
                                "Low" -> Color(0xFF92400E)
                                else -> Color(0xFF991B1B)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Column {
                        Text(
                            text = entry.mood,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                        Text(
                            text = entry.dateString,
                            fontSize = 12.sp,
                            color = MedTextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val painColor = if (entry.painLevel <= 2) Color(0xFFDCFCE7) else if (entry.painLevel <= 5) Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
                    val painTextColor = if (entry.painLevel <= 2) Color(0xFF166534) else if (entry.painLevel <= 5) Color(0xFF92400E) else Color(0xFF991B1B)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = painColor
                    ) {
                        Text(
                            text = "Pain ${entry.painLevel}/10",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = painTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete entry",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Vitals chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MedTealBg,
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Blood Pressure", fontSize = 10.sp, color = MedTextSecondary, fontWeight = FontWeight.Medium)
                        Text("${entry.systolicBp}/${entry.diastolicBp} mmHg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedTextPrimary)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MedTealBg,
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Blood Glucose", fontSize = 10.sp, color = MedTextSecondary, fontWeight = FontWeight.Medium)
                        Text("${entry.bloodSugar} mg/dL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedTextPrimary)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MedTealBg,
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Weight", fontSize = 10.sp, color = MedTextSecondary, fontWeight = FontWeight.Medium)
                        Text("${entry.weightKg} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedTextPrimary)
                    }
                }
            }

            // Symptoms Section
            if (entry.symptoms.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Healing,
                            contentDescription = null,
                            tint = MedTealPrimary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Column {
                            Text(
                                text = "Reported Symptoms & Effects:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary
                            )
                            Text(
                                text = entry.symptoms,
                                fontSize = 12.sp,
                                color = MedTextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Personal Notes Section
            if (entry.notes.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MedTealSoft.copy(alpha = 0.4f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MedTealPrimary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Column {
                            Text(
                                text = "Personal Reflections & Notes:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTealDark
                            )
                            Text(
                                text = entry.notes,
                                fontSize = 12.sp,
                                color = MedTextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dropdown selector card for viewing All Days Data or individual days in the Diary section.
 */
@Composable
fun DiaryDaysDropdownCard(
    entries: List<HealthDiaryEntity>,
    selectedDayOption: String,
    onSelectDayOption: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("diary_day_dropdown_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(MedLightLines)
        )
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MedLightAccentTint,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MedLightAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Diary Days Selector",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedLightTextPrimary
                        )
                        Text(
                            text = if (selectedDayOption == "ALL_DAYS") "Displaying All Days Data (${entries.size} days)" else "Viewing: $selectedDayOption",
                            fontSize = 11.sp,
                            color = MedLightTextSecondary
                        )
                    }
                }

                // Interactive Drop Down Button
                Box {
                    Button(
                        onClick = { expanded = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MedLightAccent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("diary_dropdown_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedDayOption == "ALL_DAYS") "All Days" else "$selectedDayOption",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .widthIn(min = 260.dp, max = 340.dp)
                            .testTag("diary_days_dropdown_menu")
                    ) {
                        // "All Days" Item (Displays All Days Data)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "All Days (All Data)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MedLightTextPrimary
                                        )
                                        Text(
                                            text = "Display complete timeline & metrics",
                                            fontSize = 10.sp,
                                            color = MedLightTextSecondary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MedLightAccentTint
                                    ) {
                                        Text(
                                            text = "${entries.size} Days",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MedLightAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onSelectDayOption("ALL_DAYS")
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AllInclusive,
                                    contentDescription = null,
                                    tint = MedLightAccent
                                )
                            }
                        )

                        HorizontalDivider(color = MedLightLines.copy(alpha = 0.7f))

                        // Individual Days
                        entries.forEach { entry ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = entry.dateString,
                                                fontWeight = if (selectedDayOption == entry.dateString) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp,
                                                color = MedLightTextPrimary
                                            )
                                            Text(
                                                text = "BP ${entry.systolicBp}/${entry.diastolicBp} • Sugar ${entry.bloodSugar}",
                                                fontSize = 10.sp,
                                                color = MedLightTextSecondary
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = when (entry.mood) {
                                                    "Great", "Good" -> Color(0xFFDCFCE7)
                                                    "Neutral" -> Color(0xFFFEF3C7)
                                                    else -> Color(0xFFFBE6E3)
                                                }
                                            ) {
                                                Text(
                                                    text = entry.mood,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (entry.mood) {
                                                        "Great", "Good" -> Color(0xFF166534)
                                                        "Neutral" -> Color(0xFF92400E)
                                                        else -> Color(0xFFB3261E)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (selectedDayOption == entry.dateString) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MedLightAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectDayOption(entry.dateString)
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = null,
                                        tint = if (selectedDayOption == entry.dateString) MedLightAccent else MedLightTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // If ALL_DAYS is selected, display All Days Comprehensive Data Summary
            if (selectedDayOption == "ALL_DAYS" && entries.isNotEmpty()) {
                AllDaysDataOverview(entries = entries)
            } else if (selectedDayOption != "ALL_DAYS") {
                val matched = entries.firstOrNull { it.dateString == selectedDayOption }
                if (matched != null) {
                    SingleDaySummaryView(
                        entry = matched,
                        onShowAllDays = { onSelectDayOption("ALL_DAYS") }
                    )
                }
            }
        }
    }
}

/**
 * Overview component displaying aggregated clinical vitals & averages across all recorded days.
 */
@Composable
fun AllDaysDataOverview(entries: List<HealthDiaryEntity>) {
    val count = entries.size
    val avgSys = if (count > 0) entries.map { it.systolicBp }.average().toInt() else 120
    val avgDia = if (count > 0) entries.map { it.diastolicBp }.average().toInt() else 80
    val avgSugar = if (count > 0) entries.map { it.bloodSugar }.average().toInt() else 95
    val avgWeight = if (count > 0) String.format(Locale.getDefault(), "%.1f", entries.map { it.weightKg }.average()) else "70.0"
    val avgPain = if (count > 0) String.format(Locale.getDefault(), "%.1f", entries.map { it.painLevel }.average()) else "0.0"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "All Days Aggregate Health Metrics",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MedLightTextPrimary
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MedLightAccentTint
            ) {
                Text(
                    text = "$count Days Logged",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MedLightAccent,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Metrics 4-cell Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AllDaysMetricChip(
                label = "Avg Blood Pressure",
                value = "$avgSys/$avgDia",
                unit = "mmHg",
                color = MedLightAccent,
                modifier = Modifier.weight(1f)
            )
            AllDaysMetricChip(
                label = "Avg Blood Sugar",
                value = "$avgSugar",
                unit = "mg/dL",
                color = Color(0xFF0284C7),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AllDaysMetricChip(
                label = "Avg Weight",
                value = avgWeight,
                unit = "kg",
                color = Color(0xFF059669),
                modifier = Modifier.weight(1f)
            )
            AllDaysMetricChip(
                label = "Avg Pain Index",
                value = avgPain,
                unit = "/ 10",
                color = Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Focused highlight banner when a specific single day is selected from the dropdown.
 */
@Composable
fun SingleDaySummaryView(
    entry: HealthDiaryEntity,
    onShowAllDays: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MedLightAccentTint.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
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
                Column {
                    Text(
                        text = "Viewing: ${entry.dateString}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MedLightTextPrimary
                    )
                    Text(
                        text = "Mood: ${entry.mood} • Pain Level: ${entry.painLevel}/10",
                        fontSize = 11.sp,
                        color = MedLightTextSecondary
                    )
                }
                TextButton(
                    onClick = onShowAllDays,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Show All Days", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MedLightAccent)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color.White, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BP", fontSize = 10.sp, color = MedLightTextSecondary)
                        Text("${entry.systolicBp}/${entry.diastolicBp}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedLightTextPrimary)
                    }
                }
                Surface(shape = RoundedCornerShape(8.dp), color = Color.White, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sugar", fontSize = 10.sp, color = MedLightTextSecondary)
                        Text("${entry.bloodSugar} mg/dL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedLightTextPrimary)
                    }
                }
                Surface(shape = RoundedCornerShape(8.dp), color = Color.White, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Weight", fontSize = 10.sp, color = MedLightTextSecondary)
                        Text("${entry.weightKg} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MedLightTextPrimary)
                    }
                }
            }
        }
    }
}

/**
 * Metric chip for All Days overview.
 */
@Composable
fun AllDaysMetricChip(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedLightLines)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = MedLightTextSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
                Spacer(modifier = Modifier.width(3.dp))
                Text(unit, fontSize = 10.sp, color = MedLightTextSecondary, modifier = Modifier.padding(bottom = 1.dp))
            }
        }
    }
}
