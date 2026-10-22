package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.medicine.MedicineSearchResult
import com.example.data.medicine.MedicineSearchState
import com.example.data.medicine.MedicineSource
import com.example.ui.theme.*

/**
 * Normal Medicine-Name Input Field with a direct searchable dropdown list of
 * matching medicines from real online databases (NIH RxNorm & Indian Medicine Directory).
 *
 * Requirements met:
 * - Starts searching after >= 2 characters
 * - Debounced typing
 * - Max 8 results with scrolling
 * - Shows brand, generic, strength, dosage form
 * - Tapping any result selects it and fills the form
 * - Does not auto-select without user tap
 * - Highlights currently selected medicine
 * - Dismisses dropdown upon selection
 * - Loading indicator
 * - Clear no-results state
 * - Graceful error handling with retry
 * - Unblocked manual entry anytime
 */
@Composable
fun SearchableMedicineDropdown(
    queryText: String,
    onQueryChange: (String) -> Unit,
    searchState: MedicineSearchState,
    onSelectMedicine: (MedicineSearchResult) -> Unit,
    onRetrySearch: () -> Unit,
    selectedMedicineId: String?,
    modifier: Modifier = Modifier
) {
    var isDropdownDismissed by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    // Reopen dropdown when query changes
    LaunchedEffect(queryText) {
        isDropdownDismissed = false
    }

    val shouldShowDropdown = queryText.trim().length >= 2 && !isDropdownDismissed

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Normal Medicine Name Input Field
        OutlinedTextField(
            value = queryText,
            onValueChange = { newText ->
                onQueryChange(newText)
                isDropdownDismissed = false
            },
            label = { Text("Medicine Name *") },
            placeholder = { Text("Type name, e.g. Paracetamol, Dolo, Amoxicillin") },
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    if (searchState is MedicineSearchState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MedTealPrimary
                        )
                    }
                    if (queryText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onQueryChange("")
                                isDropdownDismissed = true
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear medicine name",
                                tint = MedTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("medicine_name_input_field"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MedTealPrimary,
                unfocusedBorderColor = MedBorderTeal,
                focusedLabelColor = MedTealPrimary
            )
        )

        // 2. Verified Database vs Manual Entry Status Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedMedicineId != null && selectedMedicineId.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Verified Database Entry",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF065F46)
                        )
                    }
                }
            } else if (queryText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF3F4F6),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Custom / Manual Entry",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4B5563)
                        )
                    }
                }
            } else {
                Text(
                    text = "Type at least 2 letters to search online database",
                    fontSize = 11.sp,
                    color = MedTextSecondary
                )
            }

            if (shouldShowDropdown) {
                Text(
                    text = "Tap to choose",
                    fontSize = 11.sp,
                    color = MedTealPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 3. Dropdown Menu directly below input field
        AnimatedVisibility(
            visible = shouldShowDropdown,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MedTealPrimary.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    when (searchState) {
                        is MedicineSearchState.Loading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MedTealPrimary
                                )
                                Text(
                                    text = "Searching online medicine database...",
                                    fontSize = 13.sp,
                                    color = MedTextSecondary
                                )
                            }
                        }

                        is MedicineSearchState.Success -> {
                            val results = searchState.results.take(8)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MedTealBg)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MATCHING MEDICINES (${results.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedTealPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Online DB • Max 8",
                                    fontSize = 10.sp,
                                    color = MedTextSecondary
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                results.forEachIndexed { index, medicine ->
                                    val isSelected = selectedMedicineId == medicine.id

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSelectMedicine(medicine)
                                                isDropdownDismissed = true
                                            }
                                            .testTag("medicine_dropdown_item_$index"),
                                        color = if (isSelected) MedTealSoft.copy(alpha = 0.6f) else Color.White
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Icon indicator
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) MedTealPrimary else MedTealSoft),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = when (medicine.dosageForm.lowercase()) {
                                                        "liquid", "suspension", "solution", "syrup" -> Icons.Default.WaterDrop
                                                        "injection" -> Icons.Default.Vaccines
                                                        "inhaler" -> Icons.Default.Air
                                                        else -> Icons.Default.Medication
                                                    },
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color.White else MedTealPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Text Details
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = medicine.brandName,
                                                        fontSize = 14.sp,
                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                        color = if (isSelected) MedTealPrimary else MedTextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (medicine.isIndianBrand) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFFFEF3C7)
                                                        ) {
                                                            Text(
                                                                text = "IN",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFB45309),
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Generic, strength, and dosage form
                                                val details = buildString {
                                                    if (medicine.genericName.isNotBlank() && !medicine.genericName.equals(medicine.brandName, ignoreCase = true)) {
                                                        append(medicine.genericName)
                                                        append(" • ")
                                                    }
                                                    if (medicine.strength.isNotBlank()) {
                                                        append(medicine.strength)
                                                        append(" ")
                                                    }
                                                    append(medicine.dosageForm)
                                                }
                                                Text(
                                                    text = details,
                                                    fontSize = 12.sp,
                                                    color = MedTextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Highlight indicator
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = MedTealPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.NorthWest,
                                                    contentDescription = "Select this medicine",
                                                    tint = MedTextSecondary.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (index < results.lastIndex) {
                                        HorizontalDivider(
                                            color = MedBorderTeal.copy(alpha = 0.3f),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }

                            // Dismiss button footer
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isDropdownDismissed = true }
                                    .background(Color(0xFFF9FAFB))
                                    .padding(vertical = 6.dp),
                                color = Color.Transparent
                            ) {
                                Text(
                                    text = "Close Dropdown",
                                    fontSize = 11.sp,
                                    color = MedTextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }

                        is MedicineSearchState.Empty -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SearchOff,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "No matches for \"${searchState.query}\"",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedTextPrimary
                                    )
                                }
                                Text(
                                    text = "You can still add this medicine manually. Just fill out the strength and schedule below.",
                                    fontSize = 12.sp,
                                    color = MedTextSecondary,
                                    lineHeight = 16.sp
                                )
                                TextButton(
                                    onClick = { isDropdownDismissed = true },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Keep \"${searchState.query}\" & Continue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        is MedicineSearchState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudOff,
                                        contentDescription = null,
                                        tint = MedDanger,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Online database unavailable",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MedDanger
                                    )
                                }
                                Text(
                                    text = "Internet connection issue. You can retry or enter your medicine manually without waiting.",
                                    fontSize = 12.sp,
                                    color = MedTextSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { isDropdownDismissed = true }) {
                                        Text("Continue Offline", fontSize = 12.sp, color = MedTextSecondary)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = onRetrySearch,
                                        colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        is MedicineSearchState.Idle -> {
                            // Inactive
                        }
                    }
                }
            }
        }
    }
}
