package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FamilyMember
import com.example.ui.theme.*

/**
 * Dialog facilitating the patient-side verification workflow.
 * Allows simulating patient approval or manually entering the 6-digit confirmation code.
 */
@Composable
fun FamilyMemberVerificationDialog(
    member: FamilyMember,
    onVerifyCode: (code: String) -> Unit,
    onSimulateApproval: () -> Unit,
    onDismiss: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
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
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Patient Device Verification",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTextPrimary
                    )
                    Text(
                        text = "HIPAA Security Check • Local Supabase",
                        fontSize = 11.sp,
                        color = MedTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("family_verification_dialog"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status banner: Waiting for verification
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
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Waiting for ${member.name}'s Confirmation",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "SMS dispatch sent to ${member.patientPhone.ifBlank { member.emergencyContact }} (${member.relationship})",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Security Explanation & Lockout Notice
                Text(
                    text = "For security and privacy, you cannot schedule or modify medicines for ${member.name} until they confirm from their phone. Once verified, full care management is unlocked.",
                    fontSize = 12.sp,
                    color = MedTextSecondary,
                    lineHeight = 16.sp
                )

                // Notice that code was dispatched to patient's device (not revealed in caregiver/user app)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MedTealBg,
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(MedBorderTeal)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = MedTealPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "VERIFICATION DISPATCHED TO PATIENT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MedTealDark,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Security code sent to ${member.name}'s phone. Ask the patient for the 6-digit code displayed on their device.",
                                fontSize = 11.sp,
                                color = MedTextPrimary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Verification Input Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                codeInput = it
                                errorMessage = null
                            }
                        },
                        label = { Text("Enter 6-digit Patient Code") },
                        placeholder = { Text("Enter 6-digit code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("patient_verification_code_input"),
                        isError = errorMessage != null
                    )

                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = MedDanger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Simulate Patient Approval Button (Clean test mechanism)
                Button(
                    onClick = {
                        isVerifying = true
                        onSimulateApproval()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_patient_approval_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Simulate Patient Approval (One-Tap Test)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (codeInput.trim().length == 6) {
                        isVerifying = true
                        onVerifyCode(codeInput.trim())
                    } else {
                        errorMessage = "Please enter the complete 6-digit verification code."
                    }
                },
                enabled = !isVerifying,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                modifier = Modifier.testTag("submit_patient_verification_btn")
            ) {
                Text("Confirm Code", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = MedTextSecondary)
            }
        }
    )
}
