package com.tsng.hidemyapplist.xposed

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
        fun getTemplatePref(pkg: String?): XSharedPreferences? {
            val pref = XSharedPreferences(APPNAME, "Scope")
            if (!pref.file.exists()) return null
            val str = pref.getString(pkg, null) ?: return null
            return XSharedPreferences(APPNAME, "tpl_$str")
        }

        @JvmStatic
        fun isUseHook(pref: XSharedPreferences?, callerName: String?, hook: String): Boolean {
            if (pref == null) return false
            if (callerName == APPNAME)
                if (!XSharedPreferences(APPNAME, "Settings").getBoolean("HookSelf", false))
                    return false
            val enableAllHooks = pref.getBoolean("EnableAllHooks", false)
            val enabled = pref.getStringSet("ApplyHooks", HashSet())
            return enableAllHooks or enabled.contains(hook)
        }

        @JvmStatic
        fun isToHide(pref: XSharedPreferences?, callerName: String, pkgstr: String?): Boolean {
            if (pref == null || pkgstr == null) return false
            if (pref.getBoolean("ExcludeSelf", false) && pkgstr.contains(callerName)) return false
            if (pref.getBoolean("HideAllApps", false)) return true
            val set = pref.getStringSet("HideApps", HashSet())
            for (pkg in set) if (pkgstr.contains(pkg)) {
                ld("HIDE $pkgstr")
                return true
            }
            return false
        }

        @JvmStatic
        fun getRecursiveField(entry: Any, list: List<String>) : Any? {
            var field : Any? = entry
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