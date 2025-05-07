package com.example.timewell

import android.content.Context

fun getAppNameFromPackage(context: Context, packageName: String): String {
    val knownAppNames = mapOf(
        "com.facebook.katana" to "Facebook",
        "com.instagram.android" to "Instagram",
        "com.whatsapp" to "WhatsApp",
        "com.snapchat.android" to "Snapchat"
    )

    knownAppNames[packageName]?.let { return it }

    return try {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        val label = packageManager.getApplicationLabel(appInfo).toString()
        if (label.equals("android", true) || label.length <= 2) fallbackLabel(packageName) else label
    } catch (e: Exception) {
        fallbackLabel(packageName)
    }
}

fun fallbackLabel(packageName: String): String {
    return packageName.split('.').lastOrNull()
        ?.replaceFirstChar { it.uppercaseChar() }
        ?: packageName
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

