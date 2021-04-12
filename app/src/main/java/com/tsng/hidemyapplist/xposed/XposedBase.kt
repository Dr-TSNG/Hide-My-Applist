package com.tsng.hidemyapplist.xposed

import android.util.Log
import com.tsng.hidemyapplist.BuildConfig
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge

open class XposedBase {
    val LOG = "hma_log"
    val APPNAME = BuildConfig.APPLICATION_ID

    fun getTemplatePref(pkg: String?): XSharedPreferences? {
        val pref = XSharedPreferences(APPNAME, "Scope")
        if (!pref.file.exists()) return null
        val str = pref.getString(pkg, null) ?: return null
        return XSharedPreferences(APPNAME, "tpl_$str")
    }

    fun isUseHook(pref: XSharedPreferences?, callerName: String?, hook: String): Boolean {
        if (pref == null) return false
        if (callerName == APPNAME)
            if (!XSharedPreferences(APPNAME, "Settings").getBoolean("HookSelf", false))
                return false
        val enableAllHooks = pref.getBoolean("EnableAllHooks", false)
        val enabled = pref.getStringSet("ApplyHooks", HashSet())
        return enableAllHooks or enabled.contains(hook)
    }

    fun isToHide(pref: XSharedPreferences?, callerName: String, pkgstr: String): Boolean {
        if (pref == null) return false
        if (pref.getBoolean("ExcludeSelf", false) && pkgstr.contains(callerName)) return false
        if (pref.getBoolean("HideAllApps", false)) return true
        val set = pref.getStringSet("HideApps", HashSet())
        for (pkg in set) if (pkgstr.contains(pkg)) {
            ld("HIDE $pkgstr")
            return true
        }
        return false
    }

    fun ld(log: String) {
        XposedBridge.log("$APPNAME DEBUG: $log")
        Log.d(LOG, log)
    }

    fun le(log: String) {
        XposedBridge.log("$APPNAME ERROR: $log")
        Log.e(LOG, log)
    }
}