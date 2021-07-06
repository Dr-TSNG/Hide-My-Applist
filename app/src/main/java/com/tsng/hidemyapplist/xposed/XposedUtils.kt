package com.tsng.hidemyapplist.xposed

import android.content.Context
import com.tsng.hidemyapplist.BuildConfig
import de.robv.android.xposed.XposedHelpers

object XposedUtils {
    const val resultNo = "HMA No"
    const val resultYes = "HMA Yes"
    const val resultIllegal = "HMA Illegal"
    const val APPNAME = BuildConfig.APPLICATION_ID

    @JvmStatic
    fun stopSystemService(context: Context, cleanEnv: Boolean) {
        try {
            context.packageManager.getInstallerPackageName("stopSystemService#$cleanEnv")
        } catch (e: IllegalArgumentException) {
        }
    }

    @JvmStatic
    fun getServiceVersion(context: Context): Int {
        return try {
            context.packageManager.getInstallerPackageName("checkHMAServiceVersion")!!.toInt()
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    @JvmStatic
    fun getRiruExtensionVersion(context: Context): Int {
        return try {
            context.packageManager.getInstallerPackageName("checkRiruExtensionVersion")!!.toInt()
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    @JvmStatic
    fun getServeTimes(context: Context): Int {
        return try {
            context.packageManager.getInstallerPackageName("getServeTimes")!!.toInt()
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    @JvmStatic
    fun getLogs(context: Context): String? {
        return try {
            context.packageManager.getInstallerPackageName("getLogs")
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    @JvmStatic
    fun cleanLogs(context: Context) {
        try {
            context.packageManager.getInstallerPackageName("cleanLogs")
        } catch (e: IllegalArgumentException) {
        }
    }

    @JvmStatic
    fun getRecursiveField(entry: Any, list: List<String>): Any? {
        var field: Any? = entry
        for (it in list)
            field = XposedHelpers.getObjectField(field, it) ?: return null
        return field
    }
}