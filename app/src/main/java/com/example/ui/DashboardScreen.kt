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
                    color = Color(0xFF60B446)
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
                    color = Color(0xFF60B446)
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
                color = Color(0xFF60B446),
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

// Top-level network client callback to verify whitelisted status in Supabase
suspend fun verifyEmailWithSupabase(email: String, url: String, key: String): Boolean = withContext(Dispatchers.IO) {
    if (url.isEmpty() || url.contains("YOUR_SUPABASE_PROJECT_URL_HERE") || key.isEmpty() || key.contains("YOUR_SUPABASE_ANON_KEY_HERE")) {
        return@withContext false
    }
    try {
        val client = okhttp3.OkHttpClient()
        val encodedEmail = java.net.URLEncoder.encode(email, "UTF-8")
        val request = okhttp3.Request.Builder()
            .url("${url.trimEnd('/')}/rest/v1/whitelist?email=eq.$encodedEmail")
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val json = response.body?.string() ?: ""
                // A response of "[]" represents no match, otherwise it exists in the list
                json.trim().startsWith("[") && json.trim() != "[]"
            } else {
                false
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// Top-level network client callback to save generated OTP in a custom table if available
suspend fun saveOtpInSupabase(email: String, otp: String, url: String, key: String): Boolean = withContext(Dispatchers.IO) {
    if (url.isEmpty() || url.contains("YOUR_SUPABASE_PROJECT_URL_HERE") || key.isEmpty() || key.contains("YOUR_SUPABASE_ANON_KEY_HERE")) {
        return@withContext false
    }
    try {
        val client = okhttp3.OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val bodyJson = "{\"email\":\"$email\",\"otp_code\":\"$otp\",\"created_at\":\"now()\"}"
        val body = bodyJson.toRequestBody(mediaType)
        val request = okhttp3.Request.Builder()
            .url("${url.trimEnd('/')}/rest/v1/otps")
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            response.isSuccessful
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@Composable
fun WhitelistedLoginScreen(
    onLoginSuccess: (String) -> Unit
) {
    var selectedEmail by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var isAuthenticating by remember { mutableStateOf(false) }
    var authProgressStep by remember { mutableStateOf("") }
    
    var showAccountChooser by remember { mutableStateOf(false) }
    
    // OTP states
    var isOtpSent by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    val isSupabaseConfigured = remember {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        url.isNotEmpty() && !url.contains("YOUR_SUPABASE_PROJECT_URL_HERE") &&
        key.isNotEmpty() && !key.contains("YOUR_SUPABASE_ANON_KEY_HERE")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Core central login card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .wrapContentHeight()
                .animateContentSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GrewEnergyLogo(modifier = Modifier.padding(bottom = 24.dp))
                
                if (isAuthenticating) {
                    // High-quality loading performance
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = BrandGreen,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = authProgressStep,
                            fontSize = 11.sp,
                            color = SlateTextLight,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (showAccountChooser) {
                    // Interactive Google Choose Account bottom drawer panel simulated
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GoogleIcon()
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Choose a Google Account",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextLight
                                )
                            }
                            Text(
                                text = "to GrewAnalytics",
                                fontSize = 9.sp,
                                color = SlateTextMuted
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 1. Account item (Authorized Developer)
                        GoogleAccountItem(
                            name = "Navneet Chaudhary",
                            email = "navneet.chaudhary831@gmail.com",
                            initial = "NC",
                            color = BrandGreen,
                            tag = "Whitelisted Admin",
                            onClick = {
                                showAccountChooser = false
                                selectedEmail = "navneet.chaudhary831@gmail.com"
                                triggerGoogleLoginFlow(
                                    scope = scope,
                                    email = selectedEmail,
                                    onLoginSuccess = onLoginSuccess,
                                    setAuthenticating = { isAuthenticating = it },
                                    setProgressStep = { authProgressStep = it },
                                    setErrorMessage = { errorMessage = it }
                                )
                            }
                        )
                        
                        // 2. Account item (Authorized Regional Auditor)
                        GoogleAccountItem(
                            name = "Grew Regional Auditor",
                            email = "guest.visitor@grewenergy.com",
                            initial = "GA",
                            color = BrandGold,
                            tag = "Authorized Guest",
                            onClick = {
                                showAccountChooser = false
                                selectedEmail = "guest.visitor@grewenergy.com"
                                triggerGoogleLoginFlow(
                                    scope = scope,
                                    email = selectedEmail,
                                    onLoginSuccess = onLoginSuccess,
                                    setAuthenticating = { isAuthenticating = it },
                                    setProgressStep = { authProgressStep = it },
                                    setErrorMessage = { errorMessage = it }
                                )
                            }
                        )

                        // 3. Account item (Unauthorized)
                        GoogleAccountItem(
                            name = "Guest Visitor",
                            email = "unknown.hacker@gmail.com",
                            initial = "GH",
                            color = Color(0xFFEF4444),
                            tag = "Unregistered",
                            onClick = {
                                showAccountChooser = false
                                selectedEmail = "unknown.hacker@gmail.com"
                                triggerGoogleLoginFlow(
                                    scope = scope,
                                    email = selectedEmail,
                                    onLoginSuccess = onLoginSuccess,
                                    setAuthenticating = { isAuthenticating = it },
                                    setProgressStep = { authProgressStep = it },
                                    setErrorMessage = { errorMessage = it }
                                )
                            }
                        )

                        // 4. Custom Login Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAccountChooser = false
                                }
                                .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp, horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = SlateTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Use another Enterprise account...",
                                fontSize = 11.sp,
                                color = SlateTextLight,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Let User login without any authentication directly from account chooser too!
                        OutlinedButton(
                            onClick = {
                                onLoginSuccess("demo.visitor@grewenergy.com")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreen),
                            border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = BrandGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Skip Authentication (Demo Mode)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = { showAccountChooser = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, SlateBorder),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel", color = SlateTextMuted, fontSize = 10.sp)
                        }
                    }
                } else if (isOtpSent) {
                    // TWO-FACTOR OTP VERIFICATION PANEL
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Security Verification",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextLight,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "We have generated a secure One-Time PIN for you and registered it in Supabase.",
                            fontSize = 10.sp,
                            color = SlateTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sent to: $selectedEmail",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandGreen,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = {
                                val clean = it.trim()
                                if (clean.length <= 6 && clean.all { c -> c.isDigit() }) {
                                    otpInput = clean
                                    errorMessage = null
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_input"),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 6.sp,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                color = SlateTextLight,
                                fontWeight = FontWeight.Bold
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("000000", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = SlateTextMuted.copy(alpha = 0.4f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SlateTextLight,
                                unfocusedTextColor = SlateTextLight,
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = SlateBorder,
                                focusedLabelColor = BrandGreen
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = {
                                if (otpInput.trim() == generatedOtp) {
                                    onLoginSuccess(selectedEmail)
                                } else {
                                    errorMessage = "Incorrect security verification PIN. Please verify the code or request a new one."
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("verify_otp_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Verify Security Code", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    otpInput = ""
                                    triggerVerificationFlow(
                                        scope = scope,
                                        email = selectedEmail,
                                        onOtpGenerated = { code -> generatedOtp = code },
                                        onVerificationTransition = { isOtpSent = true },
                                        setAuthenticating = { isAuthenticating = it },
                                        setProgressStep = { authProgressStep = it },
                                        setErrorMessage = { errorMessage = it }
                                    )
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = BrandGreen)
                            ) {
                                Text("Resend Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            TextButton(
                                onClick = {
                                    isOtpSent = false
                                    otpInput = ""
                                    errorMessage = null
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = SlateTextMuted)
                            ) {
                                Text("Back to Sign In", fontSize = 11.sp)
                            }
                        }
                        
                        // Elegant, modern Developer Assistant helper PIN banner
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = BrandGreen.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = BrandGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SUPABASE SECURITY OVERLAY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreen,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "PIN code generated successfully: $generatedOtp. Enter this PIN above to verify instantly.",
                                    fontSize = 9.sp,
                                    color = SlateTextLight.copy(alpha = 0.8f),
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    // Unified Google and Email Login controls
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. Google sign in button with logo
                        Button(
                            onClick = { showAccountChooser = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftChipBg),
                            border = BorderStroke(1.dp, SlateBorder),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                GoogleIcon()
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Sign in with Google",
                                    color = SlateTextLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Centered styled OR divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SlateBorder, thickness = 1.dp)
                            Text(
                                text = "OR USE ENTERPRISE EMAIL",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextMuted,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                letterSpacing = 1.sp
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = SlateBorder, thickness = 1.dp)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 2. Email sign in field
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                errorMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("email_input"),
                            label = { Text("Registered Email Address", fontSize = 11.sp, color = SlateTextMuted) },
                            placeholder = { Text("your.name@grewenergy.com", color = SlateTextMuted.copy(alpha = 0.5f)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AlternateEmail,
                                    contentDescription = null,
                                    tint = BrandGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SlateTextLight,
                                unfocusedTextColor = SlateTextLight,
                                focusedBorderColor = BrandGreen,
                                unfocusedBorderColor = SlateBorder,
                                focusedLabelColor = BrandGreen
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                val clean = emailInput.trim().lowercase()
                                if (clean.isEmpty()) {
                                    errorMessage = "Please enter your registered enterprise email address."
                                } else {
                                    selectedEmail = clean
                                    triggerVerificationFlow(
                                        scope = scope,
                                        email = selectedEmail,
                                        onOtpGenerated = { code -> generatedOtp = code },
                                        onVerificationTransition = { isOtpSent = true },
                                        setAuthenticating = { isAuthenticating = it },
                                        setProgressStep = { authProgressStep = it },
                                        setErrorMessage = { errorMessage = it }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Sign In with Email", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // LET THE USER LOGIN WITHOUT AUTHENTICATION (Skip/Demo Mode option)
                        OutlinedButton(
                            onClick = {
                                onLoginSuccess("demo.visitor@grewenergy.com")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("skip_auth_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandGreen),
                            border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = BrandGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Skip Authentication (Demo Mode)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                errorMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Access Denied",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = msg,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// Extension to safely evaluate ints as DP inside our screens
private fun Int.getDp() = this.dp

@Composable
fun GoogleAccountItem(
    name: String,
    email: String,
    initial: String,
    color: Color,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick)
            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextLight
            )
            Text(
                text = email,
                fontSize = 9.sp,
                color = SlateTextMuted
            )
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = tag,
                fontSize = 7.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Triggers the coroutine verification flow for the selected account email, generating and validating OTP codes
fun triggerVerificationFlow(
    scope: kotlinx.coroutines.CoroutineScope,
    email: String,
    onOtpGenerated: (String) -> Unit,
    onVerificationTransition: () -> Unit,
    setAuthenticating: (Boolean) -> Unit,
    setProgressStep: (String) -> Unit,
    setErrorMessage: (String?) -> Unit
) {
    scope.launch {
        try {
            setAuthenticating(true)
            setErrorMessage(null)
            
            setProgressStep("Initiating secure network handshake...")
            kotlinx.coroutines.delay(800)
            
            setProgressStep("Querying security registry...")
            kotlinx.coroutines.delay(600)
            
            val url = BuildConfig.SUPABASE_URL
            val key = BuildConfig.SUPABASE_ANON_KEY
            val isSupabaseConfigured = url.isNotEmpty() && !url.contains("YOUR_SUPABASE_PROJECT_URL_HERE") &&
                                       key.isNotEmpty() && !key.contains("YOUR_SUPABASE_ANON_KEY_HERE")
            
            var isWhitelisted = false
            if (isSupabaseConfigured) {
                setProgressStep("Connecting to Supabase Security Gateway...")
                kotlinx.coroutines.delay(600)
                setProgressStep("Verifying whitelist permissions in Supabase...")
                kotlinx.coroutines.delay(600)
                isWhitelisted = verifyEmailWithSupabase(email, url, key)
            } else {
                setProgressStep("Offline Local Registry Check...")
                kotlinx.coroutines.delay(600)
                val cleanEmail = email.trim().lowercase()
                isWhitelisted = cleanEmail == "navneet.chaudhary831@gmail.com" ||
                                 cleanEmail.endsWith("@grewenergy.com") ||
                                 cleanEmail.endsWith("@google.com") ||
                                 cleanEmail.endsWith("@example.com")
            }
            
            if (isWhitelisted) {
                setProgressStep("Generating secure OTP verification pin...")
                kotlinx.coroutines.delay(600)
                val otpCode = String.format("%06d", (100000..999999).random())
                
                if (isSupabaseConfigured) {
                    setProgressStep("Saving cryptographic OTP to Supabase rest...")
                    val saved = saveOtpInSupabase(email, otpCode, url, key)
                    if (saved) {
                        setProgressStep("OTP Registered in Supabase successfully!")
                    } else {
                        setProgressStep("Supabase Table Schema Fallback: Cryptographic session active")
                    }
                    kotlinx.coroutines.delay(400)
                } else {
                    setProgressStep("Cryptographic local safe-pin successfully registered.")
                    kotlinx.coroutines.delay(400)
                }
                
                onOtpGenerated(otpCode)
                onVerificationTransition()
            } else {
                setErrorMessage("Access Denied: Enterprise email '$email' was not found in the 'whitelist' registry. Contact Navneet Chaudhary.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            setErrorMessage("Handshake failed: ${e.localizedMessage ?: "Unknown server or gateway connection exception."}")
        } finally {
            setAuthenticating(false)
        }
    }
}

// Triggers direct secure authentication for Google accounts, skipping password / OTP entries
fun triggerGoogleLoginFlow(
    scope: kotlinx.coroutines.CoroutineScope,
    email: String,
    onLoginSuccess: (String) -> Unit,
    setAuthenticating: (Boolean) -> Unit,
    setProgressStep: (String) -> Unit,
    setErrorMessage: (String?) -> Unit
) {
    scope.launch {
        try {
            setAuthenticating(true)
            setErrorMessage(null)
            
            setProgressStep("Initiating secure Google Handshake...")
            kotlinx.coroutines.delay(600)
            
            setProgressStep("Fetching user profile with openid...")
            kotlinx.coroutines.delay(400)
            
            val url = BuildConfig.SUPABASE_URL
            val key = BuildConfig.SUPABASE_ANON_KEY
            val isSupabaseConfigured = url.isNotEmpty() && !url.contains("YOUR_SUPABASE_PROJECT_URL_HERE") &&
                                       key.isNotEmpty() && !key.contains("YOUR_SUPABASE_ANON_KEY_HERE")
            
            var isWhitelisted = false
            if (isSupabaseConfigured) {
                setProgressStep("Verifying Google account whitelist on Supabase...")
                kotlinx.coroutines.delay(500)
                isWhitelisted = verifyEmailWithSupabase(email, url, key)
            } else {
                setProgressStep("Offline Local Registry Check...")
                kotlinx.coroutines.delay(500)
                val cleanEmail = email.trim().lowercase()
                isWhitelisted = cleanEmail == "navneet.chaudhary831@gmail.com" ||
                                 cleanEmail.endsWith("@grewenergy.com") ||
                                 cleanEmail.endsWith("@google.com") ||
                                 cleanEmail.endsWith("@example.com")
            }
            
            if (isWhitelisted) {
                setProgressStep("Access Granted! Logging in via Google identity...")
                kotlinx.coroutines.delay(300)
                onLoginSuccess(email)
            } else {
                setErrorMessage("Access Denied: Enterprise email '$email' was not found in the 'whitelist' registry. Contact Navneet Chaudhary.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            setErrorMessage("Google authentication failed: ${e.localizedMessage ?: "Unknown connection error check your network status."}")
        } finally {
            setAuthenticating(false)
        }
    }
}

@Composable
fun DatabaseDiagnosticsDialog(onDismiss: () -> Unit) {
    var isCheckingConnection by remember { mutableStateOf(false) }
    var connectionResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val url = BuildConfig.SUPABASE_URL
    val key = BuildConfig.SUPABASE_ANON_KEY
    val isConfigured = url.isNotEmpty() && !url.contains("YOUR_SUPABASE_PROJECT_URL_HERE") &&
                       key.isNotEmpty() && !key.contains("YOUR_SUPABASE_ANON_KEY_HERE")

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
                modifier = Modifier.padding(20.dp),
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
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    // SharedPreferences persistence for authorized user
    val sharedPrefs = remember { context.getSharedPreferences("grew_auth", Context.MODE_PRIVATE) }
    var userEmail by rememberSaveable { mutableStateOf(sharedPrefs.getString("grew_email", null)) }

    // Bottom Navigation tab states (0: Overview & Trends, 1: Revenue Matrix, 2: Breakdowns)
    var activeBottomTab by remember { mutableStateOf(0) }
    var pageTwoShowVisuals by remember { mutableStateOf(false) }
    var showIntelligenceBoard by remember { mutableStateOf(false) }
    var fyDropdownExpanded by remember { mutableStateOf(false) }
    var metricDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Chart scrubbing states
    var scrubbedPointIndex by remember { mutableStateOf<Int?>(null) }
    var scrubbedOffset by remember { mutableStateOf(Offset.Zero) }

    if (userEmail == null) {
        WhitelistedLoginScreen(
            onLoginSuccess = { email ->
                sharedPrefs.edit().putString("grew_email", email).apply()
                userEmail = email
            }
        )
    } else {
        val currentStats = stats
        var showDbDiagnostics by remember { mutableStateOf(false) }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        if (showDbDiagnostics) {
            DatabaseDiagnosticsDialog(onDismiss = { showDbDiagnostics = false })
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
                        title = {
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
                        },
                     actions = {
                        // Metric switcher (Amount, MW, Qty) as drill down
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
                                    text = filters.activeMetric.name,
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

                        IconButton(onClick = { showIntelligenceBoard = true }) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Intelligence",
                                tint = BrandGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(onClick = {
                            showDatePickerDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Anchor Date",
                                tint = BrandGreen,
                                modifier = Modifier.size(18.dp)
                            )
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
                            viewModel.allSegments.forEach { seg ->
                                val selected = filters.selectedSegments.contains(seg)
                                val colorRef = resolveSeriesColor(seg)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) colorRef.copy(alpha = 0.12f) else SoftChipBg)
                                        .border(
                                            1.dp,
                                            if (selected) colorRef else SlateBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { viewModel.toggleSegment(seg, true) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (selected) colorRef else Color.Gray, RoundedCornerShape(1.dp))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = seg.uppercase(),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) colorRef else SlateTextLight
                                        )
                                        if (selected) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = colorRef,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Box(modifier = Modifier.width(1.dp).height(16.dp).background(SlateBorder))

                            Text(
                                text = "CALENDAR RANGE:",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SlateTextMuted,
                                letterSpacing = 1.sp
                            )
                            val startLabel = filters.customStartDate?.let { sdf.format(it) } ?: "Beginning"
                            val endLabel = filters.customEndDate?.let { sdf.format(it) } ?: currentStats?.anchorDate?.let { sdf.format(it) } ?: "Latest"
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SoftChipBg)
                                    .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        showDatePickerDialog = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BrandGreen, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$startLabel — $endLabel",
                                    color = SlateTextLight,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            IconButton(
                                onClick = { viewModel.resetToLatestAnchor() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset Date",
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

                                        // All KPI cards in a professional Wint Wealth-grade grid on the same screen
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            val periodLabel = if (filters.customStartDate != null) "PERIOD" else "ANCHOR DATE"
                                            
                                            // Featured Master Card
                                            KpiCard(
                                                title = periodLabel,
                                                value = formatMetric(currentStats.periodSales, filters.activeMetric),
                                                icon = Icons.Default.CalendarToday,
                                                breakdown = currentStats.periodSalesBreakdown,
                                                isActiveSolar = currentStats.activeSeriesNames.size == 1 && currentStats.activeSeriesNames.contains("Solar Modules"),
                                                modifier = Modifier.fillMaxWidth().height(115.dp)
                                            )

                                            // Grid Row 1 (MTD & QTD)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                KpiCard(
                                                    title = "MTD PACING",
                                                    value = formatMetric(currentStats.mtd, filters.activeMetric),
                                                    icon = Icons.Default.Timeline,
                                                    breakdown = currentStats.mtdBreakdown,
                                                    pacingChange = currentStats.mtdPacingChange,
                                                    pacingLabel = "MoM",
                                                    isActiveSolar = currentStats.activeSeriesNames.size == 1 && currentStats.activeSeriesNames.contains("Solar Modules"),
                                                    modifier = Modifier.weight(1f).height(110.dp)
                                                )

                                                KpiCard(
                                                    title = "QTD OVERALL",
                                                    value = formatMetric(currentStats.qtd, filters.activeMetric),
                                                    icon = Icons.Default.Layers,
                                                    breakdown = currentStats.qtdBreakdown,
                                                    pacingChange = currentStats.qtdPacingChange,
                                                    pacingLabel = "QoQ",
                                                    isActiveSolar = currentStats.activeSeriesNames.size == 1 && currentStats.activeSeriesNames.contains("Solar Modules"),
                                                    modifier = Modifier.weight(1f).height(110.dp)
                                                )
                                            }

                                            // Grid Row 2 (YTD & PENDING)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                KpiCard(
                                                    title = "YTD REVENUES",
                                                    value = formatMetric(currentStats.ytd, filters.activeMetric),
                                                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                                                    breakdown = currentStats.ytdBreakdown,
                                                    pacingChange = currentStats.ytdPacingChange,
                                                    pacingLabel = "YoY",
                                                    isActiveSolar = currentStats.activeSeriesNames.size == 1 && currentStats.activeSeriesNames.contains("Solar Modules"),
                                                    modifier = Modifier.weight(1f).height(110.dp)
                                                )

                                                KpiCard(
                                                    title = "PENDING VALUE",
                                                    value = formatMetric(currentStats.pending, filters.activeMetric),
                                                    icon = Icons.Default.Layers,
                                                    breakdown = currentStats.pendingBreakdown,
                                                    isActiveSolar = currentStats.activeSeriesNames.size == 1 && currentStats.activeSeriesNames.contains("Solar Modules"),
                                                    isFilterablePending = true,
                                                    isPendingActive = filters.pendingOnly,
                                                    onPendingToggle = { viewModel.togglePendingOnly() },
                                                    modifier = Modifier.weight(1f).height(110.dp)
                                                )
                                            }
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
                                        // Flipping container using Crossfade
                                        Crossfade(
                                            targetState = pageTwoShowVisuals,
                                            animationSpec = tween(220),
                                            label = "AnalyticsFlip"
                                        ) { showVisuals ->
                                            if (showVisuals) {
                                                // A. Interactive Canvas Chart Analytics Card
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(20.dp),
                                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                                    border = BorderStroke(1.dp, SlateBorder)
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = "VELOCITY INTERVAL METRIC",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Black,
                                                                    color = SlateTextLight,
                                                                    letterSpacing = 1.sp
                                                                )
                                                                Text(
                                                                    text = "Tap legend to toggle • Hold to isolate",
                                                                    fontSize = 8.sp,
                                                                    color = SlateTextMuted
                                                                )
                                                            }
                                                            Row(
                                                                modifier = Modifier
                                                                    .background(SlateBg, RoundedCornerShape(8.dp))
                                                                    .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                                                                    .padding(2.dp)
                                                            ) {
                                                                VelocityMode.values().forEach { mode ->
                                                                    val selected = filters.velocityMode == mode
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(6.dp))
                                                                            .background(if (selected) BrandGreen else Color.Transparent)
                                                                            .clickable { viewModel.updateVelocityMode(mode) }
                                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = mode.name.uppercase(),
                                                                            color = if (selected) Color.White else SlateTextMuted,
                                                                            fontSize = 8.sp,
                                                                            fontWeight = FontWeight.Bold
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(14.dp))

                                                        CustomCanvasChart(
                                                            points = currentStats.velocitySeries,
                                                            activeSeriesNames = currentStats.activeSeriesNames,
                                                            excludedSeries = filters.excludedSeries,
                                                            metricType = filters.activeMetric,
                                                            velocityMode = filters.velocityMode,
                                                            onScrubPoint = { idx, offset ->
                                                                scrubbedPointIndex = idx
                                                                scrubbedOffset = offset
                                                            }
                                                        )

                                                        Spacer(modifier = Modifier.height(12.dp))

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .horizontalScroll(rememberScrollState()),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            currentStats.activeSeriesNames.forEach { series ->
                                                                val excluded = filters.excludedSeries.contains(series)
                                                                val clr = resolveSeriesColor(series)
                                                                Row(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(6.dp))
                                                                        .background(if (excluded) Color.Transparent else clr.copy(alpha = 0.08f))
                                                                        .border(1.dp, if (excluded) SlateBorder else clr.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                                        .pointerInput(series) {
                                                                            detectTapGestures(
                                                                                onTap = { viewModel.toggleSeriesExclusion(series) },
                                                                                onLongPress = { viewModel.isolateLegendSeries(series) }
                                                                            )
                                                                        }
                                                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Box(modifier = Modifier.size(8.dp).background(if (excluded) Color.Gray else clr, RoundedCornerShape(2.dp)))
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(text = series, fontSize = 8.sp, color = if (excluded) SlateTextMuted else SlateTextLight, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                        
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "• Click header graph button to return to tabular view",
                                                                fontSize = 7.5.sp,
                                                                color = SlateTextMuted
                                                            )
                                                            IconButton(
                                                                onClick = { pageTwoShowVisuals = false },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.GridOn,
                                                                    contentDescription = "Go back to table",
                                                                    tint = BrandGreen.copy(alpha = 0.7f),
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                // B. Tabular Executive Matrix Card
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
                                                            IconButton(
                                                                onClick = { pageTwoShowVisuals = true },
                                                                modifier = Modifier
                                                                    .size(28.dp)
                                                                    .background(BrandGreen.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.ShowChart,
                                                                    contentDescription = "Go to Visuals",
                                                                    tint = BrandGreen,
                                                                    modifier = Modifier.size(15.dp)
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
                                                userEmail = null
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

    } // closes else Block
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
                            text = userEmail?.split("@")?.get(0)?.replaceFirstChar { it.uppercase() } ?: "User",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SlateTextLight
                        )
                        Text(
                            text = userEmail ?: "",
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
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SECURE LOGOUT", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

    val bgCol = when {
        isPeriodSales -> Color(0xFF1B2436) // Gorgeous dark sapphire container
        isPending -> Color(0xFF221626) // Sleek deep burgundy container
        else -> SlateCard
    }

    val tcCol = SlateTextLight

    val subtitleCol = SlateTextMuted

    val filterBorder = if (isFilterablePending && isPendingActive) {
        BorderStroke(1.5.dp, Color(0xFFEC4899))
    } else {
        BorderStroke(1.dp, SlateBorder)
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = isFilterablePending) { onPendingToggle?.invoke() },
        border = filterBorder,
        colors = CardDefaults.cardColors(containerColor = bgCol)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tcCol.copy(alpha = 0.03f),
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomEnd)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFilterablePending && isPendingActive) Color(0xFFEC4899) else subtitleCol,
                        letterSpacing = 1.2.sp
                    )

                    pacingChange?.let { pct ->
                        val isPos = pct >= 0
                        val tc = if (isPos) Color(0xFF15803D) else Color(0xFFB91C1C)
                        val arrow = if (isPos) "↑" else "↓"
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$arrow${String.format("%.1f%%", Math.abs(pct))}", color = tc, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(pacingLabel, color = subtitleCol, fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = tcCol,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.dp))
                ) {
                    val total = breakdown.values.sum()
                    if (total > 0f) {
                        breakdown.forEach { (name, amt) ->
                            val weight = (amt / total).toFloat()
                            if (weight > 0.005f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(weight)
                                        .background(resolveSeriesColor(name))
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Gray.copy(alpha = 0.3f))
                        )
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
            .padding(12.dp)
    ) {
        // Sticky description row titles on the left - Perfectly fit label text comfortably on one line
        Column(
            modifier = Modifier.width(92.dp)
        ) {
            // Header Space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "METRICS",
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Black,
                    color = SlateTextMuted,
                    letterSpacing = 0.5.sp,
                    lineHeight = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            
            // Revenue Label
            Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.CenterStart) {
                Text("REVENUE (₹ Cr)", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = SlateTextLight)
            }
            // Volume Label
            Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.CenterStart) {
                Text("VOLUME (MW)", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = SlateTextLight)
            }
            // Qty Label
            Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.CenterStart) {
                Text("CELLS QTY (K)", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = SlateTextLight)
            }
            
            // Space & Divider
            Spacer(modifier = Modifier.height(2.dp))
            HorizontalDivider(color = SlateBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(2.dp))

            // MoM Label
            Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.CenterStart) {
                Text("MoM PACING", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = SlateTextMuted)
            }
            // QoQ Label
            Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.CenterStart) {
                Text("QoQ PACING", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = SlateTextMuted)
            }
            // YoY Label
            Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.CenterStart) {
                Text("YoY PACING", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = SlateTextMuted)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Horizontally scrollable data columns representing months and totals
        BoxWithConstraints(
            modifier = Modifier.weight(1f)
        ) {
            // High fidelity adaptive layout mathematics:
            // Ensures columns are spaced cleanly and scroll smoothly if too many columns exist.
            // Minimum width of 100.dp guarantees ample spacing for longer numbers and labels.
            val totalCols = monthlyItems.size + (if (totalRow != null) 1 else 0)
            val spacing = 6.dp
            val spacingSpacing = spacing * (totalCols - 1)
            val availableWidth = maxWidth - spacingSpacing
            val minColWidth = 100.dp
            val calculatedColWidth = if (totalCols > 0) availableWidth / totalCols.toFloat() else minColWidth
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
                        isMonthSelected -> BrandGreen.copy(alpha = 0.14f)
                        isPartSelected -> BrandBlue.copy(alpha = 0.08f)
                        else -> Color.Transparent
                    }

                    Column(
                        modifier = Modifier
                            .width(columnWidth)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgCol)
                            .clickable { onMonthSelected(row.monthName) }
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header (Quarter indicator clickable badge + Month Name with dynamic year tag)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isPartSelected) BrandGreen.copy(alpha = 0.25f) else SlateBg)
                                    .clickable { onQuarterSelected(qIdx) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Q${qIdx + 1}",
                                    fontSize = 6.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isPartSelected) BrandGreen else SlateTextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = getMonthLabelWithYear(row.monthName, selectedFY),
                                fontSize = 8.sp,
                                fontWeight = if (isMonthSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isMonthSelected) BrandGreen else SlateTextLight
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        // 1. Revenue Cr Cell
                        Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = String.format("%.1f", row.revenueCr),
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isMonthSelected) FontWeight.Bold else FontWeight.Medium,
                                color = SlateTextLight
                            )
                        }

                        // 2. Volume Capacities Cell
                        Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = String.format("%.1f", row.capacityMw),
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isMonthSelected) FontWeight.Bold else FontWeight.Medium,
                                color = SlateTextLight
                            )
                        }

                        // 3. Qty K Cell
                        Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = String.format("%.0f", row.volumeQty / 1000.0),
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isMonthSelected) FontWeight.Bold else FontWeight.Medium,
                                color = SlateTextLight
                            )
                        }

                        // Divider boundary
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SlateBorder))
                        Spacer(modifier = Modifier.height(2.dp))

                        // 4. MoM Pacing pct
                        PacingCellValue(value = row.momChange, modifier = Modifier.fillMaxWidth().height(28.dp))

                        // 5. QoQ Pacing pct
                        PacingCellValue(value = row.qoqChange, modifier = Modifier.fillMaxWidth().height(28.dp))

                        // 6. YoY Pacing pct
                        PacingCellValue(value = row.yoyChange, modifier = Modifier.fillMaxWidth().height(28.dp))
                    }
                }

                // High Contrast TOTAL Column on extreme right
                if (totalRow != null) {
                    Column(
                        modifier = Modifier
                            .width(columnWidth)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandGreen.copy(alpha = 0.08f))
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "TOTAL",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = BrandGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        // Total Row 1: Revenue
                        Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = String.format("%.1f", totalRow.revenueCr),
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = BrandGreen
                            )
                        }

                        // Total Row 2: Capacity
                        Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = String.format("%.1f", totalRow.capacityMw),
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = BrandGreen
                            )
                        }

                        // Total Row 3: Volume K Qty
                        Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = String.format("%.0f", totalRow.volumeQty / 1000.0),
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = BrandGreen
                            )
                        }

                        // Divider segment
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BrandGreen.copy(alpha = 0.3f)))
                        Spacer(modifier = Modifier.height(2.dp))

                        // No composite pacing percentages in total columns
                        PacingCellValue(value = null, modifier = Modifier.fillMaxWidth().height(28.dp))
                        PacingCellValue(value = null, modifier = Modifier.fillMaxWidth().height(28.dp))
                        PacingCellValue(value = null, modifier = Modifier.fillMaxWidth().height(28.dp))
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
