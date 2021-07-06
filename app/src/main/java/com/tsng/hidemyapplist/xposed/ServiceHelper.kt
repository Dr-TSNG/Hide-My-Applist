package com.tsng.hidemyapplist.xposed

import android.content.Context

object ServiceHelper {
    @JvmStatic
    fun Context.getServiceVersion(): Int {
        return try {
            packageManager.getInstallerPackageName("getServiceVersion")!!.toInt()
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    @JvmStatic
    fun Context.getRiruExtensionVersion(): Int {
        return try {
            packageManager.getInstallerPackageName("getRiruExtensionVersion")!!.toInt()
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    @JvmStatic
    fun Context.getServeTimes(): Int {
        return try {
            packageManager.getInstallerPackageName("getServeTimes")!!.toInt()
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    @JvmStatic
    fun Context.getLogs(): String? {
        return try {
            packageManager.getInstallerPackageName("getLogs")
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    @JvmStatic
    fun Context.cleanLogs() {
        try {
            packageManager.getInstallerPackageName("cleanLogs")
        } catch (e: IllegalArgumentException) {
        }
    }

    @JvmStatic
    fun Context.submitConfig() {
        try {
            packageManager.getInstallerPackageName("submitConfig")
        } catch (e: IllegalArgumentException) {
        }
    }

    @JvmStatic
    fun Context.stopSystemService(cleanEnv: Boolean) {
        try {
            packageManager.getInstallerPackageName("stopSystemService#$cleanEnv")
        } catch (e: IllegalArgumentException) {
        }
    }
}