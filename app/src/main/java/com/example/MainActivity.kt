package com.example

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.BillBuddyDatabase
import com.example.database.BillEntity
import com.example.state.BillBuddyStateManager
import com.example.state.PopupState
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BillBuddyStateManager.initialize(this)
        com.example.state.UserProfileManager.initialize(this)

        setContent {
            MyApplicationTheme {
                MainAppFlow(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainAppFlow(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val isLoggedIn by com.example.state.UserProfileManager.isLoggedIn.collectAsStateWithLifecycle()
    val isNewUser by com.example.state.UserProfileManager.isNewUser.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Profile Screen

    AnimatedContent(
        targetState = Pair(isLoggedIn, isNewUser),
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "AppFlowTransition"
    ) { (loggedIn, newUser) ->
        when {
            !loggedIn -> {
                com.example.ui.AuthScreen(modifier = modifier)
            }
            newUser -> {
                com.example.ui.OnboardingScreen(modifier = modifier)
            }
            else -> {
                Scaffold(
                    modifier = modifier,
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 8.dp,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            NavigationBarItem(
                                selected = activeTab == 0,
                                onClick = { activeTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                label = { Text("Dashboard", fontSize = 11.sp, fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF6750A4),
                                    unselectedIconColor = Color(0xFF64748B),
                                    indicatorColor = Color(0xFFEADDFF)
                                ),
                                modifier = Modifier.testTag("tab_dashboard")
                            )
                            NavigationBarItem(
                                selected = activeTab == 1,
                                onClick = { activeTab = 1 },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("My Profile", fontSize = 11.sp, fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF6750A4),
                                    unselectedIconColor = Color(0xFF64748B),
                                    indicatorColor = Color(0xFFEADDFF)
                                ),
                                modifier = Modifier.testTag("tab_profile")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (activeTab == 0) {
                            BillBuddyContent(modifier = Modifier.fillMaxSize())
                        } else {
                            com.example.ui.UserProfileScreen(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillBuddyContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activePopupState by BillBuddyStateManager.activePopupState.collectAsStateWithLifecycle()

    // Query historical bills directly from our Room database flow
    val database = remember { BillBuddyDatabase.getDatabase(context) }
    val billsFlow = remember { database.billDao().getAllBills() }
    val billsList by billsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Tracks RECEIVE_SMS permission status state
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Dynamic Permission Request Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
        if (granted) {
            Toast.makeText(context, "SMS scanning capability activated!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission denied. Scan live SMS disabled.", Toast.LENGTH_LONG).show()
        }
    }

    // Sandbox template messages
    val presetTemplates = listOf(
        "Spent Rs. 1450.00 at Starbucks with HDFC Credit Card x8912.",
        "Your account xx1092 debited INR 5,200.00 at SWIGGY FOOD SE. Ref 601243.",
        "Transaction Status: Alert, ₹450.00 spent on Amazon Web Services on 20-May-2026."
    )

    var simulatedSmsText by remember { mutableStateOf(presetTemplates[0]) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSortOption by remember { mutableStateOf(SortOption.DATE_DESC) }
    var isManualFormExpanded by remember { mutableStateOf(false) }

    var manualMerchant by remember { mutableStateOf("") }
    var manualAmount by remember { mutableStateOf("") }

    // Dynamic filtering and sorting
    val filteredAndSortedBills = remember(billsList, searchQuery, selectedSortOption) {
        val query = searchQuery.trim()
        val temp = if (query.isEmpty()) {
            billsList
        } else {
            billsList.filter {
                it.merchant.contains(query, ignoreCase = true) ||
                it.smsBody.contains(query, ignoreCase = true) ||
                it.smsSender.contains(query, ignoreCase = true)
            }
        }
        when (selectedSortOption) {
            SortOption.DATE_DESC -> temp.sortedByDescending { it.timestamp }
            SortOption.DATE_ASC -> temp.sortedBy { it.timestamp }
            SortOption.AMOUNT_DESC -> temp.sortedByDescending { it.amount }
            SortOption.AMOUNT_ASC -> temp.sortedBy { it.amount }
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFFF3F0F5)) // Professional Polish light lavender/gray canvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // BRAND HEADER BLOCK
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // White circular badge with italic purple B from Stitch design HTML
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            color = Color(0xFF6750A4),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "BillBuddy",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B), // Slate 900
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = "Instant Gemini split link generator",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B) // Slate 500
                        )
                    }
                }

                // Professional Permission Status Badge
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (hasSmsPermission) Color(0xFFE2F8E9) else Color(0xFFFDE8E8),
                    modifier = Modifier.clickable {
                        if (!hasSmsPermission) {
                            permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (hasSmsPermission) Color(0xFF16A34A) else Color(0xFFE11D48))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasSmsPermission) "SMS Scan Active" else "SMS Scan Idle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasSmsPermission) Color(0xFF16A34A) else Color(0xFF9F1239)
                        )
                    }
                }
            }

            // SANDBOX SIMULATOR PANEL (Crisp white container with slate borders and shadows)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Test Sandbox",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simulated SMS Sandbox",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B) // Slate 900
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap a preset template below or type a custom financial message to trigger Gemini parsing:",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B) // Slate 500
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset chips container matching Professional theme
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetTemplates.forEachIndexed { index, template ->
                            val label = when (index) {
                                0 -> "☕ Starbucks"
                                1 -> "🍔 Swiggy Food"
                                else -> "🛒 AWS Cloud"
                            }
                            val isSelected = simulatedSmsText == template
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFEADDFF) else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF6750A4) else Color.Transparent
                                ),
                                modifier = Modifier.clickable {
                                    simulatedSmsText = template
                                }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF6750A4) else Color(0xFF475569),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = simulatedSmsText,
                        onValueChange = { simulatedSmsText = it },
                        textStyle = TextStyle(color = Color(0xFF1E293B), fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1E293B),
                            unfocusedTextColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            BillBuddyStateManager.handleIncomingSMS(
                                context,
                                sender = "SMS-SIMULATOR",
                                body = simulatedSmsText
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulate_parse_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6750A4) // Professional Purple
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Send, contentDescription = "Simulate", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simulate SMS & Run Gemini Parser", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // DYNAMIC DISMISSABLE BANNER: NO PERMISSION WARNING (Warm elegant Red Alert box)
            if (!hasSmsPermission) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { permissionLauncher.launch(Manifest.permission.RECEIVE_SMS) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Live SMS Scanning Disabled",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B),
                                fontSize = 13.sp
                            )
                            Text(
                                "Grant SMS permission to parse live UPI split requests directly when real SMS messages arrive.",
                                color = Color(0xFF7F1D1D),
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Grant",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // DYNAMIC STATISTICS HEADER (INSIGHTS SECTION)
            InsightsSection(bills = billsList)

            // EXPANDABLE MANUAL ENTRY CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isManualFormExpanded = !isManualFormExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Manual Entry Icon",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Bill Metadata Manually",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        Icon(
                            imageVector = if (isManualFormExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isManualFormExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "Perfect for manual or non-SMS merchant payments you wish to split dynamically:",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = manualMerchant,
                                onValueChange = { manualMerchant = it },
                                label = { Text("Merchant (e.g., Starbucks, Swiggy)", fontSize = 12.sp) },
                                textStyle = TextStyle(color = Color(0xFF1E293B), fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1E293B),
                                    unfocusedTextColor = Color(0xFF1E293B),
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedLabelColor = Color(0xFF6750A4),
                                    unfocusedLabelColor = Color(0xFF64748B)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = manualAmount,
                                onValueChange = { manualAmount = it.filter { char -> char.isDigit() || char == '.' } },
                                label = { Text("Total Bill Amount (₹)", fontSize = 12.sp) },
                                textStyle = TextStyle(color = Color(0xFF1E293B), fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1E293B),
                                    unfocusedTextColor = Color(0xFF1E293B),
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedLabelColor = Color(0xFF6750A4),
                                    unfocusedLabelColor = Color(0xFF64748B)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val amt = manualAmount.toDoubleOrNull() ?: 0.0
                                        val mct = manualMerchant.trim()
                                        if (mct.isNotEmpty() && amt > 0.0) {
                                            BillBuddyStateManager.insertManualBill(context, mct, amt)
                                            Toast.makeText(context, "Bill added to scans list", Toast.LENGTH_SHORT).show()
                                            manualMerchant = ""
                                            manualAmount = ""
                                            isManualFormExpanded = false
                                        } else {
                                            Toast.makeText(context, "Please write valid Merchant and Amount values.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Add to History", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val amt = manualAmount.toDoubleOrNull() ?: 0.0
                                        val mct = manualMerchant.trim()
                                        if (mct.isNotEmpty() && amt > 0.0) {
                                            BillBuddyStateManager.triggerManualSplit(mct, amt)
                                            BillBuddyStateManager.insertManualBill(context, mct, amt)
                                            manualMerchant = ""
                                            manualAmount = ""
                                            isManualFormExpanded = false
                                        } else {
                                            Toast.makeText(context, "Please write valid Merchant and Amount values.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6750A4)),
                                    border = BorderStroke(1.dp, Color(0xFF6750A4)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Instant Split", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // FILTER & SORT BAR
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by merchant or sender...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        textStyle = TextStyle(color = Color(0xFF1E293B), fontSize = 13.sp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1E293B),
                            unfocusedTextColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sort Strategy:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            SortOption.values().forEach { option ->
                                val isSelected = selectedSortOption == option
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFEADDFF) else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { selectedSortOption = option }
                                ) {
                                    Text(
                                        text = option.displayName,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF6750A4) else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // HISTORICAL LIST BLOCK
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "Search Results (${filteredAndSortedBills.size})" else "Historical Scans",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                if (billsList.isNotEmpty()) {
                    Text(
                        text = "Clear History",
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                BillBuddyStateManager.clearHistory(context)
                                Toast.makeText(context, "History wiped cleaner", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (filteredAndSortedBills.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Empty",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No Matching Bills Found" else "No Scanned Bills",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try tweaking your keyword queries or search terms." else "Extract records easily using the simulator above or wait for incoming transaction notifications.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAndSortedBills) { bill ->
                        BillHistoryItem(
                            bill = bill,
                            onDelete = {
                                BillBuddyStateManager.deleteBill(context, bill)
                                Toast.makeText(context, "Bill deleted from list", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // FLOATING RE-TRIGGER BUTTON IF STITCH CARD IS CLOSED
        // If they want to manually trigger split card for the last transaction
        if (activePopupState == PopupState.Idle && billsList.isNotEmpty()) {
            val lastBill = billsList.first()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        // Resurrect popup modal details for last bill
                        BillBuddyStateManager.handleIncomingSMS(
                            context = context,
                            sender = lastBill.smsSender,
                            body = lastBill.smsBody
                        )
                    },
                    containerColor = Color(0xFF6750A4),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Retry Split Popup")
                }
            }
        }

        // OVERLAY ACTIVE STATE MULTI-MODAL LOGIC
        when (val state = activePopupState) {
            is PopupState.Idle -> { /* Done */ }
            is PopupState.Parsing -> {
                ParsingStatusDialog(sender = state.sender, body = state.body)
            }
            is PopupState.ParsedError -> {
                ParsingErrorDialog(
                    errorMsg = state.error,
                    sender = state.sender,
                    body = state.body,
                    onDismiss = { BillBuddyStateManager.dismissPopup() }
                )
            }
            is PopupState.ParsedSuccess -> {
                StitchDesignedPopUpModal(
                    sender = state.sender,
                    body = state.body,
                    amount = state.amount,
                    merchant = state.merchant,
                    onDismiss = { BillBuddyStateManager.dismissPopup() }
                )
            }
        }
    }
}

@Composable
fun BillHistoryItem(
    bill: BillEntity,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val cleanDate = remember(bill.timestamp) { formatter.format(Date(bill.timestamp)) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle symbol with first merchant letter (Soft Purple styling mimicking Stitch logo)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEADDFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bill.merchant.firstOrNull()?.uppercase()?.toString() ?: "T",
                    color = Color(0xFF6750A4),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.merchant,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B), // Slate 900
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SMS Sender: ${bill.smsSender} • $cleanDate",
                    color = Color(0xFF64748B), // Slate 500
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${"%.2f".format(bill.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B), // Slate 900
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete record",
                        tint = Color(0xFF94A3B8), // Slate 400
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ParsingStatusDialog(sender: String, body: String) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF6750A4),
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Gemini AI Parsing SMS",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Gemini 2.5 Flash is analyzing string structures to extract merchant identity and total amounts...",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = body,
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(8.dp),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ParsingErrorDialog(
    errorMsg: String,
    sender: String,
    body: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFDA4AF), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Parsing Failure",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMsg,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show hint if key is default
                if (BuildConfig.GEMINI_API_KEY.isEmpty() || BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "⚠️ Gemini API key placeholder detected! Please configure your GEMINI_API_KEY inside the Secrets panel of project history page to enable full AI model extraction.",
                            fontSize = 11.sp,
                            color = Color(0xFF9F1239),
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Panel", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Stitch designed pop-up modal screen logic
 * Custom UI layout matching Google Stitch specifications:
 * Elegant light layered look, split controls, UPI builder syntax, and launch intent.
 */
@Composable
fun StitchDesignedPopUpModal(
    sender: String,
    body: String,
    amount: Double,
    merchant: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Modal internal variables - default to split@billbuddy as styled in specs
    var numberOfPeople by remember { mutableStateOf(2) } // Default divide among self + 1 friend = 2
    val preferredUpi by com.example.state.UserProfileManager.profileUpi.collectAsStateWithLifecycle()
    var vpaAddress by remember(preferredUpi) {
        mutableStateOf(if (preferredUpi.isNotEmpty()) preferredUpi else "split@billbuddy")
    }

    // Recalculate split amount dynamically
    val splitAmount = remember(amount, numberOfPeople) {
        if (numberOfPeople > 0) amount / numberOfPeople else amount
    }

    // Live string construction function that generates an Indian UPI deep-link syntax:
    // upi://pay?pa=yourvpa@upi&am={split_amount}&tn={merchant}
    val upiURL = remember(vpaAddress, splitAmount, merchant) {
        try {
            val encodedMerchant = java.net.URLEncoder.encode(merchant.trim(), "UTF-8")
            val formattedAmount = "%.2f".format(splitAmount)
            "upi://pay?pa=${vpaAddress.trim()}&am=$formattedAmount&tn=$encodedMerchant"
        } catch (e: Exception) {
            "upi://pay?pa=${vpaAddress.trim()}&am=$splitAmount&tn=payment"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f)) // Sleek semitransparent dim backdrop
                .clickable { onDismiss() }, // Click outer to close
            contentAlignment = Alignment.Center
        ) {
            // Prevent close when clicking card body
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clickable(enabled = false) { }
                    .border(
                        BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column {
                    // STITCH TOP TITLE BAR (Soft Lavender background with pulsing deep purple brand accent)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEADDFF))
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF6750A4)) // Pulsing tone
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SMS DETECTED • GEMINI AI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6750A4),
                                letterSpacing = 1.5.sp
                            )
                        }

                        Text(
                            text = "DISMISS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6750A4),
                            modifier = Modifier.clickable { onDismiss() }
                        )
                    }

                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // MERCHANT DETECTION DISPLAY
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Merchant Detected",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B) // Slate 500
                            )
                            Text(
                                text = merchant,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A) // Slate 900
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // METRICS GRID (Side-by-side total & split amount)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Total amount container card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(BorderStroke(1.dp, Color(0xFFF1F5F9)), RoundedCornerShape(16.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "TOTAL AMOUNT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8) // Slate 400
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹${"%.2f".format(amount)}",
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A) // Slate 900
                                    )
                                }
                            }

                            // Split amount container card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(BorderStroke(1.dp, Color(0xFFF1F5F9)), RoundedCornerShape(16.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "SPLIT ($numberOfPeople WAYS)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8) // Slate 400
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹${"%.2f".format(splitAmount)}",
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6750A4) // Stitch Purple
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // QR CODE INTERACTIVE CARD
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(vertical = 8.dp)
                                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                StitchQrCanvas(
                                    upiLink = upiURL,
                                    modifier = Modifier.size(130.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0xFF16A34A))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SCAN & PAY INSTANTLY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // PAYEE UPI INPUT FIELD
                        Text(
                            text = "Payee VPA Profile Address:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B) // Slate 500
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = vpaAddress,
                            onValueChange = { vpaAddress = it },
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            placeholder = { Text("e.g. split@billbuddy", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // SPLIT CONTROLLERS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Split Bill Between:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B) // Slate 900
                                )
                                Text(
                                    text = "Include yourself in count",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B) // Slate 500
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (numberOfPeople > 1) numberOfPeople-- },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                ) {
                                    Text("-", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }

                                Text(
                                    text = "$numberOfPeople",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                IconButton(
                                    onClick = { if (numberOfPeople < 20) numberOfPeople++ },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                ) {
                                    Text("+", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // REAL-TIME DEEP LINK PREVIEW & COPY ACTION
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VPA: $vpaAddress",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569) // Slate 600
                            )
                            Text(
                                text = "UPI DEEP-LINK READY",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8) // Slate 400
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("UPI deep-link", upiURL)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "UPI deep-link copied!", Toast.LENGTH_SHORT).show()
                                    } catch (clipboardEx: Exception) {
                                        android.util.Log.e("BillBuddy", "Clipboard write failed safety check", clipboardEx)
                                        Toast.makeText(context, "Unable to copy to clipboard automatically.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = upiURL,
                                    fontSize = 10.sp,
                                    color = Color(0xFF6750A4), // Brand Purple link representation
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Copy Link",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // VIBRANT GREEN WHATSAPP ACTION BUTTON FROM THEME SPECIFICATIONS
                        Button(
                            onClick = {
                                val prefilledText = "Split payment request for *$merchant*.\n" +
                                        "Total bill: ₹${"%.2f".format(amount)}\n" +
                                        "Your share to pay: *₹${"%.2f".format(splitAmount)}*\n" +
                                        "\nPay directly click here: $upiURL"

                                // Walk the context safely to find an Activity context
                                var activityContext: android.content.Context = context
                                val visitedContexts = mutableSetOf<android.content.Context>()
                                while (activityContext is android.content.ContextWrapper) {
                                    if (activityContext is android.app.Activity) {
                                        break
                                    }
                                    val base = activityContext.baseContext
                                    if (base == null || !visitedContexts.add(base)) {
                                        break
                                    }
                                    activityContext = base
                                }

                                try {
                                    val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(prefilledText)}")
                                        if (activityContext !is android.app.Activity) {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    }
                                    activityContext.startActivity(whatsappIntent)
                                } catch (e: Exception) {
                                    try {
                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, prefilledText)
                                            type = "text/plain"
                                        }
                                        val chooserIntent = Intent.createChooser(shareIntent, "Share with:")
                                        if (activityContext !is android.app.Activity) {
                                            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        activityContext.startActivity(chooserIntent)
                                    } catch (innerEx: Exception) {
                                        Toast.makeText(context, "No compatible browser or messaging app found to share.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("whatsapp_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366) // WhatsApp vibrant green
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Real vector message icon
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "WhatsApp icon representation",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "SEND SPLIT LINK",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class SortOption(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount")
}

@Composable
fun StitchQrCanvas(upiLink: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sizePx = size.width
        val moduleCount = 21
        val moduleSize = sizePx / moduleCount

        val locators = listOf(
            Pair(0, 0),
            Pair(moduleCount - 7, 0),
            Pair(0, moduleCount - 7)
        )

        drawRect(Color.White)

        for (loc in locators) {
            val startX = loc.first * moduleSize
            val startY = loc.second * moduleSize

            drawRect(
                color = Color(0xFF1E293B),
                topLeft = androidx.compose.ui.geometry.Offset(startX, startY),
                size = androidx.compose.ui.geometry.Size(7 * moduleSize, 7 * moduleSize)
            )
            drawRect(
                color = Color.White,
                topLeft = androidx.compose.ui.geometry.Offset(startX + moduleSize, startY + moduleSize),
                size = androidx.compose.ui.geometry.Size(5 * moduleSize, 5 * moduleSize)
            )
            drawRect(
                color = Color(0xFF6750A4),
                topLeft = androidx.compose.ui.geometry.Offset(startX + 2 * moduleSize, startY + 2 * moduleSize),
                size = androidx.compose.ui.geometry.Size(3 * moduleSize, 3 * moduleSize)
            )
        }

        val linkHash = upiLink.hashCode()
        val random = java.util.Random(linkHash.toLong())

        for (r in 0 until moduleCount) {
            for (c in 0 until moduleCount) {
                val isLocator = (r < 7 && c < 7) || 
                                (r < 7 && c >= moduleCount - 7) || 
                                (r >= moduleCount - 7 && c < 7)
                if (isLocator) continue

                if (r == 6 || c == 6) {
                    if ((r + c) % 2 == 0) {
                        drawRect(
                            color = Color(0xFF1E293B),
                            topLeft = androidx.compose.ui.geometry.Offset(c * moduleSize, r * moduleSize),
                            size = androidx.compose.ui.geometry.Size(moduleSize, moduleSize)
                        )
                    }
                    continue
                }

                if (random.nextBoolean()) {
                    drawRect(
                        color = if (random.nextInt(10) > 7) Color(0xFF6750A4) else Color(0xFF0F172A),
                        topLeft = androidx.compose.ui.geometry.Offset(c * moduleSize, r * moduleSize),
                        size = androidx.compose.ui.geometry.Size(moduleSize, moduleSize)
                    )
                }
            }
        }
    }
}

@Composable
fun InsightsSection(bills: List<BillEntity>) {
    val stats = remember(bills) {
        val totalAmount = bills.sumOf { it.amount }
        val count = bills.size
        val avgAmount = if (count > 0) totalAmount / count else 0.0
        val topMerchantObj = bills.groupBy { it.merchant.trim() }
            .maxByOrNull { it.value.size }
        val topMerchant = if (topMerchantObj != null && topMerchantObj.value.isNotEmpty()) {
            topMerchantObj.key
        } else {
            "N/A"
        }
        val topCount = topMerchantObj?.value?.size ?: 0
        Triple(totalAmount, avgAmount, if (topMerchant == "N/A") "N/A" else "$topMerchant ($topCount)")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF9FC)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, Color(0xFFE6E1E9), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Analysis",
                    tint = Color(0xFF6750A4),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Historical Pulse Metrics",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "TOTAL SCANS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${"%.0f".format(stats.first)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "AVG AMOUNT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${"%.0f".format(stats.second)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6750A4),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "TOP VENDOR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stats.third,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
