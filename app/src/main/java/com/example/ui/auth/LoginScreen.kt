package com.example.ui.auth

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthResult
import com.example.data.auth.AuthSession
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Requirement 3: Dedicated Login Screen.
 * Includes email validation, password toggle, forgot password recovery,
 * loading state, backend authentication, and network failure resilience.
 */
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: (AuthSession) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToVerify: (email: String, userId: String) -> Unit,
    onPerformLogin: suspend (email: String, password: String) -> AuthResult,
    onPerformPasswordRecovery: suspend (email: String) -> Result<String>,
    onContinueOffline: (email: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var isNetworkError by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    fun submitLogin() {
        focusManager.clearFocus()
        emailError = null
        passwordError = null
        generalError = null
        isNetworkError = false

        val trimmedEmail = email.trim()
        var hasError = false

        if (trimmedEmail.isEmpty()) {
            emailError = "Email address is required."
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            emailError = "Please enter a valid email format."
            hasError = true
        }

        if (password.isEmpty()) {
            passwordError = "Password is required."
            hasError = true
        } else if (password.length < 6) {
            passwordError = "Password must be at least 6 characters."
            hasError = true
        }

        if (hasError) return

        coroutineScope.launch {
            isLoading = true
            try {
                when (val result = onPerformLogin(trimmedEmail, password)) {
                    is AuthResult.Success -> {
                        onLoginSuccess(result.session)
                    }
                    is AuthResult.EmailVerificationRequired -> {
                        onNavigateToVerify(result.email, result.userId)
                    }
                    is AuthResult.Error -> {
                        generalError = result.message
                        isNetworkError = result.isNetworkError
                    }
                }
            } catch (e: Exception) {
                generalError = e.message ?: "An unexpected error occurred during login."
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        containerColor = MedTealBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            AuthHeader(
                title = "Welcome back",
                subtitle = "Log in to your Medtrack account to view your medication schedules, diary, and reminders.",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Email input
                AuthTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                        generalError = null
                    },
                    label = "Email Address",
                    placeholder = "alex.henderson@example.com",
                    leadingIcon = Icons.Default.Email,
                    errorMessage = emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    testTag = "login_email_input"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password input
                AuthTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                        generalError = null
                    },
                    label = "Password",
                    placeholder = "Enter your password",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    errorMessage = passwordError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitLogin() }
                    ),
                    testTag = "login_password_input"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Forgot Password Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot password?",
                        color = MedTealPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { showForgotPasswordDialog = true }
                            .padding(vertical = 4.dp)
                            .testTag("login_forgot_password_link")
                    )
                }

                // General Error Banner
                if (generalError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = if (isNetworkError) Icons.Default.WifiOff else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isNetworkError) "Connection Notice" else "Login Error",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = generalError ?: "",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFFB91C1C),
                                        lineHeight = 17.sp,
                                        modifier = Modifier.testTag("login_error_message")
                                    )
                                }
                            }

                            if (isNetworkError) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { onContinueOffline(email) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Continue in Local On-Device Mode",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Login Button
                AuthPrimaryButton(
                    text = "Log In",
                    onClick = { submitLogin() },
                    isLoading = isLoading,
                    testTag = "login_submit_button"
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Footer link to register
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account? ",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Get Started",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MedTealPrimary,
                        modifier = Modifier
                            .clickable { onNavigateToRegister() }
                            .padding(vertical = 4.dp)
                            .testTag("login_to_register_link")
                    )
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            onDismiss = { showForgotPasswordDialog = false },
            onSubmitRecovery = onPerformPasswordRecovery
        )
    }
}
