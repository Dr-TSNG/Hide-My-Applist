package com.tsng.hidemyapplist.xposed

import android.content.Context
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.xposed.hooks.PackageManagerService
import de.robv.android.xposed.XposedBridge
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
        } catch (e: java.lang.IllegalArgumentException) {
            L.e("stopSystemService: Service not found")
        }
    }

    @JvmStatic
    fun getServiceVersion(context: Context): Int {
        return try {
            context.packageManager.getInstallerPackageName("checkHMAServiceVersion").toInt()
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    @JvmStatic
    fun getServeTimes(context: Context): Int {
        return try {
            context.packageManager.getInstallerPackageName("getServeTimes").toInt()
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    @JvmStatic
    fun getServicePreference(context: Context): String? {
        return try {
            context.packageManager.getInstallerPackageName("getPreference")
        } catch (e: java.lang.IllegalArgumentException) {
            null
        }
    }

    @JvmStatic
    fun callServiceIsUseHook(context: Context, callerName: String?, hookMethod: String): Boolean {
        try {
            val res = context.packageManager.getInstallerPackageName("callIsUseHook#$callerName#$hookMethod")
            if (res == resultIllegal) {
                L.e("callServiceIsUseHook: Illegal param callIsUseHook#$callerName#$hookMethod", context = context)
                return false
            }
            return res == resultYes
        } catch (e: IllegalArgumentException) {
            L.e("callServiceIsUseHook: Service not found")
            return false
        }
    }

    @JvmStatic
    fun callServiceIsToHide(context: Context, callerName: String?, pkgstr: String?, fileHook: Boolean): Boolean {
        try {
            val res = if (fileHook) context.packageManager.getInstallerPackageName("callIsHideFile#$callerName#$pkgstr")
            else context.packageManager.getInstallerPackageName("callIsToHide#$callerName#$pkgstr")
            if (res == resultIllegal) {
                L.e("callServiceIsToHide: Illegal param callIsUseHook#$callerName#$pkgstr", context = context)
                return false
            }
            return res == resultYes
        } catch (e: IllegalArgumentException) {
            L.e("callServiceIsToHide: Service not found")
            return false
        }
    }

    @JvmStatic
    fun getRecursiveField(entry: Any, list: List<String>): Any? {
        var field: Any? = entry
        for (it in list)
            field = XposedHelpers.getObjectField(field, it) ?: return null
        return field
    }

    object L {
        @JvmStatic
        private fun send(str: String, PKMS: PackageManagerService?, context: Context?) {
            XposedBridge.log(str)
            PKMS?.addLog(str)
            context?.let {
                try {
                    it.packageManager.getInstallerPackageName("addLog#$str")
                } catch (e: IllegalArgumentException) { }
            }
        }

        @JvmStatic
        fun d(log: String, PKMS: PackageManagerService? = null, context: Context? = null) {
            send("[HMA Xposed] [DEBUG] $log", PKMS, context)
        }

        @JvmStatic
        fun i(log: String, PKMS: PackageManagerService? = null, context: Context? = null) {
            send("[HMA Xposed] [INFO] $log", PKMS, context)
        }

        @JvmStatic
        fun e(log: String, PKMS: PackageManagerService? = null, context: Context? = null) {
            send("[HMA Xposed] [ERROR] $log", PKMS, context)
        }

        @JvmStatic
        fun nd(log: String, PKMS: PackageManagerService? = null, context: Context? = null) {
            send("[HMA Native] [DEBUG] $log", PKMS, context)
        }

        @JvmStatic
        fun ni(log: String, PKMS: PackageManagerService? = null, context: Context? = null) {
            send("[HMA Native] [INFO] $log", PKMS, context)
        }

        @JvmStatic
        fun ne(log: String, PKMS: PackageManagerService? = null, context: Context? = null) {
            send("[HMA Native] [ERROR] $log", PKMS, context)
        }
    }
}