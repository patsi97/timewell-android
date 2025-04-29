package com.example.timewell

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.timewell.ui.theme.TimeWellTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TimeWellTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UsagePermissionScreen()
                }
            }
        }
    }
}

@Composable
fun UsagePermissionScreen() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(checkUsagePermission(context)) }
    var usageStats by remember { mutableStateOf<List<UsageStats>>(emptyList()) }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            usageStats = getAppUsageStats(context)
            logUsageStats(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (hasPermission) {
            Text("Permission granted ✅")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Top used apps today:")
            Spacer(modifier = Modifier.height(8.dp))
            usageStats.take(10).forEach { stat ->
                val appName = getAppNameFromPackage(context, stat.packageName)
                val timeFormatted = formatTime((stat.totalTimeInForeground / 1000).toInt())
                Text("$appName — $timeFormatted")
            }

        } else {
            Text("Usage access permission is required.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }) {
                Text("Open Permission Settings")
            }
        }
    }
}

fun checkUsagePermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun getAppUsageStats(context: Context): List<UsageStats> {
    val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val packageManager = context.packageManager
    val endTime = System.currentTimeMillis()
    val startTime = endTime - 1000 * 60 * 60 * 24 // last 24 hours

    return usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime
    ).filter { stat ->
        // Filter criteria:
        stat.totalTimeInForeground > 0 &&
                try {
                    val appInfo = packageManager.getApplicationInfo(stat.packageName, 0)
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                } catch (e: PackageManager.NameNotFoundException) {
                    false // exclude unknown packages
                }
    }.sortedByDescending { it.totalTimeInForeground }
}


fun logUsageStats(context: Context) {
    val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val endTime = System.currentTimeMillis()
    val startTime = endTime - 1000 * 60 * 60 * 24

    val stats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime
    )

    if (stats.isNullOrEmpty()) {
        Log.d("TimeWell", "No usage stats found.")
    } else {
        stats.filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .forEach {
                Log.d(
                    "TimeWell",
                    "App: ${it.packageName}, time: ${it.totalTimeInForeground / 1000}s"
                )
            }
    }
}

