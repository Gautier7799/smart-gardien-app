package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContent {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        GuardScreen(
                            onKeepScreenOn = { keepOn ->
                                try {
                                    if (keepOn) {
                                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    } else {
                                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    }
                                } catch (e: Exception) {}
                            }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "حدث خطأ وتم تجاوزه!", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun GuardScreen(modifier: Modifier = Modifier, onKeepScreenOn: (Boolean) -> Unit) {
    var isGuardActive by remember { mutableStateOf(false) }
    var isAlarmTriggered by remember { mutableStateOf(false) }
    var isCountingDown by remember { mutableStateOf(false) }
    var remainingTime by remember { mutableStateOf(0) }
    
    // إعدادات التطبيق
    var activationDelay by remember { mutableStateOf(5) }
    var soundType by remember { mutableStateOf(RingtoneManager.TYPE_ALARM) }
    var showSettings by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // نظام العد التنازلي قبل التفعيل
    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            remainingTime = activationDelay
            while (remainingTime > 0) {
                delay(1000)
                remainingTime--
            }
            isCountingDown = false
            isGuardActive = true
            isAlarmTriggered = false
        }
    }

    DisposableEffect(isGuardActive) {
        onKeepScreenOn(isGuardActive)
        var sensorManager: SensorManager? = null
        var accelerometer: Sensor? = null
        
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } catch (e: Exception) {}
        
        var initialX = 0f
        var initialY = 0f
        var initialZ = 0f
        var isInitialized = false
        val threshold = 2.5f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (!isGuardActive || event == null) return
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    if (!isInitialized) {
                        initialX = x
                        initialY = y
                        initialZ = z
                        isInitialized = true
                    } else {
                        val deltaX = Math.abs(initialX - x)
                        val deltaY = Math.abs(initialY - y)
                        val deltaZ = Math.abs(initialZ - z)

                        if (deltaX > threshold || deltaY > threshold || deltaZ > threshold) {
                            isAlarmTriggered = true
                            isGuardActive = false
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (isGuardActive) {
            try {
                accelerometer?.let {
                    sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
            } catch (e: Exception) {
                isGuardActive = false
            }
        }

        onDispose {
            try {
                sensorManager?.unregisterListener(listener)
            } catch (e: Exception) {}
            onKeepScreenOn(false)
        }
    }

    LaunchedEffect(isAlarmTriggered) {
        if (isAlarmTriggered) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), -1))
                } else {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
                }
            } catch (e: Exception) {}

            var ringtone: android.media.Ringtone? = null
            try {
                // استخدام الصوت الذي اختاره المستخدم من الإعدادات
                val alarmUri = RingtoneManager.getDefaultUri(soundType) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                if (alarmUri != null) {
                    ringtone = RingtoneManager.getRingtone(context, alarmUri)
                    ringtone?.play()
                }
            } catch (e: Exception) {}

            delay(5000)
            
            try {
                ringtone?.stop()
            } catch (e: Exception) {}
            
            isAlarmTriggered = false
        }
    }

    // واجهة الإعدادات المنبثقة
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("إعدادات الحارس", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("مهلة التفعيل (لترك الهاتف): $activationDelay ثوانٍ", fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = activationDelay.toFloat(),
                        onValueChange = { activationDelay = it.toInt() },
                        valueRange = 2f..15f,
                        steps = 12
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("نوع صوت التنبيه:", fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = soundType == RingtoneManager.TYPE_ALARM, onClick = { soundType = RingtoneManager.TYPE_ALARM })
                        Text("إنذار عالي (Alarm)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = soundType == RingtoneManager.TYPE_RINGTONE, onClick = { soundType = RingtoneManager.TYPE_RINGTONE })
                        Text("رنة الهاتف (Ringtone)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = soundType == RingtoneManager.TYPE_NOTIFICATION, onClick = { soundType = RingtoneManager.TYPE_NOTIFICATION })
                        Text("إشعار قصير (Notification)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("حفظ وإغلاق")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // زر الإعدادات في أعلى الشاشة
        IconButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "الإعدادات",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "حارس الهاتف", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
            
            val instructionText = if (isCountingDown) "ضع الهاتف الآن! سيتم التفعيل بعد $remainingTime ثوانٍ..." else "أبقِ التطبيق مفتوحاً. سيتم إطلاق إنذار عند محاولة تحريك هاتفك."
            Text(text = instructionText, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 64.dp), textAlign = TextAlign.Center)

            val statusText = if (isAlarmTriggered) "🚨 إنذار 🚨" else if (isGuardActive) "الحارس يراقب الآن" else if (isCountingDown) "يستعد..." else "الحارس متوقف"
            val buttonText = if (isGuardActive || isCountingDown || isAlarmTriggered) "إيقاف الحارس" else "تفعيل الحارس"
            val containerColor = if (isAlarmTriggered || isGuardActive) Color(0xFFBA1A1A) else if (isCountingDown) Color(0xFFE29E25) else MaterialTheme.colorScheme.primaryContainer
            val contentColor = if (isAlarmTriggered || isGuardActive || isCountingDown) Color.White else MaterialTheme.colorScheme.onPrimaryContainer

            Box(
                modifier = Modifier.size(200.dp).clip(CircleShape).background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                // عرض الأرقام داخل الدائرة أثناء العد التنازلي
                val circleText = if (isAlarmTriggered) "إنذار!" else if (isGuardActive) "مفعل" else if (isCountingDown) "$remainingTime" else "متوقف"
                Text(text = circleText, fontSize = if (isCountingDown) 64.sp else 36.sp, fontWeight = FontWeight.Bold, color = contentColor)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = statusText, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (isAlarmTriggered) Color.Red else MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (isGuardActive || isCountingDown || isAlarmTriggered) {
                        isGuardActive = false
                        isCountingDown = false
                        isAlarmTriggered = false
                    } else {
                        isCountingDown = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isGuardActive || isAlarmTriggered || isCountingDown) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.primary)
            ) {
                Text(text = buttonText, fontSize = 18.sp, color = Color.White)
            }
        }
    }
}
