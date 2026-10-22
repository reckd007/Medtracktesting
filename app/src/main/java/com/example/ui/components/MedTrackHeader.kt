package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MedTrackCapsuleLogo(
    modifier: Modifier = Modifier,
    height: Int = 36
) {
    // Capsule pill logo: surface half with "Medtrack" in mint green, mint green half with plus
    Row(
        modifier = modifier
            .height(height.dp)
            .shadow(2.dp, RoundedCornerShape(percent = 50))
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(percent = 50)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo text half
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Medtrack",
                color = MedicoMint,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp
            )
        }
        // Mint green half
        Box(
            modifier = Modifier
                .background(MedicoMint)
                .fillMaxHeight()
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Medical Plus",
                tint = Color(0xFF0B1B3A),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun MedTrackHeader(
    currentProfile: String,
    onProfileSelected: (String) -> Unit,
    familyMembers: List<com.example.data.model.FamilyMember> = emptyList(),
    onAddNewMemberClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }
    val todayFormatted = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Logo in header
            MedTrackCapsuleLogo(
                modifier = Modifier.testTag("medtrack_header_logo")
            )

            // Family / Profile Switcher
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .testTag("profile_switcher_button")
                        .clickable { profileMenuExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MedTealSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User profile",
                                tint = MedTealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = currentProfile,
                            color = MedTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Switch profile",
                            tint = MedTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = profileMenuExpanded,
                    onDismissRequest = { profileMenuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    if (familyMembers.isNotEmpty()) {
                        familyMembers.forEach { member ->
                            val isSelected = currentProfile == member.id || currentProfile == member.name || (member.isPrimary && currentProfile == "Self")
                            val displayName = if (member.isPrimary) "Self (${member.name})" else "${member.name} (${member.relationship})"
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = displayName,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                            if (member.isPending) {
                                                Text(
                                                    text = "Pending Device Check",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFD97706),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    val selectTarget = if (member.isPrimary) "Self" else member.id
                                    onProfileSelected(selectTarget)
                                    profileMenuExpanded = false
                                }
                            )
                        }
                    } else {
                        listOf("Self", "Dad", "Maya (Daughter)").forEach { profile ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (profile == currentProfile) Icons.Filled.CheckCircle else Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = if (profile == currentProfile) MedTealPrimary else MedTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = profile,
                                            color = if (profile == currentProfile) MedTealPrimary else MedTextPrimary,
                                            fontWeight = if (profile == currentProfile) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                onClick = {
                                    onProfileSelected(profile)
                                    profileMenuExpanded = false
                                }
                            )
                        }
                    }

                    if (onAddNewMemberClick != null) {
                        HorizontalDivider(color = MedBorderTeal.copy(alpha = 0.5f))
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = MedTealPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "+ Add Family Dependent",
                                        color = MedTealPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            onClick = {
                                profileMenuExpanded = false
                                onAddNewMemberClick()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    fontSize = 13.sp,
                    color = MedTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (currentProfile == "Self") "Today's Schedule" else "$currentProfile's Regimen",
                    fontSize = 22.sp,
                    color = MedTextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            }
            Text(
                text = todayFormatted,
                fontSize = 12.sp,
                color = MedTextSecondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(MedTealSoft, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
