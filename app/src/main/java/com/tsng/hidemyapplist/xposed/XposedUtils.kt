package com.tsng.hidemyapplist.xposed

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.tsng.hidemyapplist.BuildConfig
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class XposedUtils {
    companion object {
        const val LOG = "hma_log"
        const val APPNAME = BuildConfig.APPLICATION_ID

        @JvmStatic
        fun callServiceUpdatePref(context: Context) {
            try {
                context.packageManager.getPackageUid("updatePreference", 0)
            } catch (e: PackageManager.NameNotFoundException) { }
        }

        @JvmStatic
        fun getTemplatePref(pkg: String?): XSharedPreferences? {
            val pref = XSharedPreferences(APPNAME, "Scope")
            if (!pref.file.exists()) return null
            val str = pref.getString(pkg, null) ?: return null
            return XSharedPreferences(APPNAME, "tpl_$str")
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
            XposedBridge.log("[HMA DEBUG] $log")
            Log.d(LOG, log)
        }

        @JvmStatic
        fun le(log: String) {
            XposedBridge.log("[HMA ERROR] $log")
            Log.e(LOG, log)
        }
    }
}