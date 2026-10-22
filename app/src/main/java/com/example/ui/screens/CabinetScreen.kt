package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MedicationEntity
import com.example.ui.components.MedTrackCapsuleLogo
import com.example.ui.theme.*

@Composable
fun CabinetScreen(
    medications: List<MedicationEntity>,
    onRefillMedication: (Long, Int) -> Unit,
    onDeleteMedication: (MedicationEntity) -> Unit,
    onAddMedicationClick: () -> Unit,
    onEditMedication: (MedicationEntity) -> Unit = {},
    onRunRefillService: () -> Unit = {},
    currentProfile: String = "Self",
    isProfilePending: Boolean = false,
    onVerifyProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Low Stock", "Chronic"
    var refillDialogMed by remember { mutableStateOf<MedicationEntity?>(null) }
    var refillAmountText by remember { mutableStateOf("") }
    var orderNotificationMsg by remember { mutableStateOf<String?>(null) }
    var refillServiceFeedback by remember { mutableStateOf(false) }

    val lowStockCount = remember(medications) {
        medications.count { it.stockRemaining <= it.refillThreshold }
    }

    val filteredMeds = remember(medications, selectedFilter) {
        when (selectedFilter) {
            "Low Stock" -> medications.filter { it.stockRemaining <= it.refillThreshold }
            "Temporary" -> medications.filter { it.courseDurationDays > 0 }
            else -> medications
        }
    }

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
                icon = {
                    Icon(
                        imageVector = if (isProfilePending) Icons.Default.Lock else Icons.Default.Add,
                        contentDescription = "Add medicine"
                    )
                },
                text = {
                    Text(
                        text = if (isProfilePending) "Verification Required" else "Add to Cabinet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .testTag("cabinet_add_medication_fab")
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
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MedTealPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${medications.size} Total Items",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Medicine Cabinet",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MedTextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Real-time pill inventory and automated prescription refill alerts",
                        fontSize = 13.sp,
                        color = MedTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
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
                            .testTag("cabinet_access_lockout_banner")
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
                                        text = "Waiting for verification from $currentProfile's device. Adding or modifying medications for this profile is locked.",
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
                                    .testTag("cabinet_verify_device_btn")
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verify Device (Enter 6-digit Code)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Low Stock Warning Banner if any
            if (lowStockCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .testTag("low_stock_banner"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = lowStockSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = androidx.compose.ui.graphics.SolidColor(lowStockBorder)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(lowStockIconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = "Warning",
                                    tint = lowStockIcon,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$lowStockCount Medication${if (lowStockCount > 1) "s" else ""} Running Low",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = lowStockText
                                )
                                Text(
                                    text = "Stock is below your safety threshold. Refill now to avoid missing scheduled doses.",
                                    fontSize = 12.sp,
                                    color = lowStockSecondaryText,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        onRunRefillService()
                                        refillServiceFeedback = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("cabinet_trigger_refill_service_btn")
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Run Refill Background Check", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Filter Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Low Stock", "Temporary").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = if (filter == "Low Stock" && lowStockCount > 0) "Low Stock ($lowStockCount)" else filter,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
                            modifier = Modifier.testTag("cabinet_filter_$filter")
                        )
                    }
                }
            }

            // List of Inventory Items
            if (filteredMeds.isEmpty()) {
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
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MedSuccess,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "All supplies in healthy standing",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MedTextPrimary
                            )
                            Text(
                                text = "No medications match the current filter.",
                                fontSize = 13.sp,
                                color = MedTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(filteredMeds, key = { it.id }) { med ->
                    CabinetMedicationCard(
                        medication = med,
                        onEditClick = { onEditMedication(med) },
                        onRefillClick = {
                            refillDialogMed = med
                            refillAmountText = ""
                        },
                        onOrderMore = {
                            orderNotificationMsg = "Pharmacy order link initiated for ${med.name} (${med.dosage})"
                        },
                        onDelete = { onDeleteMedication(med) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // Modal Dialog: Refill Medication
    val targetRefillMed = refillDialogMed
    if (targetRefillMed != null) {
        val targetMed = targetRefillMed
        AlertDialog(
            onDismissRequest = { refillDialogMed = null },
            title = {
                Text(
                    text = "Refill ${targetMed.name}",
                    fontWeight = FontWeight.Bold,
                    color = MedTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Current Stock: ${targetMed.stockRemaining} units left",
                        fontSize = 14.sp,
                        color = MedTextSecondary
                    )
                    val refillUnitsLabel = when (targetMed.form.lowercase(java.util.Locale.getDefault())) {
                        "liquid", "solution", "suspension", "syrup" -> "Volume to add (mL)"
                        "inhaler" -> "Puffs / Canisters to add"
                        "injection" -> "Vials / Pens to add"
                        "capsule" -> "Capsules to add"
                        "tablet" -> "Tablets to add"
                        else -> "Units to add"
                    }
                    OutlinedTextField(
                        value = refillAmountText,
                        onValueChange = { refillAmountText = it.filter { char -> char.isDigit() } },
                        label = { Text(refillUnitsLabel) },
                        placeholder = { Text("Quantity to add (e.g. 30)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        listOf(14, 30, 60, 90).forEach { amount ->
                            FilledTonalButton(
                                onClick = { refillAmountText = amount.toString() },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("+$amount", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = refillAmountText.toIntOrNull() ?: 30
                        onRefillMedication(targetMed.id, amount)
                        refillDialogMed = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary)
                ) {
                    Text("Add to Stock", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { refillDialogMed = null }) {
                    Text("Cancel", color = MedTextSecondary)
                }
            }
        )
    }

    if (refillServiceFeedback) {
        AlertDialog(
            onDismissRequest = { refillServiceFeedback = false },
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
                    text = "Refill Check Background Service",
                    fontWeight = FontWeight.Bold,
                    color = MedTextPrimary,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "RefillCheckBackgroundService launched. It is querying the Room database for any medications at or below your custom refill alert thresholds and dispatching push notifications.",
                    fontSize = 14.sp,
                    color = MedTextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { refillServiceFeedback = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun CabinetMedicationCard(
    medication: MedicationEntity,
    onEditClick: () -> Unit = {},
    onRefillClick: () -> Unit,
    onOrderMore: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLowStock = medication.stockRemaining <= medication.refillThreshold
    var cardMenuExpanded by remember { mutableStateOf(false) }

    // Estimate days remaining assuming frequencyPerDay
    val dailyUsage = if (medication.frequencyPerDay > 0) medication.frequencyPerDay else 1
    val daysLeft = medication.stockRemaining / dailyUsage

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEditClick() }
            .shadow(2.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isLowStock) lowStockBorder else MedBorderTeal
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Name, Form, and Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(medication.colorHex)).copy(alpha = 0.15f)
                                } catch (_: Exception) {
                                    MedTealSoft
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (medication.form.lowercase()) {
                                "injection" -> Icons.Default.Vaccines
                                "liquid" -> Icons.Default.WaterDrop
                                else -> Icons.Default.Medication
                            },
                            contentDescription = medication.form,
                            tint = try {
                                Color(android.graphics.Color.parseColor(medication.colorHex))
                            } catch (_: Exception) {
                                MedTealPrimary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = medication.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedTextPrimary
                        )
                        Text(
                            text = "${medication.dosage} • ${medication.form} • ${medication.timingSlots}",
                            fontSize = 13.sp,
                            color = MedTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { cardMenuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Cabinet actions",
                            tint = MedTextSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = cardMenuExpanded,
                        onDismissRequest = { cardMenuExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Medication") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = MedTealPrimary) },
                            onClick = {
                                cardMenuExpanded = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Log Refill") },
                            leadingIcon = { Icon(Icons.Outlined.AddCircleOutline, contentDescription = null) },
                            onClick = {
                                cardMenuExpanded = false
                                onRefillClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Order More / Pharmacy") },
                            leadingIcon = { Icon(Icons.Outlined.LocalPharmacy, contentDescription = null) },
                            onClick = {
                                cardMenuExpanded = false
                                onOrderMore()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove from Cabinet", color = MedDanger) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MedDanger) },
                            onClick = {
                                cardMenuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Pill Quantity & Visual Gauge
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${medication.stockRemaining} units left",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLowStock) lowStockText else MedTextPrimary
                        )
                        Text(
                            text = "• ~$daysLeft days left",
                            fontSize = 13.sp,
                            color = MedTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (isLowStock) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = lowStockBadgeBg,
                            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(lowStockBorder))
                        ) {
                            Text(
                                text = "LOW STOCK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = lowStockBadgeText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Visual Stock Gauge Progress Bar
                val maxGauge = (medication.refillThreshold * 3).coerceAtLeast(30)
                val progressFraction = (medication.stockRemaining.toFloat() / maxGauge).coerceIn(0f, 1f)

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isLowStock) lowStockAccent else MedTealPrimary,
                    trackColor = if (isLowStock) lowStockTrack else MedTealSoft
                )
            }

            // Quick Actions: "Refill Reminder" & "Order More"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRefillClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("refill_button_${medication.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MedTealPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Refill", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                }

                Button(
                    onClick = onOrderMore,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("order_more_button_${medication.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MedTealPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalPharmacy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Order More", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
