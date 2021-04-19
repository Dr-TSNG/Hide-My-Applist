package com.tsng.hidemyapplist.xposed

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.tsng.hidemyapplist.BuildConfig
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class XposedUtils {
    companion object {
        const val LOG = "hma_log"
        const val APPNAME = BuildConfig.APPLICATION_ID

        @JvmStatic
        fun callServiceIsUseHook(context: Context, callerName: String?, hookMethod: String): Boolean {
            return try {
                context.packageManager.getPackageUid("callIsUseHook#$callerName#$hookMethod", 0) == 1
            } catch (e: PackageManager.NameNotFoundException) {
                le("callServiceIsUseHook: Service not found")
                false
            }
        }

        @JvmStatic
        fun callServiceIsToHide(context: Context, callerName: String?, pkgstr: String?, fileHook: Boolean): Boolean {
            return try {
                if (fileHook) context.packageManager.getPackageUid("callIsHideFile#$callerName#$pkgstr", 0) == 1
                else context.packageManager.getPackageUid("callIsToHide#$callerName#$pkgstr", 0) == 1
            } catch (e: PackageManager.NameNotFoundException) {
                le("callServiceIsToHide: Service not found")
                false
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