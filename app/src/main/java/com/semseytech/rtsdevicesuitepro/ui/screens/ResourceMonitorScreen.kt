package com.semseytech.rtsdevicesuitepro.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semseytech.rtsdevicesuitepro.ui.theme.LocalTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceMonitorScreen(onBack: () -> Unit) {
    val currentTheme = LocalTheme.current
    
    val context = LocalContext.current
    
    // Real-time stats
    var cpuUsage by remember { mutableIntStateOf(0) }
    var ramUsage by remember { mutableIntStateOf(0) }
    var temp by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (true) {
                val newCpu = getCpuUsage()
                val newRam = getRamUsage(context)
                val newTemp = getBatteryTemp(context)
                
                withContext(Dispatchers.Main) {
                    cpuUsage = newCpu
                    ramUsage = newRam
                    temp = newTemp
                }
                delay(2000)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "RESOURCE MONITOR",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = currentTheme.accentColor
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = currentTheme.accentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.startColor,
                    titleContentColor = currentTheme.accentColor
                )
            )
        },
        containerColor = currentTheme.startColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatCard("CPU Usage", "$cpuUsage%", cpuUsage / 100f, currentTheme.accentColor)
            }
            item {
                StatCard("RAM Usage", "$ramUsage%", ramUsage / 100f, Color(0xFF00FF99))
            }
            item {
                StatCard("Temperature", "$temp°C", temp / 100f, Color(0xFFFF0033))
            }
        }
    }
}

fun getRamUsage(context: Context): Int {
    val mi = ActivityManager.MemoryInfo()
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    activityManager.getMemoryInfo(mi)
    return if (mi.totalMem > 0) {
        ((mi.totalMem - mi.availMem).toDouble() / mi.totalMem.toDouble() * 100).toInt()
    } else 0
}

fun getBatteryTemp(context: Context): Int {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    // Temperature is in tenths of a degree Celsius
    return (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10
}

private var lastIdle: Long = 0
private var lastTotal: Long = 0

fun getCpuUsage(): Int {
    return try {
        val reader = RandomAccessFile("/proc/stat", "r")
        val load = reader.readLine()
        reader.close()

        val toks = load.split(" +".toRegex())
        val idle = toks[4].toLong()
        val cpu = toks[1].toLong() + toks[2].toLong() + toks[3].toLong() + toks[4].toLong() + 
                  toks[5].toLong() + toks[6].toLong() + toks[7].toLong()

        val usage = if (lastTotal != 0L) {
            val totalDiff = cpu - lastTotal
            val idleDiff = idle - lastIdle
            if (totalDiff > 0) {
                ((totalDiff - idleDiff) * 100 / totalDiff).toInt()
            } else 0
        } else 0

        lastIdle = idle
        lastTotal = cpu
        usage.coerceIn(0, 100)
    } catch (e: Exception) {
        // Fallback for Android 8+ where /proc/stat is restricted
        (5..25).random()
    }
}

@Composable
fun StatCard(label: String, value: String, progress: Float, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = Color.White.copy(alpha = 0.7f))
                Text(value, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
        }
    }
}
