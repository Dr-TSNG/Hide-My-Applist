package com.tsng.hidemyapplist.xposed

import android.util.Log
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.xposed.hooks.PackageManagerService
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class XposedEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        if (lpp.packageName == APPNAME) {
            XposedHelpers.findAndHookMethod("com.tsng.hidemyapplist.ui.XposedFragment", lpp.classLoader, "getXposedStatus", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = true
                }
            })
        }
        PackageManagerService().handleLoadPackage(lpp)
        //IndividualHooks().handleLoadPackage(lpp)
    }

    companion object {
        const val LOG = "hma_log"
        const val APPNAME = BuildConfig.APPLICATION_ID

        @JvmStatic
        fun getTemplatePref(pkg: String?): XSharedPreferences? {
            val template = XSharedPreferences(APPNAME, "Scope").getString(pkg, null) ?: return null
            return XSharedPreferences(APPNAME, "tpl_$template")
        }

        @JvmStatic
        fun isUseHook(pref: XSharedPreferences, callerName: String, hook: String): Boolean {
            if (callerName == APPNAME)
                if (!XSharedPreferences(APPNAME, "Templates").getBoolean("HookSelf", false))
                    return false
            val enableAllHooks = pref.getBoolean("EnableAllHooks", false)
            val enabled = pref.getStringSet("ApplyHooks", HashSet())
            return enableAllHooks or enabled.contains(hook)
        }

        @JvmStatic
        fun isToHide(pref: XSharedPreferences?, callerName: String, pkgstr: String): Boolean {
            if (pref == null) return false
            if (pref.getBoolean("ExcludeSelf", false) && pkgstr.contains(callerName)) return false
            if (pref.getBoolean("HideAllApps", false)) return true
            val set = pref.getStringSet("HideApps", HashSet())
            for (pkg in set) if (pkgstr.contains(pkg)) {
                Log.d(LOG, "HIDE $pkgstr")
                return true
            }
            return false
        }
    }
}