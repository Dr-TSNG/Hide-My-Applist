package com.tsng.hidemyapplist.xposed.hooks

import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.APPNAME
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.getTemplatePref
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.*
import java.nio.charset.StandardCharsets

class IndividualHooks : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        if (lpp.packageName == APPNAME)
            if(!XSharedPreferences(APPNAME, "Settings").getBoolean("HookSelf", false))
                return
        val pref: XSharedPreferences = getTemplatePref(lpp.packageName) ?: return
        val enableAllHooks = pref.getBoolean("EnableAllHooks", false)
        val enabled = pref.getStringSet("ApplyHooks", null)
        if (enableAllHooks || enabled.contains("File detections")) fileHook(lpp, pref)
    }

    fun fileHook(lpp: LoadPackageParam, pref: XSharedPreferences) {
        XposedHelpers.findAndHookConstructor(File::class.java, String::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val path = param.args[0] as String
                if (path.contains(lpp.packageName)) return
                if (pref.getBoolean("HideAllApps", false) && path.contains("Android/data/")) {
                    param.args[0] = "fuck/there/is/no/file"
                    return
                }
                for (pkg in pref.getStringSet("HideApps", null)) if (path.contains(pkg!!)) {
                    param.args[0] = "fuck/there/is/no/file"
                    break
                }
            }
        })
    }
}