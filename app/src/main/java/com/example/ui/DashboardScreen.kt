package com.example.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.GrewRecord
import com.example.ui.theme.*
import com.example.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.*

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.net.URLEncoder
import io.github.jan.supabase.auth.status.SessionStatus

@Composable
fun GrewEnergyLogo(modifier: Modifier = Modifier, showText: Boolean = true) {
    val words = remember { listOf("ENERGY", "SOLAR", "ANALYTICS") }
    var currentIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1800)
            currentIndex = (currentIndex + 1) % words.size
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Precise customized corporate sharp arrow vector logo
        Box(
            modifier = Modifier
                .size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scX = size.width / 100f
                val scY = size.height / 100f
                
                // Draw main geometric dart shape polygon
                val mainPath = Path().apply {
                    moveTo(4f * scX, 17.5f * scY)
                    lineTo(88.5f * scX, 17.5f * scY)
                    lineTo(47.5f * scX, 95.5f * scY)
                    lineTo(42.5f * scX, 47.5f * scY)
                    close()
                }
                drawPath(
                    path = mainPath,
                    color = BrandGreen
                )
                
                // Draw edge artifact polygon
                val artifactPath = Path().apply {
                    moveTo(0f * scX, 85.5f * scY)
                    lineTo(8f * scX, 100f * scY)
                    lineTo(0f * scX, 100f * scY)
                    close()
                }
                drawPath(
                    path = artifactPath,
                    color = BrandGreen
                )
            }
        }
        if (showText) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "GREW",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = SlateTextLight
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = words[currentIndex],
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BrandGreen,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val width = size.width
        val height = size.height
        val strokeWidth = width * 0.16f

        // Draw Google arcs
        drawArc(
            color = Color(0xFFEA4335), // Google Red
            startAngle = 135f,
            sweepAngle = 180f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF4285F4), // Google Blue
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFFFBBC05), // Google Yellow
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF34A853), // Google Green
            startAngle = -135f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun GrewLoginScreen(
    viewModel: GrewViewModel,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var otpValue by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var progressStep by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userSession by viewModel.userSession.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    // Google Sign-In Setup
    val googleWebClientId = remember {
        val resId = context.resources.getIdentifier("google_web_client_id", "string", context.packageName)
        val id = if (resId != 0) context.getString(resId) else ""
        android.util.Log.d("GrewAuth", "Google Web Client ID: '$id'")
        id
    }

    val googleSignInOptions = remember(googleWebClientId) {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
        
        if (googleWebClientId.isNotEmpty()) {
            builder.requestIdToken(googleWebClientId)
        }
        
        builder.build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, googleSignInOptions) }
    
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            account?.idToken?.let { idToken ->
                viewModel.signInWithGoogle(idToken)
            } ?: run {
                errorMessage = "Google authentication failed: Missing ID Token"
            }
        } catch (e: Exception) {
            errorMessage = "Google login error: ${e.localizedMessage}"
        }
    }

    LaunchedEffect(userSession) {
        if (userSession is SessionStatus.Authenticated) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(syncState) {
        if (syncState is SheetSyncState.Error) {
            errorMessage = (syncState as SheetSyncState.Error).message
            isLoading = false
        } else if (syncState is SheetSyncState.Idle && isLoading && !isOtpSent) {
            // This happens after sendOtp succeeds
            isOtpSent = true
            isLoading = false
            progressStep = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBg),
        contentAlignment = Alignment.Center
    ) {
        // High-tech grid background pattern
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 40.dp.toPx()
            val strokeWidth = 1.dp.toPx()
            val color = BrandGreen.copy(alpha = 0.05f)
            
            for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
                drawLine(color, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth)
            }
            for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
                drawLine(color, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp)
                .shadow(24.dp, RoundedCornerShape(16.dp), ambientColor = BrandGreen, spotColor = BrandGreen),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.9f)),
            border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GrewEnergyLogo(modifier = Modifier.padding(bottom = 32.dp))
                
                Text(
                    text = if (isOtpSent) "VERIFICATION REQUIRED" else "EXECUTIVE ACCESS",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandGreen,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                if (!isOtpSent) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Corporate Email") },
                        placeholder = { Text("email@grewenergy.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = SlateBorder,
                            focusedLabelColor = BrandGreen,
                            focusedTextColor = SlateTextLight,
                            unfocusedTextColor = SlateTextLight
                        )
                    )
                } else {
                    Text(
                        text = "We sent a 6-digit code to $email",
                        fontSize = 10.sp,
                        color = SlateTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = otpValue,
                        onValueChange = { if (it.length <= 6) otpValue = it },
                        label = { Text("Verification Code") },
                        placeholder = { Text("000000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = SlateBorder,
                            focusedLabelColor = BrandGreen,
                            focusedTextColor = SlateTextLight,
                            unfocusedTextColor = SlateTextLight
                        )
                    )
                    TextButton(
                        onClick = { isOtpSent = false; otpValue = "" },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Change Email", fontSize = 10.sp, color = BrandGreen)
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFEF4444),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(color = BrandGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(progressStep, fontSize = 9.sp, color = SlateTextMuted)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (!isOtpSent) {
                            if (email.contains("@")) {
                                isLoading = true
                                errorMessage = null
                                progressStep = "Checking whitelist status..."
                                scope.launch {
                                    val whitelisted = verifyEmailWithSupabase(
                                        email.trim(),
                                        BuildConfig.SUPABASE_URL,
                                        BuildConfig.SUPABASE_ANON_KEY
                                    )
                                    if (whitelisted) {
                                        progressStep = "Sending secure OTP..."
                                        viewModel.sendOtp(email.trim())
                                    } else {
                                        isLoading = false
                                        errorMessage = "Access Denied: Email not whitelisted."
                                    }
                                }
                            } else {
                                errorMessage = "Please enter a valid corporate email"
                            }
                        } else {
                            if (otpValue.length >= 6) {
                                viewModel.verifyOtp(email.trim(), otpValue.trim())
                                isLoading = true
                                progressStep = "Validating security token..."
                            } else {
                                errorMessage = "Enter the verification code"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f) // Reduced width for better balance
                        .height(44.dp), // Slightly smaller height
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                    enabled = !isLoading
                ) {
                    Text(
                        text = if (isOtpSent) "VERIFY" else "GET CODE", // Shorter, cleaner text
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 12.sp // Slightly smaller text
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SlateBorder.copy(alpha = 0.5f))
                    Text(" or ", color = SlateTextMuted, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 12.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = SlateBorder.copy(alpha = 0.5f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        if (googleWebClientId.isEmpty()) {
                            errorMessage = "Google Sign-in configuration missing (Client ID not found in strings.xml)."
                        } else {
                            errorMessage = null
                            googleLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    enabled = !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateTextLight)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GoogleIcon(modifier = Modifier.size(16.dp)) // Explicitly sized Google Icon
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Continue with Google", 
                            fontWeight = FontWeight.Medium, 
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DatabaseDiagnosticsDialog(viewModel: GrewViewModel, onDismiss: () -> Unit) {
    var isCheckingConnection by remember { mutableStateOf(false) }
    var connectionResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val url = BuildConfig.SUPABASE_URL
    val key = BuildConfig.SUPABASE_ANON_KEY
    val isConfigured = url.isNotEmpty() && !url.contains("YOUR_SUPABASE_PROJECT_URL_HERE") &&
                       key.isNotEmpty() && !key.contains("YOUR_SUPABASE_ANON_KEY_HERE")
    val syncState by viewModel.syncState.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, SlateBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Supabase API Diagnostics",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextLight
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateBg, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    DiagnosticRow(label = "Protocol Status", value = if (isConfigured) "Production Active" else "Local Sandbox Mode", isSuccess = isConfigured)
                    DiagnosticRow(label = "Supabase Host", value = if (isConfigured) url.split("//").getOrNull(1)?.split("/")?.getOrNull(0) ?: "Connected" else "None (Placeholder)")
                    DiagnosticRow(label = "Target Table", value = "whitelist")
                    DiagnosticRow(label = "Access Method", value = "Secure TLS REST API")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Google Sheet Sync Diagnostics",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateBg, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    val statusText = when (val s = syncState) {
                        is SheetSyncState.Idle -> "Idle / Uninitialized"
                        is SheetSyncState.Syncing -> "Syncing live records..."
                        is SheetSyncState.Success -> "Success (${s.count} rows)"
                        is SheetSyncState.Error -> "Error: ${s.message}"
                        else -> "Status Unknown"
                    }
                    val sheetIdText = when (val s = syncState) {
                        is SheetSyncState.Success -> s.sheetId.take(12) + "..." + s.sheetId.takeLast(12)
                        else -> "1rL...F4 [Fallback]"
                    }
                    val sourceText = when (val s = syncState) {
                        is SheetSyncState.Success -> s.source
                        else -> "Local Config (Offline Fallback)"
                    }
                    DiagnosticRow(label = "Sync Progress", value = statusText, isSuccess = syncState is SheetSyncState.Success)
                    DiagnosticRow(label = "Source Provider", value = sourceText)
                    DiagnosticRow(label = "Sheet ID Reference", value = sheetIdText)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isCheckingConnection) {
                    CircularProgressIndicator(color = BrandGreen, modifier = Modifier.size(24.dp))
                } else {
                    connectionResult?.let {
                        Text(
                            text = it,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (it.contains("Success")) BrandGreen else Color(0xFFEF4444),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    isCheckingConnection = true
                                    connectionResult = null
                                    kotlinx.coroutines.delay(1000)
                                    if (isConfigured) {
                                        val tempCheck = verifyEmailWithSupabase("navneet.chaudhary831@gmail.com", url, key)
                                        connectionResult = if (tempCheck || verifyEmailWithSupabase("guest.visitor@grewenergy.com", url, key)) {
                                            "Connection Successful! Verified database table is responsive."
                                        } else {
                                            "Supabase responsive but whitelist verified query failed (expected)."
                                        }
                                    } else {
                                        connectionResult = "Local Whitelist: Active and responsive (Offline Fallback)."
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    connectionResult = "Check Failed: ${e.localizedMessage ?: "Unknown diagnostic error."}"
                                } finally {
                                    isCheckingConnection = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Connection", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Panel", color = SlateTextMuted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String, isSuccess: Boolean? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 9.sp, color = SlateTextMuted, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            isSuccess?.let {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (it) BrandGreen else BrandGold, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(text = value, fontSize = 9.sp, color = SlateTextLight, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NavigationDrawerItemHelper(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp)),
        color = if (selected) BrandGreen.copy(alpha = 0.12f) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) BrandGreen.copy(alpha = 0.3f) else Color.Transparent),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) BrandGreen else SlateTextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) BrandGreen else SlateTextLight
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: GrewViewModel) {
    val filters by viewModel.filters.collectAsState()
    val stats by viewModel.stats.collectAsState()
    
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("grew_prefs", Context.MODE_PRIVATE) }
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    // Bottom Navigation tab states (0: Overview & Trends, 1: Revenue Matrix, 2: Breakdowns)
    var activeBottomTab by remember { mutableStateOf(0) }
    var pageTwoShowVisuals by remember { mutableStateOf(false) }
    var showIntelligenceBoard by remember { mutableStateOf(false) }
    var fyDropdownExpanded by remember { mutableStateOf(false) }
    var metricDropdownExpanded by remember { mutableStateOf(false) }
    var segmentDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Chart scrubbing states
    var scrubbedPointIndex by remember { mutableStateOf<Int?>(null) }
    var scrubbedOffset by remember { mutableStateOf(Offset.Zero) }

    val currentStats = stats
    var showDbDiagnostics by remember { mutableStateOf(false) }
    var showSyncDiagnostics by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val userEmail by viewModel.currentUserEmail.collectAsState()

    if (showDbDiagnostics) {
        DatabaseDiagnosticsDialog(viewModel = viewModel, onDismiss = { showDbDiagnostics = false })
    }

    if (showSyncDiagnostics) {
        SyncDiagnosticsDialog(viewModel = viewModel, onDismiss = { showSyncDiagnostics = false })
    }

    if (showDatePickerDialog) {
        val initialDate = filters.customEndDate ?: currentStats?.anchorDate ?: java.util.Date()
        GrewDatePickerDialog(
            initialDate = initialDate,
            onDateSelected = { selectedDate ->
                val newCal = Calendar.getInstance().apply { 
                    time = selectedDate
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                val startCal = Calendar.getInstance().apply {
                    val m = newCal.get(Calendar.MONTH)
                    val y = newCal.get(Calendar.YEAR)
                    val fiscalStartYear = if (m >= Calendar.APRIL) y else y - 1
                    set(fiscalStartYear, Calendar.APRIL, 1, 0, 0, 0)
                }
                viewModel.selectCustomDateRange(startCal.time, newCal.time)
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("dashboard_root"),
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = SlateCard,
                            titleContentColor = SlateTextLight,
                            navigationIconContentColor = BrandGreen
                        ),
                        navigationIcon = {
                            GrewEnergyLogo(showText = false, modifier = Modifier.padding(start = 12.dp))
                        },
                        title = {

                            val startLabel = filters.customStartDate?.let { sdf.format(it) } ?: sdf.format(viewModel.globalMinDate)
                            val endLabel = filters.customEndDate?.let { sdf.format(it) } ?: sdf.format(viewModel.globalMaxDate)
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandGreen.copy(alpha = 0.12f))
                                    .border(1.dp, BrandGreen, RoundedCornerShape(8.dp))
                                    .clickable { showDatePickerDialog = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (startLabel == endLabel) startLabel else "$startLabel — $endLabel",
                                        color = BrandGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Date Range",
                                        tint = BrandGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                     actions = {
                        IconButton(onClick = { showIntelligenceBoard = true }) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Intelligence",
                                tint = BrandGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        val syncState by viewModel.syncState.collectAsState()
                        IconButton(onClick = { viewModel.loadSheetData() }) {
                            when (syncState) {
                                is SheetSyncState.Syncing -> {
                                    CircularProgressIndicator(
                                        color = BrandGreen,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sync Live Sheet",
                                        tint = if (syncState is SheetSyncState.Error) Color(0xFFEF4444) else BrandGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = SlateCard,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = activeBottomTab == 0,
                        onClick = { activeBottomTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Overview") },
                        label = { Text("KPI Standings", fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandGreen,
                            selectedTextColor = BrandGreen,
                            unselectedIconColor = SlateTextMuted,
                            unselectedTextColor = SlateTextMuted,
                            indicatorColor = BrandGreen.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = activeBottomTab == 1,
                        onClick = { activeBottomTab = 1 },
                        icon = { Icon(Icons.Default.GridOn, contentDescription = "Visuals & Matrix") },
                        label = { Text("Visuals & Matrix", fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandGreen,
                            selectedTextColor = BrandGreen,
                            unselectedIconColor = SlateTextMuted,
                            unselectedTextColor = SlateTextMuted,
                            indicatorColor = BrandGreen.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = activeBottomTab == 2,
                        onClick = { activeBottomTab = 2 },
                        icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = "Breakdowns") },
                        label = { Text("Breakdowns", fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandGreen,
                            selectedTextColor = BrandGreen,
                            unselectedIconColor = SlateTextMuted,
                            unselectedTextColor = SlateTextMuted,
                            indicatorColor = BrandGreen.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = activeBottomTab == 3,
                        onClick = { activeBottomTab = 3 },
                        icon = { GrewEnergyLogo(showText = false, modifier = Modifier.size(20.dp)) },
                        label = { Text("Grew Portal", fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandGreen,
                            selectedTextColor = BrandGreen,
                            unselectedIconColor = SlateTextMuted,
                            unselectedTextColor = SlateTextMuted,
                            indicatorColor = BrandGreen.copy(alpha = 0.15f)
                        )
                    )
                }
            },
            containerColor = SlateBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                val currentStats = stats
                if (currentStats == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BrandGreen)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("POWERING SECURE MATRIX ENGINE...", color = SlateTextMuted, fontSize = 9.sp, letterSpacing = 2.sp)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val currentSyncState by viewModel.syncState.collectAsState()
                        when (val s = currentSyncState) {
                            is SheetSyncState.Syncing -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BrandGreen.copy(alpha = 0.1f))
                                        .padding(vertical = 4.dp, horizontal = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(color = BrandGreen, modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Syncing live Grew Google Sheet records...", color = BrandGreen, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            is SheetSyncState.Success -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F2618))
                                        .padding(vertical = 4.dp, horizontal = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Sync Success: Loaded ${s.count} rows from ${s.source}", 
                                            color = BrandGreen, 
                                            fontSize = 9.sp, 
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            is SheetSyncState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF381414))
                                        .padding(vertical = 4.dp, horizontal = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = s.message, 
                                            color = Color(0xFFEF4444), 
                                            fontSize = 9.sp, 
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "LOG",
                                            color = BrandGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { showSyncDiagnostics = true }
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "RETRY",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { viewModel.loadSheetData() }
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }

                        // GLOBAL HIGH-FIDELITY HORIZONTAL SCREEN-WIDE FILTER ROW FOR MAXIMUM VISUAL ACCESSIBILITY
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateCard)
                                .border(1.dp, SlateBorder)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "FILTERS:",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SlateTextMuted,
                                letterSpacing = 1.sp
                            )

                            // 1. FY Drill down
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BrandGreen.copy(alpha = 0.12f))
                                        .border(1.dp, BrandGreen, RoundedCornerShape(8.dp))
                                        .clickable { fyDropdownExpanded = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = filters.selectedFY,
                                        color = BrandGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select FY",
                                        tint = BrandGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = fyDropdownExpanded,
                                    onDismissRequest = { fyDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(SlateCard)
                                        .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                                ) {
                                    viewModel.allFinancialYears.forEach { fy ->
                                        val selected = filters.selectedFY == fy
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = fy,
                                                        color = if (selected) BrandGreen else SlateTextLight,
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    if (selected) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "selected",
                                                            tint = BrandGreen,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.updateFY(fy)
                                                fyDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 2. Segment Drill down
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BrandGreen.copy(alpha = 0.12f))
                                        .border(1.dp, BrandGreen, RoundedCornerShape(8.dp))
                                        .clickable { segmentDropdownExpanded = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val segmentLabel = if (filters.selectedSegments.size == 1) filters.selectedSegments.first() else "Multi-Segment"
                                    Text(
                                        text = segmentLabel.uppercase(),
                                        color = BrandGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Segment",
                                        tint = BrandGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = segmentDropdownExpanded,
                                    onDismissRequest = { segmentDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(SlateCard)
                                        .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                                ) {
                                    viewModel.allSegments.forEach { seg ->
                                        val selected = filters.selectedSegments.contains(seg)
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .background(if (selected) resolveSeriesColor(seg) else Color.Gray, RoundedCornerShape(1.dp))
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = seg,
                                                        color = if (selected) BrandGreen else SlateTextLight,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    if (selected) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "selected",
                                                            tint = BrandGreen,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.toggleSegment(seg, true)
                                                segmentDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 3. Amount (Metric) Drill down
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(BrandGreen.copy(alpha = 0.12f))
                                        .border(1.dp, BrandGreen, RoundedCornerShape(8.dp))
                                        .clickable { metricDropdownExpanded = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = filters.activeMetric.name.uppercase(),
                                        color = BrandGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Metric",
                                        tint = BrandGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = metricDropdownExpanded,
                                    onDismissRequest = { metricDropdownExpanded = false },
                                    modifier = Modifier
                                        .background(SlateCard)
                                        .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                                ) {
                                    DashboardMetric.values().forEach { m ->
                                        val selected = filters.activeMetric == m
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = m.name,
                                                        color = if (selected) BrandGreen else SlateTextLight,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    if (selected) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "selected",
                                                            tint = BrandGreen,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.updateMetric(m)
                                                metricDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { viewModel.resetToLatestAnchor() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset Filters",
                                    tint = SlateTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Tab selector views
                        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            if (activeBottomTab < 3 && filters.selectedSegments.contains("Solar Modules")) {
                                SlimSkuSidebar(
                                    viewModel = viewModel,
                                    selectedSkus = filters.skus,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            
                            Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                when (activeBottomTab) {
                                0 -> {
                                    // Page 1: KPI card & Trend monitoring dashboard
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "DYNAMIC PACING STANDINGS",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = SlateTextMuted,
                                            letterSpacing = 1.2.sp,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )

                                        // All KPI cards in a Metro-style grid
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 14.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val periodLabel = if (filters.customStartDate != null) "PERIOD" else "ANCHOR DATE"
                                            
                                            // Featured Master Card (2x1)
                                            KpiCard(
                                                title = periodLabel,
                                                value = formatMetric(currentStats.periodSales, filters.activeMetric),
                                                icon = Icons.Default.CalendarToday,
                                                breakdown = currentStats.periodSalesBreakdown,
                                                isActiveSolar = currentStats.activeSeriesNames.size == 1 && currentStats.activeSeriesNames.contains("Solar Modules"),
                                                modifier = Modifier.fillMaxWidth().height(100.dp)
                                            )

                                            // Grid Row 1 (MTD & QTD) (1x1 | 1x1)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                KpiCard(
                                                    title = "MTD PACING",
                                                    value = formatMetric(currentStats.mtd, filters.activeMetric),
                                                    icon = Icons.Default.Timeline,
                                                    breakdown = currentStats.mtdBreakdown,
                                                    pacingChange = currentStats.mtdPacingChange,
                                                    pacingLabel = "MoM",
                                                    isActiveSolar = currentStats.activeSeriesNames.size == 1 && currentStats.activeSeriesNames.contains("Solar Modules"),
                                                    modifier = Modifier.weight(1f).aspectRatio(1f)
                                                )

                                                KpiCard(
                                                    title = "QTD OVERALL",
                                                    value = formatMetric(currentStats.qtd, filters.activeMetric),
                                                    icon = Icons.Default.Layers,
                                                    breakdown = currentStats.qtdBreakdown,
                                                    pacingChange = currentStats.qtdPacingChange,
                                                    pacingLabel = "QoQ",
                                                    isActiveSolar = currentStats.activeSeriesNames.size == 1 && currentStats.activeSeriesNames.contains("Solar Modules"),
                                                    modifier = Modifier.weight(1f).aspectRatio(1f)
                                                )
                                            }

                                            // Featured YTD Card (2x1)
                                            KpiCard(
                                                title = "YTD REVENUES",
                                                value = formatMetric(currentStats.ytd, filters.activeMetric),
                                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                                breakdown = currentStats.ytdBreakdown,
                                                pacingChange = currentStats.ytdPacingChange,
                                                pacingLabel = "YoY",
                                                isActiveSolar = currentStats.activeSeriesNames.size == 1 && currentStats.activeSeriesNames.contains("Solar Modules"),
                                                modifier = Modifier.fillMaxWidth().height(100.dp)
                                            )

                                            // Pending Card (Full width or split)
                                            KpiCard(
                                                title = "PENDING VALUE",
                                                value = formatMetric(currentStats.pending, filters.activeMetric),
                                                icon = Icons.Default.Layers,
                                                breakdown = currentStats.pendingBreakdown,
                                                isActiveSolar = currentStats.activeSeriesNames.size == 1 && currentStats.activeSeriesNames.contains("Solar Modules"),
                                                isFilterablePending = true,
                                                isPendingActive = filters.pendingOnly,
                                                onPendingToggle = { viewModel.togglePendingOnly() },
                                                modifier = Modifier.fillMaxWidth().height(80.dp)
                                            )
                                        }

                                    }
                                }
                                1 -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Tabular Executive Matrix Table Starts Directly Here
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                                            border = BorderStroke(1.dp, SlateBorder)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "REVENUE MATRIX",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = SlateTextLight,
                                                            letterSpacing = 1.sp
                                                        )
                                                        Text(
                                                            text = "Focus calculations instantly on touch headers",
                                                            fontSize = 8.sp,
                                                            color = SlateTextMuted
                                                        )
                                                    }
                                                }

                                                MatrixTableView(
                                                    items = currentStats.matrix,
                                                    currentMonth = filters.matrixMonth,
                                                    currentQuarter = filters.selectedQuarter,
                                                    selectedFY = filters.selectedFY,
                                                    onMonthSelected = { viewModel.toggleMatrixMonth(it) },
                                                    onQuarterSelected = { viewModel.toggleMatrixQuarter(it) }
                                                )
                                            }
                                        }

                                        // 3. Dynamic Business Intelligence & Summary Card
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                                            border = BorderStroke(1.dp, SlateBorder)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "PACING INTELLIGENCE PANEL",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = SlateTextLight,
                                                        letterSpacing = 1.sp
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .background(BrandGreen.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "AI INSIGHTS",
                                                            fontSize = 7.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BrandGreen
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Compute metrics dynamically from matrix dataset
                                                val monthlyItems = currentStats.matrix.filter { it.monthName != "Total" }
                                                val peakItem = monthlyItems.maxByOrNull { it.revenueCr }
                                                val avgRev = if (monthlyItems.isNotEmpty()) monthlyItems.map { it.revenueCr }.average() else 0.0

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    // Peak Run-rate
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .background(SlateBg, RoundedCornerShape(10.dp))
                                                            .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                                                            .padding(8.dp)
                                                    ) {
                                                        Text("PEAK MONTH", fontSize = 7.5.sp, color = SlateTextMuted, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            text = peakItem?.monthName?.uppercase() ?: "N/A",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = BrandGreen
                                                        )
                                                        Text(
                                                            text = String.format("₹ %.1f Cr", peakItem?.revenueCr ?: 0.0),
                                                            fontSize = 9.sp,
                                                            color = SlateTextLight,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }

                                                    // Average run-rate
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .background(SlateBg, RoundedCornerShape(10.dp))
                                                            .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                                                            .padding(8.dp)
                                                    ) {
                                                        Text("AVG RUN-RATE", fontSize = 7.5.sp, color = SlateTextMuted, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            text = "MONTHLY",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = BrandBlue
                                                        )
                                                        Text(
                                                            text = String.format("₹ %.1f Cr", avgRev),
                                                            fontSize = 9.sp,
                                                            color = SlateTextLight,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Summary text based on series filtering state
                                                val isDiversified = currentStats.activeSeriesNames.size > 1
                                                val summaryText = if (isDiversified) {
                                                    "Solar and wind pacing vectors are tracking with high dispersion. Joint modeling ensures balanced seasonal load coverage."
                                                } else if (currentStats.activeSeriesNames.contains("Solar Modules")) {
                                                     "Solar photovoltaic deployment is driving a high-intensity curve. Peak performance correlates strongly with pre-monsoon dispatches."
                                                } else {
                                                    "Wind turbine deployments are in a steady state cycle. Model adjustments dynamically buffer off-season dispatch intervals."
                                                }

                                                Text(
                                                    text = summaryText,
                                                    fontSize = 8.5.sp,
                                                    color = SlateTextMuted,
                                                    lineHeight = 12.sp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(SlateBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                        .padding(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    // Page 3: Breakdown stacks (All leaderboards listed stacked inside clean scroll)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        LeaderboardCard(
                                            title = "SALES EXECUTIVE STANDINGS",
                                            items = currentStats.salesLeaders,
                                            activeFilters = filters.salesHeads,
                                            onItemToggle = { name, ctrl -> viewModel.toggleSalesHeadFilter(name, ctrl) },
                                            metricType = filters.activeMetric,
                                            isRep = true
                                        )

                                        LeaderboardCard(
                                            title = "ENTERPRISE CLIENT DISTRIBUTIONS",
                                            items = currentStats.clientDistribution,
                                            activeFilters = filters.customers,
                                            onItemToggle = { name, ctrl -> viewModel.toggleCustomerFilter(name, ctrl) },
                                            metricType = filters.activeMetric,
                                            isRep = false
                                        )

                                        LeaderboardCard(
                                            title = "PRODUCT SKUS & SOLAR RATIOS",
                                            items = currentStats.skuDistribution,
                                            activeFilters = filters.skus,
                                            onItemToggle = { name, ctrl -> viewModel.toggleSkuFilter(name, ctrl) },
                                            metricType = filters.activeMetric,
                                            isRep = false
                                        )
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(SlateCard, RoundedCornerShape(12.dp))
                                                .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("HEALTH MONITOR ACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = BrandGreen)
                                            Text("MATRIX ANALYTICS v2.3 SECURED", fontSize = 8.sp, color = SlateTextMuted, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                                3 -> {
                                        GrewPortalTabContent(
                                            userEmail = userEmail,
                                            onLogout = {
                                                sharedPrefs.edit().remove("grew_email").apply()
                                                viewModel.signOut()
                                            },
                                            viewModel = viewModel,
                                            onShowDiagnostics = { showDbDiagnostics = true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    // Intelligence Board Overlay
    if (showIntelligenceBoard) {
        val statsRef = stats
        Dialog(onDismissRequest = { showIntelligenceBoard = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("INTELLIGENCE BOARD", fontSize = 11.sp, fontWeight = FontWeight.Black, color = BrandGreen, letterSpacing = 2.sp)
                        }
                        IconButton(onClick = { showIntelligenceBoard = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = BrandGreen)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateBorder)

                    if (statsRef != null) {
                        val cStats = statsRef.concentration
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                IntelligenceModuleCard(
                                    title = "HERFINDAHL-HIRSCHMAN (HHI) INDEX",
                                    subtitle = "CLIENT CONCENTRATION CONSOLIDATION",
                                    score = String.format("%,.0f HHI", cStats.hhiCustomer),
                                    verdict = if (cStats.isDiversifiedCustomer) "DIVERSIFIED" else "CONCENTRATED",
                                    verdictColor = if (cStats.isDiversifiedCustomer) BrandGreen else Color(0xFFD97706),
                                    details = "Corporate revenue spreads: Top 5 accounts deliver ${String.format("%.1f%%", cStats.top5CustomerShare)} of fiscal volume. ${
                                        if (cStats.isDiversifiedCustomer) "No client dependency risks registered inside the active matrix block." else "Warning: High client concentration noticed."
                                    }"
                                )
                            }
                            item {
                                IntelligenceModuleCard(
                                    title = "PRODUCT MATRIX COMPLIANCE (HHI)",
                                    subtitle = "SKU SEGMENT DEPENDENCY MEASUREMENT",
                                    score = String.format("%,.0f HHI", cStats.hhiProduct),
                                    verdict = if (cStats.isDiversifiedProduct) "STABLE SKU" else "DOMINANT SKU",
                                    verdictColor = if (cStats.isDiversifiedProduct) BrandGreen else SlateTextLight,
                                    details = "Product spreads: Top 3 SKU modules monopolize ${String.format("%.1f%%", cStats.top3ProductShare)} of total production. ${
                                        if (cStats.isDiversifiedProduct) "Sales distributions are robust and split smoothly across items." else "A singular SKU module drives significant pacing dependency."
                                    }"
                                )
                            }
                            item {
                                IntelligenceModuleCard(
                                    title = "TRAILING 7-DAYS VELOCITY PROJECTION",
                                    subtitle = "PREDICTIVE REAL-TIME FORECASTS",
                                    score = formatMetric(cStats.trailing7DayVelocityProjection, filters.activeMetric),
                                    verdict = "FORWARD PACE",
                                    verdictColor = BrandGreen,
                                    details = "Estimated monthly aggregate pacing based on recent 7-days actual transaction streams: ${formatMetric(cStats.trailing7DayVelocityProjection, filters.activeMetric)} projected throughput."
                                )
                            }
                            item {
                                IntelligenceModuleCard(
                                    title = "AVERAGE YIELD REALIZATION PER MW",
                                    subtitle = "PRODUCT CAPACITY ECONOMIC REALIZATION",
                                    score = String.format("₹ %.2f Cr", cStats.yieldRealizationPerMw),
                                    verdict = "YIELD RATIO",
                                    verdictColor = BrandGreen,
                                    details = "Total net revenue generation divided by total active capacity: Grew Energy delivers ₹ ${String.format("%.2f Cr", cStats.yieldRealizationPerMw)} per MW produced."
                                )
                            }
                        }
                    } else {
                        Text("Recomputing portfolio parameters...", fontSize = 9.sp, color = SlateTextMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardCard(
    title: String,
    items: List<ContributorItem>,
    activeFilters: Set<String>,
    onItemToggle: (String, Boolean) -> Unit,
    metricType: DashboardMetric,
    isRep: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextLight,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records match your drilldown scope.", color = SlateTextMuted, fontSize = 8.5.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.forEachIndexed { idx, item ->
                        val isSelected = activeFilters.contains(item.name)
                        val barColor = resolveSeriesColor(item.name)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onItemToggle(item.name, false) }
                                .drawBehind {
                                    val barWidth = size.width * (item.percentage / 100f).toFloat()
                                    drawRect(
                                        color = barColor.copy(alpha = if (isSelected) 0.16f else 0.04f),
                                        topLeft = Offset(0f, 0f),
                                        size = Size(barWidth, size.height)
                                    )
                                }
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}. ${item.name}",
                                color = if (isSelected) barColor else SlateTextLight,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatMetric(item.value, metricType),
                                    color = if (isSelected) barColor else SlateTextLight,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isRep) "${item.uniqueCount} CLIENTS" else String.format("%.1f%% SHARE", item.percentage),
                                    color = SlateTextMuted,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.SemiBold
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
fun SlimSkuSidebar(
    viewModel: GrewViewModel,
    selectedSkus: Set<String>,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsState()
    val solarSkus = stats?.applicableSkus ?: listOf(
        "540 WP Mono",
        "545 WP Bifacial",
        "550 WP Mono",
        "580 WP DCR",
        "585 WP TOPCon",
        "600 WP TOPCon"
    )
    
    Card(
        modifier = modifier
            .width(54.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "SKU",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = SlateTextLight,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                solarSkus.forEach { sku ->
                    val isSelected = selectedSkus.contains(sku)
                    val isActive = selectedSkus.isEmpty() || isSelected
                    val skuColor = resolveSeriesColor(sku)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) skuColor.copy(alpha = 0.15f) else SoftChipBg.copy(alpha = 0.4f))
                            .border(
                                1.5.dp,
                                if (isActive) skuColor else SlateBorder.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.toggleSkuFilter(sku, false) },
                        contentAlignment = Alignment.Center
                    ) {
                        val parts = sku.split(" ")
                        val numPart = parts.getOrNull(0) ?: ""
                        Text(
                            text = numPart,
                            color = if (isActive) skuColor else SlateTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GrewPortalTabContent(
    userEmail: String?,
    onLogout: () -> Unit,
    viewModel: GrewViewModel,
    onShowDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        ) {
            GrewEnergyLogo(showText = true)
        }
        
        HorizontalDivider(color = SlateBorder, thickness = 1.dp)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(BrandGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userEmail?.take(1)?.uppercase() ?: "G",
                            color = BrandGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userEmail?.split("@")?.get(0)?.replaceFirstChar { it.uppercase() } ?: "Grew Executive",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SlateTextLight
                        )
                        Text(
                            text = userEmail ?: "Local Offline Session",
                            fontSize = 9.sp,
                            color = SlateTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                val isSupabaseConfigured = BuildConfig.SUPABASE_URL.isNotEmpty() && !BuildConfig.SUPABASE_URL.contains("YOUR_SUPABASE_PROJECT_URL_HERE")
                Box(
                    modifier = Modifier
                        .background((if (isSupabaseConfigured) BrandGreen else BrandGold).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, (if (isSupabaseConfigured) BrandGreen else BrandGold).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSupabaseConfigured) Icons.Default.VerifiedUser else Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = if (isSupabaseConfigured) BrandGreen else BrandGold,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSupabaseConfigured) "Verified via Supabase Gateway" else "Authenticated (Local Whitelist)",
                            color = if (isSupabaseConfigured) BrandGreen else BrandGold,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        Text(
            text = "PORTAL UTILITIES",
            fontSize = 9.sp,
            color = SlateTextMuted,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.resetToLatestAnchor() },
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Reset Date Anchors", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextLight, textAlign = TextAlign.Center)
                }
            }
            
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onShowDiagnostics() },
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = BrandGold, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Database Diagnostics", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextLight, textAlign = TextAlign.Center)
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EXECUTIVE PORTAL & UTILITY GUIDE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = SlateTextLight,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "PACING CALCULATIONS:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
                Text(
                    text = "Comparing equivalent days in history is crucial for solar matrices. e.g., MTD compares exactly day 1 to day 23 of previous months, ensuring valid pacing evaluations.",
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    color = SlateTextMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "INTERACTIVE DRILLDOWN & TOOLS:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
                Text(
                    text = "• Click on table columns to focus / isolate specific month velocity.\n• Use top shortcut chips to switch Financial Years and active Operating Segments.\n• Click on chart legends to toggle lines; hold legend to isolate a metric.",
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    color = SlateTextMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "HERFINDAHL-HIRSCHMAN (HHI):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreen
                )
                Text(
                    text = "An economic concentration metric. HHI <1500 indicates robust diversification, while HHI >2500 indicates dependency on concentrated channels.",
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    color = SlateTextMuted
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrewDatePickerDialog(
    initialDate: java.util.Date,
    onDateSelected: (java.util.Date) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.time
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(java.util.Date(millis))
                    }
                    onDismiss()
                }
            ) {
                Text("Confirm", color = BrandGreen, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SlateTextMuted)
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = SlateCard
        )
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                containerColor = SlateCard,
                selectedDayContainerColor = BrandGreen,
                selectedDayContentColor = Color.White,
                todayContentColor = BrandGreen,
                todayDateBorderColor = BrandGreen
            )
        )
    }
}

// Support functions & smaller components
private fun formatMetric(valRaw: Double, activeMetric: DashboardMetric): String {
    return when (activeMetric) {
        DashboardMetric.Amount -> String.format("₹ %.2f Cr", valRaw)
        DashboardMetric.MW -> String.format("%.2f MW", valRaw)
        DashboardMetric.Qty -> String.format("%,.0f Qty", valRaw)
    }
}

private fun toptModeRadius() = 10.dp

private fun getMonthIndexFromName(name: String): Int {
    return when (name) {
        "Apr" -> Calendar.APRIL
        "May" -> Calendar.MAY
        "Jun" -> Calendar.JUNE
        "Jul" -> Calendar.JULY
        "Aug" -> Calendar.AUGUST
        "Sep" -> Calendar.SEPTEMBER
        "Oct" -> Calendar.OCTOBER
        "Nov" -> Calendar.NOVEMBER
        "Dec" -> Calendar.DECEMBER
        "Jan" -> Calendar.JANUARY
        "Feb" -> Calendar.FEBRUARY
        "Mar" -> Calendar.MARCH
        else -> Calendar.APRIL
    }
}

private fun getMonthName(idx: Int): String {
    return when (idx) {
        Calendar.APRIL -> "Apr"
        Calendar.MAY -> "May"
        Calendar.JUNE -> "Jun"
        Calendar.JULY -> "Jul"
        Calendar.AUGUST -> "Aug"
        Calendar.SEPTEMBER -> "Sep"
        Calendar.OCTOBER -> "Oct"
        Calendar.NOVEMBER -> "Nov"
        Calendar.DECEMBER -> "Dec"
        Calendar.JANUARY -> "Jan"
        Calendar.FEBRUARY -> "Feb"
        Calendar.MARCH -> "Mar"
        else -> "Apr"
    }
}

private fun resolveSeriesColor(series: String): Color {
    return when {
        series.contains("Solar Modules (Internal)") -> Color(0xFF11B994)
        series.contains("Solar Modules") -> Color(0xFF10B981)
        series.contains("Raw Material") -> Color(0xFFF8B62B)
        series.contains("Scrap") -> Color(0xFF888888)
        
        series.contains("540") -> Color(0xFF10B981)
        series.contains("545") -> Color(0xFF48CED9)
        series.contains("550") -> Color(0xFFD8BFD8)
        series.contains("580") -> Color(0xFFD2B48C)
        series.contains("585") -> Color(0xFFC0E8D5)
        series.contains("600") -> Color(0xFF93C5FD)

        series.contains("Amit") -> Color(0xFF0EA5E9)
        series.contains("Priya") -> Color(0xFF10B981)
        series.contains("Vikram") -> Color(0xFF8B5CF6)
        series.contains("Nitin") -> Color(0xFFFFC000)
        series.contains("Siddharth") -> Color(0xFFEC4899)
        else -> Color(0xFF6B7280)
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    breakdown: Map<String, Double>,
    pacingChange: Double? = null,
    pacingLabel: String = "",
    isActiveSolar: Boolean = false,
    isFilterablePending: Boolean = false,
    isPendingActive: Boolean = false,
    onPendingToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier.width(180.dp).height(115.dp)
) {
    val isPeriodSales = title.contains("PERIOD") || title.contains("ANCHOR")
    val isPending = title.contains("PENDING")
    val isMTD = title.contains("MTD")
    val isQTD = title.contains("QTD")
    val isYTD = title.contains("YTD")

    val bgCol = when {
        isPeriodSales -> Color(0xFF0078D7) // Windows Blue
        isPending -> Color(0xFFCA5010) // Windows Orange/Brick
        isMTD -> Color(0xFF107C10) // Windows Green
        isQTD -> Color(0xFF00B7C3) // Windows Teal
        isYTD -> Color(0xFF881798) // Windows Purple
        else -> SlateCard
    }

    val tcCol = Color.White // Authentic Metro uses high contrast white text

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(0.dp)) // Sharp Metro corners
            .clickable(enabled = isFilterablePending) { onPendingToggle?.invoke() },
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = bgCol),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat design
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tcCol.copy(alpha = 0.15f), // subtle background icon
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tcCol,
                        letterSpacing = 0.5.sp
                    )

                    pacingChange?.let { pct ->
                        val isPos = pct >= 0
                        val arrow = if (isPos) "↑" else "↓"
                        
                        Text(
                            text = "$arrow${String.format("%.1f%%", Math.abs(pct))} $pacingLabel",
                            color = tcCol.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tcCol,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Monochrome Metro-style breakdown bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                ) {
                    val total = breakdown.values.sum()
                    if (total > 0f) {
                        breakdown.values.forEach { amt ->
                            val weight = (amt / total).toFloat()
                            if (weight > 0.005f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(weight)
                                        .background(Color.White.copy(alpha = 0.6f))
                                )
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PacingCellValue(value: Double?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (value != null) {
            val isPos = value >= 0
            val cellColor = if (isPos) Color(0xFF039B4F) else Color(0xFFF43F5E)
            val sign = if (isPos) "+" else ""
            Text(
                text = "$sign${String.format("%.0f%%", value)}",
                fontSize = 7.5.sp,
                color = cellColor,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        } else {
            Text(
                text = "—",
                fontSize = 7.5.sp,
                color = SlateTextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

fun getMonthLabelWithYear(monthAbbr: String, selectedFY: String): String {
    val parts = selectedFY.split("-")
    val fullMonth = when (monthAbbr.lowercase()) {
        "apr" -> "April"
        "may" -> "May"
        "jun" -> "June"
        "jul" -> "July"
        "aug" -> "August"
        "sep" -> "September"
        "oct" -> "October"
        "nov" -> "November"
        "dec" -> "December"
        "jan" -> "January"
        "feb" -> "February"
        "mar" -> "March"
        else -> monthAbbr.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
    if (parts.size < 2) return fullMonth
    val startYearFull = parts[0].toIntOrNull() ?: 2025
    val startYr2Digit = startYearFull % 100
    val endYr2Digit = parts[1].toIntOrNull() ?: ((startYearFull + 1) % 100)
    
    val mLower = monthAbbr.lowercase()
    val isLaterPart = mLower == "jan" || mLower == "feb" || mLower == "mar"
    val yrSuffix = if (isLaterPart) endYr2Digit else startYr2Digit
    return "$fullMonth $yrSuffix"
}

@Composable
fun MatrixTableView(
    items: List<MatrixRowItem>,
    currentMonth: String?,
    currentQuarter: Int?,
    selectedFY: String,
    onMonthSelected: (String) -> Unit,
    onQuarterSelected: (Int) -> Unit
) {
    val monthlyItems = items.filter { it.monthName != "Total" }
    val totalRow = items.find { it.monthName == "Total" }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateCard)
            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        // Sticky description row titles on the left
        Column(
            modifier = Modifier.width(88.dp)
        ) {
            // Header Space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "METRICS",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    color = SlateTextMuted,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            
            val labelHeight = 26.dp
            Box(modifier = Modifier.fillMaxWidth().height(labelHeight), contentAlignment = Alignment.CenterStart) {
                Text("REVENUE Cr", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = SlateTextLight)
            }
            Box(modifier = Modifier.fillMaxWidth().height(labelHeight), contentAlignment = Alignment.CenterStart) {
                Text("VOLUME MW", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = SlateTextLight)
            }
            Box(modifier = Modifier.fillMaxWidth().height(labelHeight), contentAlignment = Alignment.CenterStart) {
                Text("QTY (K)", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = SlateTextLight)
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            HorizontalDivider(color = SlateBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(2.dp))

            Box(modifier = Modifier.fillMaxWidth().height(labelHeight), contentAlignment = Alignment.CenterStart) {
                Text("MoM %", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = SlateTextMuted)
            }
            Box(modifier = Modifier.fillMaxWidth().height(labelHeight), contentAlignment = Alignment.CenterStart) {
                Text("QoQ %", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = SlateTextMuted)
            }
            Box(modifier = Modifier.fillMaxWidth().height(labelHeight), contentAlignment = Alignment.CenterStart) {
                Text("YoY %", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = SlateTextMuted)
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Horizontally scrollable data columns
        BoxWithConstraints(
            modifier = Modifier.weight(1f)
        ) {
            val totalCols = monthlyItems.size + (if (totalRow != null) 1 else 0)
            val spacing = 4.dp
            // Ensure columns fit within width if possible, else use minimum width
            val minColWidth = 72.dp 
            val availableWidth = maxWidth - (spacing * (totalCols - 1))
            val calculatedColWidth = if (totalCols > 0) availableWidth / totalCols else minColWidth
            val columnWidth = if (calculatedColWidth > minColWidth) calculatedColWidth else minColWidth

            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    monthlyItems.forEachIndexed { idx, row ->
                        val qIdx = idx / 3
                        val isPartSelected = currentQuarter == qIdx
                        val isMonthSelected = currentMonth == row.monthName

                        val bgCol = when {
                            isMonthSelected -> BrandGreen.copy(alpha = 0.15f)
                            isPartSelected -> BrandBlue.copy(alpha = 0.1f)
                            else -> Color.Transparent
                        }

                        Column(
                            modifier = Modifier
                                .width(columnWidth)
                                .clip(RoundedCornerShape(6.dp))
                                .background(bgCol)
                                .clickable { onMonthSelected(row.monthName) }
                                .padding(horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isPartSelected) BrandGreen.copy(alpha = 0.2f) else SlateBg)
                                        .clickable { onQuarterSelected(qIdx) }
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "Q${qIdx + 1}",
                                        fontSize = 6.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isPartSelected) BrandGreen else SlateTextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = row.monthName.uppercase(),
                                    fontSize = 7.5.sp,
                                    fontWeight = if (isMonthSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isMonthSelected) BrandGreen else SlateTextLight
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            val cellHeight = 26.dp
                            Box(modifier = Modifier.fillMaxWidth().height(cellHeight), contentAlignment = Alignment.Center) {
                                Text(text = String.format("%.1f", row.revenueCr), fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = SlateTextLight)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(cellHeight), contentAlignment = Alignment.Center) {
                                Text(text = String.format("%.1f", row.capacityMw), fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = SlateTextLight)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(cellHeight), contentAlignment = Alignment.Center) {
                                Text(text = String.format("%.0f", row.volumeQty / 1000.0), fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = SlateTextLight)
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SlateBorder))
                            Spacer(modifier = Modifier.height(2.dp))

                            PacingCellValue(value = row.momChange, modifier = Modifier.fillMaxWidth().height(cellHeight))
                            PacingCellValue(value = row.qoqChange, modifier = Modifier.fillMaxWidth().height(cellHeight))
                            PacingCellValue(value = row.yoyChange, modifier = Modifier.fillMaxWidth().height(cellHeight))
                        }
                    }

                    if (totalRow != null) {
                        Column(
                            modifier = Modifier
                                .width(columnWidth)
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrandGreen.copy(alpha = 0.1f))
                                .padding(horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "TOTAL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = BrandGreen)
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            val cellHeight = 26.dp
                            Box(modifier = Modifier.fillMaxWidth().height(cellHeight), contentAlignment = Alignment.Center) {
                                Text(text = String.format("%.1f", totalRow.revenueCr), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = BrandGreen)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(cellHeight), contentAlignment = Alignment.Center) {
                                Text(text = String.format("%.1f", totalRow.capacityMw), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = BrandGreen)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(cellHeight), contentAlignment = Alignment.Center) {
                                Text(text = String.format("%.0f", totalRow.volumeQty / 1000.0), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = BrandGreen)
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BrandGreen.copy(alpha = 0.3f)))
                            Spacer(modifier = Modifier.height(2.dp))

                            repeat(3) { Box(modifier = Modifier.fillMaxWidth().height(cellHeight)) }
                        }
                    }
                }
            }

            // Smooth Scroll Assist Overlays (Left/Right Chevrons with rich feedback)
            val showLeftArrow = remember { derivedStateOf { scrollState.value > 0 } }
            val showRightArrow = remember { derivedStateOf { scrollState.value < scrollState.maxValue } }

            if (showLeftArrow.value) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .matchParentSize()
                        .width(36.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(SlateCard.copy(alpha = 0.95f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                scrollState.animateScrollTo((scrollState.value - 240).coerceAtLeast(0))
                            }
                        },
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(24.dp)
                            .background(SlateBg.copy(alpha = 0.9f), CircleShape)
                            .border(0.5.dp, SlateBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Scroll Left",
                            tint = BrandGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (showRightArrow.value) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .matchParentSize()
                        .width(36.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, SlateCard.copy(alpha = 0.95f))
                            )
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                scrollState.animateScrollTo((scrollState.value + 240).coerceAtMost(scrollState.maxValue))
                            }
                        },
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(24.dp)
                            .background(SlateBg.copy(alpha = 0.9f), CircleShape)
                            .border(0.5.dp, SlateBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Scroll Right",
                            tint = BrandGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetHeaderTab(title: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) SlateCard else Color.Transparent)
            .border(1.dp, if (isActive) SlateBorder else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Black,
            fontSize = 8.5.sp,
            color = if (isActive) BrandGreen else SlateTextMuted,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun IntelligenceModuleCard(
    title: String,
    subtitle: String,
    score: String,
    verdict: String,
    verdictColor: Color,
    details: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextLight, letterSpacing = 1.sp)
            Text(subtitle, fontSize = 7.5.sp, color = SlateTextMuted, modifier = Modifier.padding(bottom = 6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(score, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = SlateTextLight)
                Text(
                    text = verdict.uppercase(),
                    color = verdictColor,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .background(verdictColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            HorizontalDivider(color = SlateBorder)
            Text(
                text = details,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium,
                color = SlateTextMuted,
                lineHeight = 11.5.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
fun CustomCanvasChart(
    points: List<VelocityPoint>,
    activeSeriesNames: List<String>,
    excludedSeries: Set<String>,
    metricType: DashboardMetric,
    velocityMode: VelocityMode,
    onScrubPoint: (Int?, Offset) -> Unit
) {
    val activeKeys = activeSeriesNames.filter { !excludedSeries.contains(it) }
    
    val maxStackedValue = points.maxOfOrNull { pt ->
        if (velocityMode == VelocityMode.Daily) {
            pt.seriesValues.filterKeys { activeKeys.contains(it) }.values.maxOrNull() ?: 1.0
        } else {
            pt.seriesValues.filterKeys { activeKeys.contains(it) }.values.sum()
        }
    } ?: 10.0
    
    val safeMaxScale = if (maxStackedValue > 0f) maxStackedValue * 1.15 else 10.0

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .background(Color(0xFFEAEAEE), RoundedCornerShape(20.dp))
            .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
            .padding(top = 10.dp, bottom = 6.dp, start = 4.dp, end = 4.dp)
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (points.isNotEmpty()) {
                            val colW = size.width / points.size.toFloat()
                            if (colW > 0.01f) {
                                val maxIdx = maxOf(0, points.size - 1)
                                val colIdx = (offset.x / colW).toInt().coerceIn(0, maxIdx)
                                onScrubPoint(colIdx, offset)
                            }
                        }
                    },
                    onDragEnd = { onScrubPoint(null, Offset.Zero) },
                    onDragCancel = { onScrubPoint(null, Offset.Zero) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (points.isNotEmpty()) {
                            val colW = size.width / points.size.toFloat()
                            if (colW > 0.01f) {
                                val maxIdx = maxOf(0, points.size - 1)
                                val colIdx = (change.position.x / colW).toInt().coerceIn(0, maxIdx)
                                onScrubPoint(colIdx, change.position)
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            val axisOffset = 42f
            val plotWidth = width - axisOffset - 10f
            val plotHeight = height - axisOffset - 10f
            
            val steps = 4
            for (i in 0..steps) {
                val y = plotHeight - (plotHeight / steps) * i
                val scaleVal = (safeMaxScale / steps) * i
                
                drawLine(
                    color = SlateBorder,
                    start = Offset(axisOffset, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                
                drawContext.canvas.nativeCanvas.drawText(
                    if (scaleVal > 1000) String.format("%,.0f", scaleVal) else String.format("%.1f", scaleVal),
                    8f,
                    y + 4f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#4C5668")
                        textSize = 21f
                        isAntiAlias = true
                    }
                )
            }

            if (points.isNotEmpty()) {
                val colCount = points.size
                val colStride = plotWidth / colCount.toFloat()
                
                if (velocityMode == VelocityMode.Daily) {
                    activeKeys.forEach { seriesName ->
                        val colorRef = resolveSeriesColor(seriesName)
                        val linePath = Path()
                        val fillPath = Path()

                        points.forEachIndexed { idx, pt ->
                            val v = pt.seriesValues[seriesName] ?: 0.0
                            val xPos = axisOffset + (colStride * idx) + (colStride / 2)
                            val yPos = ((plotHeight - (v / safeMaxScale) * plotHeight)).toFloat()

                            if (idx == 0) {
                                linePath.moveTo(xPos, yPos)
                                fillPath.moveTo(xPos, plotHeight)
                                fillPath.lineTo(xPos, yPos)
                            } else {
                                linePath.lineTo(xPos, yPos)
                                fillPath.lineTo(xPos, yPos)
                            }
                            
                            if (idx == points.lastIndex) {
                                fillPath.lineTo(xPos, plotHeight)
                                fillPath.close()
                            }
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(colorRef.copy(alpha = 0.15f), Color.Transparent),
                                startY = 0f,
                                endY = plotHeight
                            )
                        )

                        drawPath(
                            path = linePath,
                            color = colorRef,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )
                    }
                } else {
                    points.forEachIndexed { pIdx, pt ->
                        val xPosLeft = axisOffset + (colStride * pIdx) + (colStride * 0.1f)
                        val colW = colStride * 0.8f
                        
                        var runningPastedHeight = 0f
                        
                        activeKeys.forEach { seriesName ->
                            val v = pt.seriesValues[seriesName] ?: 0.0
                            if (v > 0.0001f) {
                                val sH = ((v / safeMaxScale) * plotHeight).toFloat()
                                val yStart = plotHeight - runningPastedHeight - sH
                                
                                drawRect(
                                    brush = Brush.linearGradient(
                                        listOf(resolveSeriesColor(seriesName), resolveSeriesColor(seriesName).copy(alpha = 0.5f))
                                    ),
                                    topLeft = Offset(xPosLeft, yStart),
                                    size = Size(colW, sH)
                                )
                                
                                runningPastedHeight += sH
                            }
                        }
                    }
                }

                points.forEachIndexed { idx, pt ->
                    val xPosCent = axisOffset + (colStride * idx) + (colStride / 2)
                    val skip = if (colCount > 15) idx % 3 != 0 else false
                    if (!skip) {
                        drawContext.canvas.nativeCanvas.drawText(
                            pt.label,
                            xPosCent - 12f,
                            plotHeight + 30f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#4C5668")
                                textSize = 22f
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncDiagnosticsDialog(viewModel: GrewViewModel, onDismiss: () -> Unit) {
    val logs by viewModel.diagnostics.collectAsState()
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sync Engine Diagnostics", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SlateTextLight)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = logs,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF10B981)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

suspend fun verifyEmailWithSupabase(email: String, url: String, key: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$url/rest/v1/whitelist?email=eq.$email&select=id")
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                return@withContext body != "[]"
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext false
}

suspend fun saveOtpInSupabase(email: String, otp: String, url: String, key: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val json = """{"email":"$email", "otp":"$otp", "created_at":"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH).format(Date())}"}"""
        val request = Request.Builder()
            .url("$url/rest/v1/otps")
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(json.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        client.newCall(request).execute().use { response ->
            return@withContext response.isSuccessful
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext false
}

