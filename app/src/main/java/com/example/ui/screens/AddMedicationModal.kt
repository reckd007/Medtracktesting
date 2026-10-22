package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.api.RxNormDrugProduct
import com.example.data.medicine.MedicineSearchResult
import com.example.data.medicine.MedicineSearchState
import com.example.data.model.MedicationEntity
import com.example.ui.components.SearchableMedicineDropdown
import com.example.ui.scanner.CameraTextRecognitionScannerView
import com.example.ui.scanner.ExtractedMedicineDetails
import com.example.ui.theme.*
import java.util.Calendar
import java.util.Locale
import java.util.UUID

enum class ScannerMode {
    NONE,
    OCR_CAMERA
}

/**
 * Representation of a scheduled intake dose time for the medication.
 */
data class DoseTimeSlot(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val time24: String // "HH:mm" 24-hour format
)

private fun formatTimeTo12Hour(time24: String): String {
    val parts = time24.split(":")
    if (parts.size < 2) return time24
    val hour = parts[0].toIntOrNull() ?: return time24
    val minute = parts[1].toIntOrNull() ?: return time24
    val amPm = if (hour >= 12) "PM" else "AM"
    val h12 = when (val h = hour % 12) {
        0 -> 12
        else -> h
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, amPm)
}

private fun formatTime24Hour(hour: Int, minute: Int): String {
    return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddMedicationModal(
    currentProfile: String,
    medicationToEdit: MedicationEntity? = null,
    medicineSearchQuery: String = "",
    medicineSearchState: MedicineSearchState = MedicineSearchState.Idle,
    onSearchQueryChange: (String) -> Unit = {},
    onRetrySearch: () -> Unit = {},
    rxNormSuggestions: List<String> = emptyList(),
    rxNormProducts: List<RxNormDrugProduct> = emptyList(),
    isLoadingRxNorm: Boolean = false,
    onQueryRxNorm: (String) -> Unit = {},
    onSelectRxNormSuggestion: (String) -> Unit = {},
    onMedicineOcrCaptured: (ExtractedMedicineDetails) -> Unit = {},
    onQuickCommitOcrMedicine: (ExtractedMedicineDetails) -> Unit = {},
    onDismiss: () -> Unit,
    onSaveMedication: (MedicationEntity) -> Unit,
    isProfilePending: Boolean = false,
    onVerifyProfileClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEditMode = medicationToEdit != null

    // Form States (pre-populated when editing)
    var medName by remember(medicationToEdit) { mutableStateOf(medicationToEdit?.name ?: "") }
    var dosage by remember(medicationToEdit) { mutableStateOf(medicationToEdit?.dosage ?: "") }
    var formType by remember(medicationToEdit) { mutableStateOf(medicationToEdit?.form ?: "Tablet / Capsule") }
    var colorHex by remember(medicationToEdit) { mutableStateOf(medicationToEdit?.colorHex ?: "#0D9488") }
    var rxcuiSelected by remember(medicationToEdit) { mutableStateOf(medicationToEdit?.rxcui ?: "") }

    // Pre-populate dose times safely
    val initialDoseTimes: List<DoseTimeSlot> = remember(medicationToEdit) {
        if (medicationToEdit != null) {
            val times = (medicationToEdit.timesOfDay ?: "08:00")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val slots = (medicationToEdit.timingSlots ?: "Morning")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (times.isNotEmpty()) {
                times.mapIndexed { index, timeStr ->
                    val defaultLabel = when (timeStr) {
                        "08:00" -> "Morning"
                        "13:00" -> "Afternoon"
                        "19:00" -> "Evening"
                        "22:00" -> "Bedtime"
                        else -> formatTimeTo12Hour(timeStr)
                    }
                    val label = slots.getOrNull(index)?.takeIf { it.isNotBlank() } ?: defaultLabel
                    DoseTimeSlot(label = label, time24 = timeStr)
                }
            } else {
                listOf(DoseTimeSlot(label = "Morning", time24 = "08:00"))
            }
        } else {
            listOf(DoseTimeSlot(label = "Morning", time24 = "08:00"))
        }
    }
    var doseTimes by remember(medicationToEdit) { mutableStateOf(initialDoseTimes) }

    // Days of the week selection (defaults to all 7 days)
    val allDaysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDays by remember(medicationToEdit) {
        val daysFromMed = medicationToEdit?.daysOfWeek
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()

        mutableStateOf(
            if (!daysFromMed.isNullOrEmpty()) daysFromMed else allDaysList.toSet()
        )
    }

    var courseDurationDays by remember(medicationToEdit) { mutableIntStateOf(medicationToEdit?.courseDurationDays ?: 0) } // 0 = Ongoing

    // Inventory & Refill
    var stockRemainingText by remember(medicationToEdit) {
        mutableStateOf(medicationToEdit?.stockRemaining?.toString() ?: "")
    }
    var refillThresholdText by remember(medicationToEdit) {
        mutableStateOf(medicationToEdit?.refillThreshold?.toString() ?: "")
    }
    var instructions by remember(medicationToEdit) { mutableStateOf(medicationToEdit?.instructions ?: "") }

    // Scanner state
    var currentScannerMode by remember { mutableStateOf(ScannerMode.NONE) }
    var scannerNoticeMessage by remember { mutableStateOf<String?>(null) }

    // Helper: launches Android interactive clock dialog
    fun launchInteractiveClock(
        initialTime24: String = "08:00",
        onTimeChosen: (hour: Int, minute: Int) -> Unit
    ) {
        val parts = initialTime24.split(":")
        val startHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val startMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onTimeChosen(hourOfDay, minute)
            },
            startHour,
            startMinute,
            false // 12-hour AM/PM interactive clock
        ).show()
    }

    // If OCR scanner is active, display viewfinder dialog
    if (currentScannerMode == ScannerMode.OCR_CAMERA) {
        OcrScannerViewfinderDialog(
            onClose = { currentScannerMode = ScannerMode.NONE },
            onDetailsCaptured = { details ->
                medName = details.name
                dosage = details.dosage
                formType = details.formFactor
                scannerNoticeMessage = "Auto-Filled: ${details.name} (${details.dosage}) • ${details.formFactor}. Review & save below."
                onMedicineOcrCaptured(details)
                currentScannerMode = ScannerMode.NONE
            },
            onCommitDirectly = { details ->
                onQuickCommitOcrMedicine(details)
                currentScannerMode = ScannerMode.NONE
                onDismiss()
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, MedBorderTeal.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // STICKY TOP HEADER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEditMode) "Edit Medication" else "Add Medication",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MedTextPrimary
                            )
                            Text(
                                text = "Profile: $currentProfile",
                                fontSize = 12.sp,
                                color = MedTealPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MedTextSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = MedBorderTeal.copy(alpha = 0.4f), thickness = 1.dp)

                    // SCROLLABLE BODY
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Access Lockout Warning Banner if profile is pending verification
                        if (isProfilePending) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
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
                                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Verification Required",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                            Text(
                                                text = "You cannot add medicines for $currentProfile until their device is verified.",
                                                fontSize = 12.sp,
                                                color = Color(0xFFB45309),
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                    if (onVerifyProfileClick != null) {
                                        Button(
                                            onClick = {
                                                onDismiss()
                                                onVerifyProfileClick()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().height(40.dp)
                                        ) {
                                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Verify Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // OCR Scanner Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MedTealSoft.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, MedBorderTeal)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Scan Medicine Label",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedTextPrimary
                                    )
                                    Text(
                                        text = "Auto-extract details with camera OCR",
                                        fontSize = 11.sp,
                                        color = MedTextSecondary
                                    )
                                }
                                Button(
                                    onClick = { currentScannerMode = ScannerMode.OCR_CAMERA },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        scannerNoticeMessage?.let { notice ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MedicoTheme.colors.successBg,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MedicoTheme.colors.successText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = notice,
                                        fontSize = 12.sp,
                                        color = MedicoTheme.colors.successText,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // SECTION 1: MEDICINE BASICS
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "1. Medicine Details",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTealPrimary
                            )

                            SearchableMedicineDropdown(
                                queryText = medName,
                                onQueryChange = { newText ->
                                    medName = newText
                                    onSearchQueryChange(newText)
                                    onQueryRxNorm(newText)
                                },
                                searchState = medicineSearchState,
                                selectedMedicineId = rxcuiSelected,
                                onSelectMedicine = { selected ->
                                    medName = selected.brandName
                                    rxcuiSelected = selected.id
                                    if (selected.strength.isNotBlank()) {
                                        dosage = selected.strength
                                    }
                                    if (selected.dosageForm.isNotBlank()) {
                                        formType = selected.dosageForm
                                    }
                                },
                                onRetrySearch = onRetrySearch,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = dosage,
                                onValueChange = { dosage = it },
                                label = { Text("Strength / Dosage") },
                                placeholder = { Text("e.g. 500 mg, 10 ml") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Form Factor
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Form Factor:", fontSize = 12.sp, color = MedTextSecondary, fontWeight = FontWeight.Bold)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val baseForms = listOf("Tablet / Capsule", "Liquid", "Injection", "Topical", "Inhaler", "Other")
                                    val availableForms = if (baseForms.any { it.equals(formType, ignoreCase = true) }) baseForms else listOf(formType) + baseForms
                                    availableForms.forEach { form ->
                                        val isSel = formType.equals(form, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSel) MedTealPrimary else MedTealSoft.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, if (isSel) MedTealPrimary else MedBorderTeal),
                                            modifier = Modifier.clickable { formType = form }
                                        ) {
                                            Text(
                                                text = form,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else MedTextPrimary,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // SECTION 2: TIMING & CUSTOM TIME SELECTION (WITH INTERACTIVE CLOCK)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "2. Intake Times & Days",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTealPrimary
                            )

                            // Standard preset slots
                            Text(
                                text = "Quick Presets:",
                                fontSize = 12.sp,
                                color = MedTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val standardSlots = listOf(
                                    Pair("Morning", "08:00"),
                                    Pair("Afternoon", "13:00"),
                                    Pair("Evening", "19:00"),
                                    Pair("Bedtime", "22:00")
                                )
                                standardSlots.forEach { (slotLabel, slotTime) ->
                                    val isSelected = doseTimes.any { it.time24 == slotTime }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            doseTimes = if (isSelected) {
                                                if (doseTimes.size > 1) doseTimes.filterNot { it.time24 == slotTime } else doseTimes
                                            } else {
                                                doseTimes + DoseTimeSlot(label = slotLabel, time24 = slotTime)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = "$slotLabel (${formatTimeTo12Hour(slotTime)})",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MedTealPrimary,
                                            selectedLabelColor = Color.White,
                                            containerColor = MedTealSoft.copy(alpha = 0.35f),
                                            labelColor = MedTextPrimary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = MedBorderTeal,
                                            selectedBorderColor = MedTealPrimary
                                        )
                                    )
                                }
                            }

                            // Active Configured Times with interactive clock edit and custom time button
                            Text(
                                text = "Scheduled Dose Times (${doseTimes.size}):",
                                fontSize = 12.sp,
                                color = MedTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                doseTimes.forEachIndexed { index, slot ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MedTealSoft,
                                        border = BorderStroke(1.dp, MedTealPrimary.copy(alpha = 0.6f)),
                                        modifier = Modifier.clickable {
                                            // Open interactive clock to modify this slot's time
                                            launchInteractiveClock(initialTime24 = slot.time24) { hour, minute ->
                                                val new24 = formatTime24Hour(hour, minute)
                                                val new12 = formatTimeTo12Hour(new24)
                                                doseTimes = doseTimes.toMutableList().also { list ->
                                                    list[index] = slot.copy(
                                                        label = if (slot.label in listOf("Morning", "Afternoon", "Evening", "Bedtime")) slot.label else new12,
                                                        time24 = new24
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AccessTime,
                                                contentDescription = "Edit Time",
                                                tint = MedTealPrimary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "${slot.label} • ${formatTimeTo12Hour(slot.time24)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MedTealDark
                                            )
                                            if (doseTimes.size > 1) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = Color.Gray,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            doseTimes = doseTimes.filterIndexed { i, _ -> i != index }
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Interactive Clock Button to Add ANY Custom Time (e.g. 7:15 AM, 10:40 AM)
                                OutlinedButton(
                                    onClick = {
                                        launchInteractiveClock("09:00") { hour, minute ->
                                            val new24 = formatTime24Hour(hour, minute)
                                            val new12 = formatTimeTo12Hour(new24)
                                            // Check if not already added
                                            if (doseTimes.none { it.time24 == new24 }) {
                                                doseTimes = doseTimes + DoseTimeSlot(
                                                    label = "Custom ($new12)",
                                                    time24 = new24
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MedTealPrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AddAlarm,
                                        contentDescription = "Add Custom Time",
                                        tint = MedTealPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+ Custom Time",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedTealPrimary
                                    )
                                }
                            }

                            // Days of the week selection
                            Text(
                                text = "Intake Days of the Week:",
                                fontSize = 12.sp,
                                color = MedTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Preset shortcuts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedDays.size == 7,
                                    onClick = { selectedDays = allDaysList.toSet() },
                                    label = { Text("Everyday", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                FilterChip(
                                    selected = selectedDays == setOf("Mon", "Tue", "Wed", "Thu", "Fri"),
                                    onClick = { selectedDays = setOf("Mon", "Tue", "Wed", "Thu", "Fri") },
                                    label = { Text("Weekdays", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                FilterChip(
                                    selected = selectedDays == setOf("Sat", "Sun"),
                                    onClick = { selectedDays = setOf("Sat", "Sun") },
                                    label = { Text("Weekends", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            // Individual day toggles
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                allDaysList.forEach { day ->
                                    val isDaySelected = selectedDays.contains(day)
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDaySelected) MedTealPrimary else MedTealSoft.copy(alpha = 0.4f),
                                        border = BorderStroke(1.dp, if (isDaySelected) MedTealPrimary else MedBorderTeal),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clickable {
                                                selectedDays = if (isDaySelected) {
                                                    if (selectedDays.size > 1) selectedDays - day else selectedDays
                                                } else {
                                                    selectedDays + day
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = day.take(2),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDaySelected) Color.White else MedTextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // SECTION 3: COURSE DURATION
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "3. Course Duration",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTealPrimary
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Pair(0, "Ongoing / Chronic"),
                                    Pair(7, "7 Days"),
                                    Pair(10, "10 Days"),
                                    Pair(30, "30 Days")
                                ).forEach { (duration, label) ->
                                    val isSel = courseDurationDays == duration
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) MedTealPrimary else MedTealSoft.copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, if (isSel) MedTealPrimary else MedBorderTeal),
                                        modifier = Modifier.clickable { courseDurationDays = duration }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else MedTextPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // SECTION 4: INVENTORY & INSTRUCTIONS
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "4. Inventory & Instructions",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTealPrimary
                            )

                            val stockUnitsLabel = when (formType.lowercase(Locale.getDefault())) {
                                "liquid", "solution", "suspension", "syrup" -> "mL in Stock"
                                "inhaler" -> "Puffs in Stock"
                                "spray" -> "Sprays in Stock"
                                "injection" -> "Vials / Pens in Stock"
                                "capsule" -> "Capsules in Stock"
                                "tablet" -> "Tablets in Stock"
                                else -> "Units in Stock"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = stockRemainingText,
                                    onValueChange = { stockRemainingText = it.filter { c -> c.isDigit() } },
                                    label = { Text(stockUnitsLabel) },
                                    placeholder = { Text("e.g. 30") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    )
                                )

                                OutlinedTextField(
                                    value = refillThresholdText,
                                    onValueChange = { refillThresholdText = it.filter { c -> c.isDigit() } },
                                    label = { Text("Refill Alert") },
                                    placeholder = { Text("e.g. 5") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    )
                                )
                            }

                            OutlinedTextField(
                                value = instructions,
                                onValueChange = { instructions = it },
                                label = { Text("Instructions") },
                                placeholder = { Text("e.g. Take with a meal") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 3
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    HorizontalDivider(color = MedBorderTeal.copy(alpha = 0.4f), thickness = 1.dp)

                    // STICKY BOTTOM ACTION BAR (PINNED ABOVE SYSTEM NAVIGATION BAR & IME)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = MedTextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Button(
                                onClick = {
                                    if (isProfilePending) return@Button
                                    val finalName = if (medName.isNotBlank()) medName.trim() else "Amoxicillin"
                                    val finalDosage = if (dosage.isNotBlank()) dosage.trim() else "500 mg"
                                    val finalStock = stockRemainingText.toIntOrNull() ?: 30
                                    val finalThreshold = refillThresholdText.toIntOrNull() ?: 5
                                    val finalInstructions = if (instructions.isNotBlank()) instructions.trim() else "Take as directed"

                                    // Build persistent comma-separated times & labels
                                    val finalTimesOfDay = if (doseTimes.isNotEmpty()) {
                                        doseTimes.joinToString(",") { it.time24 }
                                    } else {
                                        "08:00"
                                    }
                                    val finalTimingSlots = if (doseTimes.isNotEmpty()) {
                                        doseTimes.joinToString(",") { it.label }
                                    } else {
                                        "Morning"
                                    }
                                    val finalDaysOfWeek = if (selectedDays.isNotEmpty()) {
                                        allDaysList.filter { selectedDays.contains(it) }.joinToString(",")
                                    } else {
                                        "Mon,Tue,Wed,Thu,Fri,Sat,Sun"
                                    }

                                    onSaveMedication(
                                        MedicationEntity(
                                            id = medicationToEdit?.id ?: 0L,
                                            name = finalName,
                                            dosage = finalDosage,
                                            form = formType,
                                            colorHex = colorHex,
                                            instructions = finalInstructions,
                                            timesOfDay = finalTimesOfDay,
                                            timingSlots = finalTimingSlots,
                                            frequencyPerDay = doseTimes.size.coerceAtLeast(1),
                                            frequencyDaysPerWeek = selectedDays.size.coerceAtLeast(1),
                                            courseDurationDays = courseDurationDays,
                                            stockRemaining = finalStock,
                                            refillThreshold = finalThreshold,
                                            profileName = medicationToEdit?.profileName ?: currentProfile,
                                            notes = medicationToEdit?.notes ?: "",
                                            rxcui = rxcuiSelected,
                                            daysOfWeek = finalDaysOfWeek
                                        )
                                    )
                                    onDismiss()
                                },
                                enabled = !isProfilePending,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isProfilePending) Color.LightGray else MedTealPrimary,
                                    disabledContainerColor = Color(0xFFE2E8F0)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("save_medication_button")
                            ) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isProfilePending) "Locked" else if (isEditMode) "Save Changes" else "Add Medication",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
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
fun OcrScannerViewfinderDialog(
    onClose: () -> Unit,
    onDetailsCaptured: (ExtractedMedicineDetails) -> Unit,
    onCommitDirectly: (ExtractedMedicineDetails) -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.fillMaxWidth(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = MedTealPrimary
                    )
                    Text(
                        text = "Medicine Label Scanner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MedTextPrimary
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MedTextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CameraTextRecognitionScannerView(
                    onMedicineDetailsExtracted = { details ->
                        onDetailsCaptured(details)
                    }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("Cancel / Close", color = MedTextSecondary)
            }
        }
    )
}
