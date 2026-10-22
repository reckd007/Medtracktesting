package com.example.ui.auth

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Requirement 6: Forgot-password flow.
 * Allows entering registered email, triggers password recovery against backend,
 * displays cooldown and user-friendly confirmation instructions.
 */
@Composable
fun ForgotPasswordDialog(
    initialEmail: String = "",
    onDismiss: () -> Unit,
    onSubmitRecovery: suspend (String) -> Result<String>
) {
    var email by remember { mutableStateOf(initialEmail) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var cooldownSeconds by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000L)
            cooldownSeconds -= 1
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MedTealSoft,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = MedTealPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Reset Password",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MedLightTextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Enter your registered Medtrack email address. We will send you instructions to safely reset your password.",
                    fontSize = 13.5.sp,
                    color = MedLightTextSecondary,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (successMessage != null) {
                    // Success Confirmation State
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFECFDF5),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MedtrackMint,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Recovery Link Sent",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF065F46)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = successMessage ?: "",
                                fontSize = 13.sp,
                                color = Color(0xFF047857),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MedTealPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Return to Login", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Form State
                    AuthTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                            generalError = null
                        },
                        label = "Email Address",
                        placeholder = "you@example.com",
                        leadingIcon = Icons.Default.Email,
                        errorMessage = emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        testTag = "forgot_password_email_input"
                    )

                    if (generalError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = generalError ?: "",
                            fontSize = 12.5.sp,
                            color = Color(0xFFDC2626),
                            lineHeight = 17.sp,
                            modifier = Modifier.testTag("forgot_password_error_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    AuthPrimaryButton(
                        text = if (cooldownSeconds > 0) "Wait ${cooldownSeconds}s" else "Send Recovery Instructions",
                        enabled = cooldownSeconds == 0,
                        isLoading = isLoading,
                        onClick = {
                            val trimmedEmail = email.trim()
                            if (trimmedEmail.isBlank()) {
                                emailError = "Please enter your email address."
                                return@AuthPrimaryButton
                            }
                            if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                                emailError = "Please enter a valid email format."
                                return@AuthPrimaryButton
                            }

                            coroutineScope.launch {
                                isLoading = true
                                generalError = null
                                try {
                                    val result = onSubmitRecovery(trimmedEmail)
                                    if (result.isSuccess) {
                                        successMessage = result.getOrNull() ?: "Instructions have been sent to your email."
                                        cooldownSeconds = 60
                                    } else {
                                        generalError = result.exceptionOrNull()?.message ?: "Failed to send reset instructions."
                                    }
                                } catch (e: Exception) {
                                    generalError = e.message ?: "An unexpected error occurred."
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        testTag = "forgot_password_submit_button"
                    )
                }
            }
        }
    }
}
