package com.tsng.hidemyapplist.xposed

import android.content.Context
import com.tsng.hidemyapplist.BuildConfig
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class XposedUtils {
    companion object {
        const val LOG = "hma_log"
        const val resultNo = "HMA No"
        const val resultYes = "HMA Yes"
        const val resultIllegal = "HMA Illegal"
        const val APPNAME = BuildConfig.APPLICATION_ID

        @JvmStatic
        fun getServiceVersion(context: Context): Int {
            return try {
                context.packageManager.getInstallerPackageName("checkHMAServiceVersion").toInt()
            } catch (e: IllegalArgumentException) { 0 }
        }

        @JvmStatic
        fun callServiceIsUseHook(context: Context, callerName: String?, hookMethod: String): Boolean {
            try {
                val res = context.packageManager.getInstallerPackageName("callIsUseHook#$callerName#$hookMethod")
                if (res == resultIllegal) {
                    le("callServiceIsUseHook: Illegal param callIsUseHook#$callerName#$hookMethod")
                    return false
                }
                return res == resultYes
            } catch (e: IllegalArgumentException) {
                le("callServiceIsUseHook: Service not found")
                return false
            }
        }

        @JvmStatic
        fun callServiceIsToHide(context: Context, callerName: String?, pkgstr: String?, fileHook: Boolean): Boolean {
            try {
                val res = if (fileHook) context.packageManager.getInstallerPackageName("callIsHideFile#$callerName#$pkgstr")
                        else context.packageManager.getInstallerPackageName("callIsToHide#$callerName#$pkgstr")
                if (res == resultIllegal) {
                    le("callServiceIsToHide: Illegal param callIsUseHook#$callerName#$pkgstr")
                    return false
                }
                return res == resultYes
            } catch (e: IllegalArgumentException) {
                le("callServiceIsToHide: Service not found")
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

        @JvmStatic
        fun ld(log: String) {
            XposedBridge.log("[HMA LOG] [DEBUG] $log")
        }

        @JvmStatic
        fun li(log: String) {
            XposedBridge.log("[HMA LOG] [INFO] $log")
        }

        @JvmStatic
        fun le(log: String) {
            XposedBridge.log("[HMA LOG] [ERROR] $log")
        }
    }
}