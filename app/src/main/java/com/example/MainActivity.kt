package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
        
        // التقاط أي انهيار في الخلفية قبل أن يغلق النظام التطبيق
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            prefs.edit().putString("last_crash", sw.toString()).apply()
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }

        // التحقق مما إذا كان هناك خطأ مسجل من المحاولة السابقة
        val crashError = prefs.getString("last_crash", null)
        if (crashError != null) {
            prefs.edit().remove("last_crash").apply()
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (crashError != null) {
                        // إذا حدث انهيار سابق، اعرضه على الشاشة
                        ErrorScreen(crashError)
                    } else {
                        try {
                            GuardScreen()
                        } catch (e: Exception) {
                            // التقاط الأخطاء اللحظية
                            val sw = StringWriter()
                            e.printStackTrace(PrintWriter(sw))
                            ErrorScreen(sw.toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(error: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("⚠️ تم التقاط الخطأ!", fontSize = 24.sp, color = Color.Red, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("أرجوك قم بتصوير هذه الشاشة وأرسلها لي لنعرف السبب الدقيق:", fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(error, fontSize = 12.sp, color = Color.DarkGray)
    }
}

@Composable
fun GuardScreen(modifier: Modifier = Modifier) {
    var isGuardActive by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "حارس الهاتف",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "سيقوم التطبيق بتنبيهك عند لمس هاتفك أو تحريكه.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 64.dp),
            textAlign = TextAlign.Center
        )

        val statusText = if (isGuardActive) "الحارس نشط" else "الحارس متوقف"
        val buttonText = if (isGuardActive) "إيقاف الحارس" else "تفعيل الحارس"
        val containerColor = if (isGuardActive) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.primaryContainer
        val contentColor = if (isGuardActive) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isGuardActive) "مفعل" else "متوقف",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = statusText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                isGuardActive = !isGuardActive
                val intent = Intent(context, GuardService::class.java)
                if (isGuardActive) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } else {
                    intent.action = "STOP_GUARD"
                    context.startService(intent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGuardActive) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = buttonText, fontSize = 18.sp, color = Color.White)
        }
    }
}
