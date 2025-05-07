package com.example.timewell

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    var showAllApps by remember { mutableStateOf(false) }

    LaunchedEffect(hasPermission, showAllApps) {
        if (hasPermission) {
            usageStats = getAppUsageStats(context, showAllApps)
            logUsageStats(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        if (hasPermission) {
            Text("Permission granted ✅", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Top used apps today:", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    usageStats = getAppUsageStats(context, showAllApps)
                }) {
                    Text("Refresh")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show all apps", modifier = Modifier.padding(end = 8.dp))
                    Switch(
                        checked = showAllApps,
                        onCheckedChange = {
                            showAllApps = it
                            usageStats = getAppUsageStats(context, showAllApps)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = usageStats, key = { it.packageName }) { stat ->
                    val appName = getAppNameFromPackage(context, stat.packageName)
                    val timeFormatted = formatTime((stat.totalTimeInForeground / 1000).toInt())

                    Column {
                        Text(
                            text = "$appName — $timeFormatted",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
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

fun getAppUsageStats(context: Context, showAllApps: Boolean): List<UsageStats> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val packageManager = context.packageManager
    val endTime = System.currentTimeMillis()
    val startTime = endTime - 1000 * 60 * 60 * 24 // last 24 hours

    val allStats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime
    )

    if (showAllApps) {
        return allStats
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
    }

    val installedPackages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { appInfo ->
            packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
        }
        .map { it.packageName }
        .toSet()

    return allStats
        .filter { stat ->
            stat.totalTimeInForeground > 0 && stat.packageName in installedPackages
        }
        .sortedByDescending { it.totalTimeInForeground }
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

