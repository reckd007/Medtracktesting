package com.example.ui.auth

import android.app.DatePickerDialog
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.auth.RegistrationDraft
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Requirement 4: Multi-Step Registration and Onboarding Flow.
 * Steps:
 * 1. Profile Name (First, Last name)
 * 2. Gender / Identity (Female, Male, Non-binary, Other [with field], Prefer not to say)
 * 3. Date of Birth (DatePicker, real date check, age 18+ policy enforcement)
 * 4. Medication Preferences (Multi-select options)
 * 5. Account Credentials (Email, Password with checklist)
 * 6. Email Verification (Destination email, 6-digit code, resend countdown, return option)
 */
@Composable
fun RegistrationFlowScreen(
    initialDraft: RegistrationDraft = RegistrationDraft(),
    onCancelRegistration: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onRegistrationCompleted: (AuthSession) -> Unit,
    onPerformRegistration: suspend (RegistrationDraft) -> AuthResult,
    onPerformVerifyCode: suspend (email: String, code: String, draft: RegistrationDraft) -> AuthResult,
    onPerformResendCode: suspend (email: String) -> Result<String>,
    onContinueOffline: (RegistrationDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var draft by remember { mutableStateOf(initialDraft) }

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Error states per step
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var isNetworkError by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableIntStateOf(0) }
    var resendFeedback by remember { mutableStateOf<String?>(null) }

    // Resend countdown timer
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000L)
            resendCooldown -= 1
        }
    }

    // Handle back button throughout the 6-step flow
    BackHandler {
        if (currentStep > 1) {
            generalError = null
            currentStep -= 1
        } else {
            onCancelRegistration()
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
            when (currentStep) {
                // STEP 1: Profile Name
                1 -> {
                    AuthHeader(
                        title = "Let's get to know you better! What's your name?",
                        subtitle = "We use your name to personalize your medication schedule and family care circle.",
                        currentStep = 1,
                        totalSteps = 6,
                        onBack = onCancelRegistration
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        AuthTextField(
                            value = draft.firstName,
                            onValueChange = {
                                draft = draft.copy(firstName = it)
                                firstNameError = null
                            },
                            label = "First Name *",
                            placeholder = "Alex",
                            leadingIcon = Icons.Default.Person,
                            errorMessage = firstNameError,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            testTag = "reg_first_name_input"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        AuthTextField(
                            value = draft.lastName,
                            onValueChange = {
                                draft = draft.copy(lastName = it)
                                lastNameError = null
                            },
                            label = "Last Name *",
                            placeholder = "Henderson",
                            leadingIcon = Icons.Default.Badge,
                            errorMessage = lastNameError,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            testTag = "reg_last_name_input"
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        AuthPrimaryButton(
                            text = "Next",
                            onClick = {
                                focusManager.clearFocus()
                                var valid = true
                                if (draft.firstName.trim().isBlank()) {
                                    firstNameError = "First name is required."
                                    valid = false
                                }
                                if (draft.lastName.trim().isBlank()) {
                                    lastNameError = "Last name is required."
                                    valid = false
                                }
                                if (valid) {
                                    currentStep = 2
                                }
                            },
                            leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            testTag = "reg_step1_next_button"
                        )
                    }
                }

                // STEP 2: Gender or Identity
                2 -> {
                    AuthHeader(
                        title = "How do you identify?",
                        subtitle = "This helps ensure dosage benchmarks and physiological health insights are accurate. You can also select 'Prefer not to say'.",
                        currentStep = 2,
                        totalSteps = 6,
                        onBack = { currentStep = 1 }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        val genderOptions = listOf(
                            "Female",
                            "Male",
                            "Non-binary",
                            "Other (please specify)",
                            "Prefer not to say"
                        )

                        genderOptions.forEach { option ->
                            val isSelected = draft.gender == option
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MedTealSoft else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    if (isSelected) MedTealPrimary else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clickable {
                                        draft = draft.copy(gender = option)
                                    }
                                    .testTag("gender_option_${option.replace(" ", "_").lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { draft = draft.copy(gender = option) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MedTealPrimary,
                                            unselectedColor = Color(0xFF94A3B8)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = option,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MedTealPrimary else MedLightTextPrimary
                                    )
                                }
                            }
                        }

                        // Conditional custom specification for "Other (please specify)"
                        AnimatedVisibility(
                            visible = draft.gender == "Other (please specify)",
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                AuthTextField(
                                    value = draft.otherGenderText,
                                    onValueChange = { draft = draft.copy(otherGenderText = it) },
                                    label = "Please specify (optional)",
                                    placeholder = "Enter how you identify",
                                    testTag = "gender_other_specify_input"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        AuthPrimaryButton(
                            text = "Next",
                            onClick = {
                                if (draft.gender.isBlank()) {
                                    draft = draft.copy(gender = "Prefer not to say")
                                }
                                currentStep = 3
                            },
                            leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            testTag = "reg_step2_next_button"
                        )
                    }
                }

                // STEP 3: Date of Birth
                3 -> {
                    AuthHeader(
                        title = "What is your date of birth?",
                        subtitle = "Used for pediatric and geriatric dosage warnings and to verify minimum account age requirements.",
                        currentStep = 3,
                        totalSteps = 6,
                        onBack = { currentStep = 2 }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        val calendar = Calendar.getInstance()
                        val currentYear = calendar.get(Calendar.YEAR)
                        val currentMonth = calendar.get(Calendar.MONTH)
                        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

                        // Date picker trigger card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (dobError != null) Color(0xFFDC2626) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val datePicker = DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedCal = Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth, 0, 0, 0)
                                            }
                                            val nowCal = Calendar.getInstance()

                                            if (selectedCal.after(nowCal)) {
                                                dobError = "Date of birth cannot be in the future."
                                                return@DatePickerDialog
                                            }

                                            // Calculate age
                                            var age = nowCal.get(Calendar.YEAR) - year
                                            if (nowCal.get(Calendar.DAY_OF_YEAR) < selectedCal.get(Calendar.DAY_OF_YEAR)) {
                                                age--
                                            }

                                            val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.US)
                                            val formatted = sdf.format(selectedCal.time)

                                            if (age < 18) {
                                                dobError = "Medtrack primary caregiver accounts require you to be at least 18 years old. (Current age: $age). Parents or guardians can register to manage dependent prescriptions."
                                                draft = draft.copy(
                                                    birthDateMillis = selectedCal.timeInMillis,
                                                    birthDateFormatted = formatted,
                                                    calculatedAge = age,
                                                    isUnderageError = true
                                                )
                                            } else {
                                                dobError = null
                                                draft = draft.copy(
                                                    birthDateMillis = selectedCal.timeInMillis,
                                                    birthDateFormatted = formatted,
                                                    calculatedAge = age,
                                                    isUnderageError = false
                                                )
                                            }
                                        },
                                        currentYear - 28,
                                        currentMonth,
                                        currentDay
                                    )
                                    datePicker.datePicker.maxDate = System.currentTimeMillis()
                                    datePicker.show()
                                }
                                .testTag("reg_dob_picker_trigger")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MedTealSoft,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Date of Birth",
                                            tint = MedTealPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Selected Date of Birth",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (draft.birthDateFormatted.isNotBlank()) draft.birthDateFormatted else "Tap to choose your birth date",
                                        fontSize = 15.sp,
                                        fontWeight = if (draft.birthDateFormatted.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                        color = if (draft.birthDateFormatted.isNotBlank()) MedLightTextPrimary else Color(0xFF94A3B8)
                                    )
                                }

                                if (draft.calculatedAge > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (draft.calculatedAge >= 18) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                                    ) {
                                        Text(
                                            text = "Age ${draft.calculatedAge}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (draft.calculatedAge >= 18) Color(0xFF047857) else Color(0xFFB91C1C),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (dobError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dobError ?: "",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF991B1B),
                                        lineHeight = 17.sp,
                                        modifier = Modifier.testTag("dob_error_text")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        AuthPrimaryButton(
                            text = "Next",
                            enabled = draft.birthDateFormatted.isNotBlank() && !draft.isUnderageError,
                            onClick = {
                                if (draft.birthDateFormatted.isBlank()) {
                                    dobError = "Please select your date of birth."
                                    return@AuthPrimaryButton
                                }
                                if (draft.isUnderageError) {
                                    return@AuthPrimaryButton
                                }
                                currentStep = 4
                            },
                            leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            testTag = "reg_step3_next_button"
                        )
                    }
                }

                // STEP 4: Medication App Preferences
                4 -> {
                    AuthHeader(
                        title = "How would you like to use the app?",
                        subtitle = "You can choose more than one.",
                        currentStep = 4,
                        totalSteps = 6,
                        onBack = { currentStep = 3 }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        val preferenceItems = listOf(
                            Pair(RegistrationDraft.PREF_GET_REMINDERS, Icons.Default.Alarm),
                            Pair(RegistrationDraft.PREF_TRACK_TAKEN, Icons.Default.CheckCircleOutline),
                            Pair(RegistrationDraft.PREF_KEEP_LIST, Icons.Default.Inventory2),
                            Pair(RegistrationDraft.PREF_REFILL_REMINDERS, Icons.Default.Autorenew),
                            Pair(RegistrationDraft.PREF_CHECK_INTERACTIONS, Icons.Default.AutoAwesome),
                            Pair(RegistrationDraft.PREF_TRACK_SIDE_EFFECTS, Icons.Default.WarningAmber),
                            Pair(RegistrationDraft.PREF_TRACK_MEASUREMENTS, Icons.Default.FavoriteBorder)
                        )

                        preferenceItems.forEach { (prefText, icon) ->
                            val isChecked = draft.selectedPreferences.contains(prefText)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isChecked) MedTealSoft else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    if (isChecked) MedTealPrimary else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clickable {
                                        val updated = draft.selectedPreferences.toMutableSet()
                                        if (isChecked) {
                                            updated.remove(prefText)
                                        } else {
                                            updated.add(prefText)
                                        }
                                        draft = draft.copy(selectedPreferences = updated)
                                    }
                                    .testTag("pref_option_${prefText.replace(" ", "_").lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isChecked) MedTealPrimary else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = prefText,
                                        fontSize = 14.5.sp,
                                        fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isChecked) MedTealPrimary else MedLightTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            val updated = draft.selectedPreferences.toMutableSet()
                                            if (checked) updated.add(prefText) else updated.remove(prefText)
                                            draft = draft.copy(selectedPreferences = updated)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MedTealPrimary,
                                            checkmarkColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        AuthPrimaryButton(
                            text = "Continue",
                            onClick = { currentStep = 5 },
                            leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            testTag = "reg_step4_next_button"
                        )
                    }
                }

                // STEP 5: Account Credentials
                5 -> {
                    AuthHeader(
                        title = "Create your Medtrack account",
                        subtitle = "Enter your email and create a password to securely safeguard your medication records.",
                        currentStep = 5,
                        totalSteps = 6,
                        onBack = { currentStep = 4 }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        AuthTextField(
                            value = draft.email,
                            onValueChange = {
                                draft = draft.copy(email = it)
                                emailError = null
                                generalError = null
                            },
                            label = "Email Address *",
                            placeholder = "alex.henderson@example.com",
                            leadingIcon = Icons.Default.Email,
                            errorMessage = emailError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            testTag = "reg_email_input"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        AuthTextField(
                            value = draft.password,
                            onValueChange = {
                                draft = draft.copy(password = it)
                                passwordError = null
                                generalError = null
                            },
                            label = "Password *",
                            placeholder = "Create a secure password",
                            leadingIcon = Icons.Default.Lock,
                            isPassword = true,
                            errorMessage = passwordError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            testTag = "reg_password_input"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Requirements Checklist
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Password Requirements",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val hasMinLength = draft.password.length >= 6
                                val hasLetter = draft.password.any { it.isLetter() }
                                val hasDigit = draft.password.any { it.isDigit() }

                                PasswordRequirementRow(
                                    text = "At least 6 characters",
                                    isMet = hasMinLength
                                )
                                PasswordRequirementRow(
                                    text = "Contains at least 1 letter",
                                    isMet = hasLetter
                                )
                                PasswordRequirementRow(
                                    text = "Contains at least 1 number",
                                    isMet = hasDigit
                                )
                            }
                        }

                        if (generalError != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = generalError ?: "",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF991B1B),
                                        lineHeight = 17.sp,
                                        modifier = Modifier.testTag("reg_step5_error_text")
                                    )
                                    if (isNetworkError) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { onContinueOffline(draft) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Continue Setup in Offline Mode", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        AuthPrimaryButton(
                            text = "Create Account",
                            isLoading = isLoading,
                            onClick = {
                                focusManager.clearFocus()
                                val trimmedEmail = draft.email.trim()
                                var hasErr = false

                                if (trimmedEmail.isBlank()) {
                                    emailError = "Email address is required."
                                    hasErr = true
                                } else if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                                    emailError = "Please enter a valid email format."
                                    hasErr = true
                                }

                                if (draft.password.length < 6) {
                                    passwordError = "Password must be at least 6 characters."
                                    hasErr = true
                                } else if (!draft.password.any { it.isLetter() }) {
                                    passwordError = "Password must contain at least one letter."
                                    hasErr = true
                                } else if (!draft.password.any { it.isDigit() }) {
                                    passwordError = "Password must contain at least one number."
                                    hasErr = true
                                }

                                if (hasErr) return@AuthPrimaryButton

                                coroutineScope.launch {
                                    isLoading = true
                                    generalError = null
                                    isNetworkError = false
                                    try {
                                        val result = onPerformRegistration(draft)
                                        when (result) {
                                            is AuthResult.Success -> {
                                                onRegistrationCompleted(result.session)
                                            }
                                            is AuthResult.EmailVerificationRequired -> {
                                                resendCooldown = 60
                                                currentStep = 6
                                            }
                                            is AuthResult.Error -> {
                                                generalError = result.message
                                                isNetworkError = result.isNetworkError
                                            }
                                        }
                                    } catch (e: Exception) {
                                        generalError = e.message ?: "Failed to create account."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            testTag = "reg_create_account_button"
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Already have an account? ",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "Log In",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MedTealPrimary,
                                modifier = Modifier
                                    .clickable { onNavigateToLogin() }
                                    .padding(vertical = 4.dp)
                                    .testTag("reg_to_login_link")
                            )
                        }
                    }
                }

                // STEP 6: Email Verification
                6 -> {
                    AuthHeader(
                        title = "Verify your email address",
                        subtitle = "We have sent a verification confirmation to:",
                        currentStep = 6,
                        totalSteps = 6,
                        onBack = { currentStep = 5 }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        // Email display badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MedTealSoft,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MedTealPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailRead,
                                    contentDescription = null,
                                    tint = MedTealPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = draft.email,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MedTealPrimary,
                                    modifier = Modifier.testTag("verify_destination_email")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Enter the 6-digit confirmation code sent to your email to verify your account (For local testing, enter 123456):",
                            fontSize = 13.5.sp,
                            color = MedLightTextSecondary,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        AuthTextField(
                            value = draft.verificationCode,
                            onValueChange = {
                                if (it.length <= 8) {
                                    draft = draft.copy(verificationCode = it)
                                    codeError = null
                                    generalError = null
                                }
                            },
                            label = "6-Digit Verification Code",
                            placeholder = "123456",
                            leadingIcon = Icons.Default.Key,
                            errorMessage = codeError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            testTag = "reg_verify_code_input"
                        )

                        if (generalError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = generalError ?: "",
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF991B1B),
                                    lineHeight = 17.sp,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .testTag("verify_error_text")
                                )
                            }
                        }

                        if (resendFeedback != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = resendFeedback ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Verify button
                        AuthPrimaryButton(
                            text = "Verify & Complete Setup",
                            isLoading = isLoading,
                            onClick = {
                                focusManager.clearFocus()
                                val code = draft.verificationCode.trim()
                                if (code.isBlank()) {
                                    codeError = "Please enter the verification code."
                                    return@AuthPrimaryButton
                                }
                                coroutineScope.launch {
                                    isLoading = true
                                    generalError = null
                                    try {
                                        val result = onPerformVerifyCode(draft.email, code, draft)
                                        when (result) {
                                            is AuthResult.Success -> {
                                                onRegistrationCompleted(result.session)
                                            }
                                            is AuthResult.EmailVerificationRequired -> {
                                                generalError = result.message
                                            }
                                            is AuthResult.Error -> {
                                                generalError = result.message
                                            }
                                        }
                                    } catch (e: Exception) {
                                        generalError = e.message ?: "Verification failed."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            testTag = "reg_submit_verify_button"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Resend Button with Cooldown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { currentStep = 5 },
                                modifier = Modifier.testTag("verify_back_to_credentials")
                            ) {
                                Text("Change email", color = Color(0xFF64748B), fontSize = 13.sp)
                            }

                            TextButton(
                                onClick = {
                                    if (resendCooldown == 0) {
                                        coroutineScope.launch {
                                            val res = onPerformResendCode(draft.email)
                                            if (res.isSuccess) {
                                                resendFeedback = res.getOrNull()
                                                resendCooldown = 60
                                            } else {
                                                generalError = res.exceptionOrNull()?.message
                                            }
                                        }
                                    }
                                },
                                enabled = resendCooldown == 0,
                                modifier = Modifier.testTag("verify_resend_code_button")
                            ) {
                                Text(
                                    text = if (resendCooldown > 0) "Resend in ${resendCooldown}s" else "Resend Code",
                                    color = if (resendCooldown > 0) Color(0xFF94A3B8) else MedTealPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
