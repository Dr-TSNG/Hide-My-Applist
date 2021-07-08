package com.tsng.hidemyapplist.app.helpers

import com.tsng.hidemyapplist.app.MyApplication.Companion.appContext

object ServiceHelper {
    @JvmStatic
    fun getServiceVersion(): Int {
        return try {
            appContext.packageManager.getInstallerPackageName("getServiceVersion")!!.toInt()
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    @JvmStatic
    fun getRiruExtensionVersion(): Int {
        return try {
            appContext.packageManager.getInstallerPackageName("getRiruExtensionVersion")!!.toInt()
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    @JvmStatic
    fun getServeTimes(): Int {
        return try {
            appContext.packageManager.getInstallerPackageName("getServeTimes")!!.toInt()
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    @JvmStatic
    fun getLogs(): String? {
        return try {
            appContext.packageManager.getInstallerPackageName("getLogs")
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    @JvmStatic
    fun cleanLogs() {
        try {
            appContext.packageManager.getInstallerPackageName("cleanLogs")
        } catch (e: IllegalArgumentException) {
        }
    }

    @JvmStatic
    fun submitConfig(json: String) {
        try {
            appContext.packageManager.getInstallerPackageName("submitConfig#$json")
        } catch (e: IllegalArgumentException) {
        }
    }

    @JvmStatic
    fun stopSystemService(cleanEnv: Boolean) {
        try {
            appContext.packageManager.getInstallerPackageName("stopSystemService#$cleanEnv")
        } catch (e: IllegalArgumentException) {
        }
    }
}