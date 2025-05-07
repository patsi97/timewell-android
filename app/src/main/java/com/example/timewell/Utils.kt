package com.example.timewell

import android.content.Context
import android.content.res.Resources

fun getAppNameFromPackage(context: Context, packageName: String): String {
    val packageManager = context.packageManager
    return try {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)

        // Try label resource first
        val label = try {
            if (appInfo.labelRes != 0) {
                context.getString(appInfo.labelRes)
            } else {
                packageManager.getApplicationLabel(appInfo).toString().trim()
            }
        } catch (e: Resources.NotFoundException) {
            // Fallback if labelRes points to a missing resource
            packageManager.getApplicationLabel(appInfo).toString().trim()
        }


        if (
            label.equals("android", ignoreCase = true) ||
            label.equals("katana", ignoreCase = true) ||
            label.equals("insta", ignoreCase = true) ||
            label.length <= 3
        ) {
            fallbackLabel(packageName)
        } else {
            label
        }
    } catch (e: Exception) {
        fallbackLabel(packageName)
    }
}


// Helper for fallback label
fun fallbackLabel(packageName: String): String {
    return packageName.split('.').lastOrNull()?.replaceFirstChar { it.uppercaseChar() } ?: packageName
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

