package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HealthDiaryEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.TodayDoseItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogHealthEntryDialog(
    todayEntry: HealthDiaryEntity?,
    todayDoses: List<TodayDoseItem> = emptyList(),
    onDismiss: () -> Unit,
    onSaveEntry: (
        mood: String,
        pain: Int,
        sys: Int,
        dia: Int,
        sugar: Int,
        weight: Float,
        symptoms: String,
        notes: String
    ) -> Unit
) {
    var moodSelected by remember(todayEntry) { mutableStateOf(todayEntry?.mood ?: "Great") }
    var painSliderValue by remember(todayEntry) { mutableFloatStateOf(todayEntry?.painLevel?.toFloat() ?: 0f) }
    var sysBpText by remember(todayEntry) { mutableStateOf(todayEntry?.systolicBp?.takeIf { it > 0 }?.toString() ?: "") }
    var diaBpText by remember(todayEntry) { mutableStateOf(todayEntry?.diastolicBp?.takeIf { it > 0 }?.toString() ?: "") }
    var bloodSugarText by remember(todayEntry) { mutableStateOf(todayEntry?.bloodSugar?.takeIf { it > 0 }?.toString() ?: "") }
    var weightText by remember(todayEntry) { mutableStateOf(todayEntry?.weightKg?.takeIf { it > 0f }?.toString() ?: "") }
    var symptomsText by remember(todayEntry) { mutableStateOf(todayEntry?.symptoms ?: "") }
    var diaryNotesText by remember(todayEntry) { mutableStateOf(todayEntry?.notes ?: "") }

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
        onDismissRequest = onDismiss,
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

                // Section 1: Mood Selection
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "How are you feeling overall?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(
                                Pair("Great", "😄"),
                                Pair("Good", "🙂"),
                                Pair("Okay", "😐"),
                                Pair("Unwell", "🤒"),
                                Pair("Pain", "😣")
                            ).forEach { (label, emoji) ->
                                val isSelected = moodSelected.equals(label, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MedTealPrimary else MedTealSoft.copy(alpha = 0.5f),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        width = 1.dp,
                                        brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) MedTealPrimary else MedBorderTeal)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp)
                                        .clickable { moodSelected = label }
                                        .testTag("mood_option_${label.lowercase()}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = emoji, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MedTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Pain Level Slider (0 to 10)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pain Severity",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTextPrimary
                            )
                            Text(
                                text = "${painSliderValue.toInt()} / 10",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (painSliderValue > 6) Color(0xFFDC2626) else if (painSliderValue > 3) MedWarning else MedSuccess
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
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("diary_pain_slider")
                        )
                    }
                }

                // Section 3: Quick Symptoms Tag Chips
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Symptoms Experienced",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickSymptoms.forEach { symptom ->
                                val currentList = symptomsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                val isSelected = currentList.any { it.equals(symptom, ignoreCase = true) }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) MedTealPrimary else MedSurface,
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        width = 1.dp,
                                        brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) MedTealPrimary else MedBorderTeal)
                                    ),
                                    modifier = Modifier.clickable {
                                        symptomsText = if (isSelected) {
                                            currentList.filterNot { it.equals(symptom, ignoreCase = true) }.joinToString(", ")
                                        } else {
                                            (currentList + symptom).joinToString(", ")
                                        }
                                    }
                                ) {
                                    Text(
                                        text = symptom,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MedTextPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 4: Daily Health Notes / Doctor Observations
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Diary Notes & Observations",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                        OutlinedTextField(
                            value = diaryNotesText,
                            onValueChange = { diaryNotesText = it },
                            placeholder = { Text("e.g. Took antibiotics with yoghurt. Mild nausea after lunch, improved after resting.") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 84.dp)
                                .testTag("diary_notes_field"),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 4
                        )
                    }
                }

                // Section 5: Biometrics & Vitals Tracking
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Biometrics & Vitals (Optional)",
                            fontSize = 14.sp,
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
                    onSaveEntry(
                        moodSelected,
                        painSliderValue.toInt(),
                        sys,
                        dia,
                        sugar,
                        weight,
                        symptomsText.trim(),
                        diaryNotesText.trim()
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Health Entry", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MedTextSecondary)
            }
        }
    )
}
