package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.state.UserProfileManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isLoginMode by remember { mutableStateOf(true) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFAFF), Color(0xFFF3F0F5))
                )
            )
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3E8FF))
                        .border(1.dp, Color(0xFFD8B4FE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = Color(0xFF6750A4),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isLoginMode) "Welcome Back" else "Create Account",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    fontFamily = FontFamily.SansSerif
                )

                Text(
                    text = if (isLoginMode) "Sign in to access persistent splits & scans" else "Get started with BillBuddy split billing",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Input fields
                if (!isLoginMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("auth_name_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Star else Icons.Default.Star, // Password status icon fallback representation
                                contentDescription = "Toggle password visibility",
                                tint = Color(0xFF64748B)
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                )

                if (!isLoginMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password") },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("auth_confirm_password_input")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button
                Button(
                    onClick = {
                        val activeEmail = email.trim()
                        val activePwd = password.trim()
                        if (isLoginMode) {
                            if (activeEmail.isEmpty() || activePwd.isEmpty()) {
                                Toast.makeText(context, "Please enter email and password.", Toast.LENGTH_SHORT).show()
                            } else {
                                val success = UserProfileManager.login(context, activeEmail, activePwd)
                                if (success) {
                                    Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Invalid email or password.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            val activeName = name.trim()
                            val cPwd = confirmPassword.trim()
                            if (activeEmail.isEmpty() || activePwd.isEmpty() || activeName.isEmpty()) {
                                Toast.makeText(context, "Please complete all fields.", Toast.LENGTH_SHORT).show()
                            } else if (!activeEmail.contains("@")) {
                                Toast.makeText(context, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
                            } else if (activePwd.length < 4) {
                                Toast.makeText(context, "Password must be at least 4 characters.", Toast.LENGTH_SHORT).show()
                            } else if (activePwd != cPwd) {
                                Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                            } else {
                                val success = UserProfileManager.register(context, activeEmail, activeName, activePwd)
                                if (success) {
                                    Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "This email is already registered.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_primary_button")
                ) {
                    Text(
                        text = if (isLoginMode) "Sign In" else "Sign Up",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Link
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            isLoginMode = !isLoginMode
                            // Clear inputs
                            email = ""
                            password = ""
                            name = ""
                            confirmPassword = ""
                        }
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (isLoginMode) "New user? " else "Already registered? ",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = if (isLoginMode) "Sign Up" else "Sign In",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6750A4)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val registeredName by UserProfileManager.profileName.collectAsStateWithLifecycle()

    var nameState by remember(registeredName) { mutableStateOf(registeredName) }
    var upiState by remember { mutableStateOf("") }
    var phoneState by remember { mutableStateOf("") }
    var bioState by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFAF8FC), Color(0xFFF3F0F5))
                )
            )
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Celebration Icon Header
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9))
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success check",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to BillBuddy!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Text(
                    text = "Let's configure your profile profile detail settings. You can skip non-purpose biographical details easily.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Form details
                Text(
                    text = "Name Details *",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = nameState,
                    onValueChange = { nameState = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("onboard_name")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Preferred UPI Address (Optional)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = upiState,
                    onValueChange = { upiState = it },
                    label = { Text("e.g. jonge@okicici") },
                    placeholder = { Text("yourusername@upi") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("onboard_upi")
                )
                Text(
                    text = "Feeds directly into split payment QR generators!",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Mobile Number (Optional, Skipable)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = phoneState,
                    onValueChange = { phoneState = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("Contact Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("onboard_phone")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Short Bio (Optional, Skipable)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedTextField(
                    value = bioState,
                    onValueChange = { bioState = it },
                    label = { Text("Favorite splitting tagline") },
                    placeholder = { Text("Always split bills cleanly!") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("onboard_bio")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action controls: Register completely OR Skip optional fields entirely!
                Button(
                    onClick = {
                        val trimmedName = nameState.trim()
                        if (trimmedName.isEmpty()) {
                            Toast.makeText(context, "Full Name is required.", Toast.LENGTH_SHORT).show()
                        } else {
                            UserProfileManager.updateProfile(
                                context,
                                trimmedName,
                                phoneState.trim(),
                                bioState.trim(),
                                upiState.trim()
                            )
                            UserProfileManager.completeOnboarding(context)
                            Toast.makeText(context, "Profile setup finalized successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("onboard_save_btn")
                ) {
                    Text("Save & Quick Start", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        val trimmedName = if (nameState.trim().isEmpty()) "BillBuddy User" else nameState.trim()
                        // On their behalf, skip optional inputs by auto saving default/empty values for optional stats!
                        UserProfileManager.updateProfile(
                            context = context,
                            name = trimmedName,
                            phone = "",
                            bio = "",
                            upiId = ""
                        )
                        UserProfileManager.completeOnboarding(context)
                        Toast.makeText(context, "Optional details skipped. Welcome!", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("onboard_skip_btn")
                ) {
                    Text("Skip Optional Details", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // Read from flowing settings
    val email by UserProfileManager.currentUserEmail.collectAsStateWithLifecycle()
    val profileName by UserProfileManager.profileName.collectAsStateWithLifecycle()
    val profilePhone by UserProfileManager.profilePhone.collectAsStateWithLifecycle()
    val profileBio by UserProfileManager.profileBio.collectAsStateWithLifecycle()
    val profileUpi by UserProfileManager.profileUpi.collectAsStateWithLifecycle()

    var isEditing by remember { mutableStateOf(false) }

    var editName by remember(profileName) { mutableStateOf(profileName) }
    var editPhone by remember(profilePhone) { mutableStateOf(profilePhone) }
    var editBio by remember(profileBio) { mutableStateOf(profileBio) }
    var editUpi by remember(profileUpi) { mutableStateOf(profileUpi) }

    Column(
        modifier = modifier
            .background(Color(0xFFF3F0F5))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Visual Interactive Profile Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circle initials badge in Material 3
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFECE6F0), Color(0xFFD3C2E5))
                            )
                        )
                        .border(2.dp, Color(0xFF6750A4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = if (profileName.trim().isNotEmpty()) {
                        val names = profileName.trim().split(" ")
                        if (names.size > 1) {
                            "${names[0].firstOrNull() ?: ""}${names[1].firstOrNull() ?: ""}".uppercase()
                        } else {
                            (names[0].take(2)).uppercase()
                        }
                    } else {
                        "BB"
                    }
                    Text(
                        text = initials,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF6750A4)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = profileName.ifEmpty { "BillBuddy User" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Text(
                    text = email ?: "No Active Session",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                if (profileBio.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "\"$profileBio\"",
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile details form / visual key-value data list card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Member Details" else "My Personal Details",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    IconButton(
                        onClick = {
                            if (isEditing) {
                                // Cancel editing mode - restore values
                                editName = profileName
                                editPhone = profilePhone
                                editBio = profileBio
                                editUpi = profileUpi
                            }
                            isEditing = !isEditing
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isEditing) {
                    // Editable fields
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_name")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editUpi,
                        onValueChange = { editUpi = it },
                        label = { Text("Preferred UPI VPA") },
                        placeholder = { Text("username@upi") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_upi")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it.filter { c -> c.isDigit() || c == '+' } },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_phone")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Profile Headline / Bio") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("profile_edit_bio")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val trimmedName = editName.trim()
                            if (trimmedName.isEmpty()) {
                                Toast.makeText(context, "Display Name is required.", Toast.LENGTH_SHORT).show()
                            } else {
                                UserProfileManager.updateProfile(
                                    context,
                                    trimmedName,
                                    editPhone.trim(),
                                    editBio.trim(),
                                    editUpi.trim()
                                )
                                isEditing = false
                                Toast.makeText(context, "Profile settings updated!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("profile_save_edit_btn")
                    ) {
                        Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // View-only profiles
                    ProfileInfoRow(
                        icon = Icons.Default.Person,
                        label = "Display Name",
                        value = profileName.ifEmpty { "Not set" }
                    )
                    Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))

                    ProfileInfoRow(
                        icon = Icons.Default.Email,
                        label = "Account Email",
                        value = email ?: "N/A"
                    )
                    Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))

                    ProfileInfoRow(
                        icon = Icons.Default.Star, // Vector check star fallback Representation
                        label = "Preferred UPI VPA",
                        value = profileUpi.ifEmpty { "None (will default to split@billbuddy)" }
                    )
                    Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))

                    ProfileInfoRow(
                        icon = Icons.Default.Phone,
                        label = "Mobile Phone",
                        value = profilePhone.ifEmpty { "Not specified" }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Red sign out button
        OutlinedButton(
            onClick = {
                UserProfileManager.logout(context)
                Toast.makeText(context, "Signed out safely.", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
            border = BorderStroke(1.dp, Color(0xFFFDA4AF)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("profile_logout_btn")
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Log out icon", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sign Out of Session", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFFFAFF))
                .border(1.dp, Color(0xFFECE6F0), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color(0xFF6750A4), modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(text = label, fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF334155),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
