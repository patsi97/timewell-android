package com.example.timewell

import android.content.Context
import android.content.pm.PackageManager

fun getAppNameFromPackage(context: Context, packageName: String): String {
    return try {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        // Try best-effort label from package
        packageName.split('.').lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
    }
}


fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> "%dh %02dm".format(hours, minutes)
        minutes > 0 -> "%dm %02ds".format(minutes, secs)
        else -> "%ds".format(secs)
    }
}
