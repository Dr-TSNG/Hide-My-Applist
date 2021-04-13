package com.tsng.hidemyapplist.xposed

import com.tsng.hidemyapplist.xposed.hooks.IndividualHooks
import com.tsng.hidemyapplist.xposed.hooks.PackageManagerService
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class XposedEntry : IXposedHookLoadPackage, XposedBase() {
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        if (lpp.packageName == APPNAME) {
            XposedHelpers.findAndHookMethod("com.tsng.hidemyapplist.MainActivity", lpp.classLoader, "getXposedStatus", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = if (getHookMode() == "Individual Hook") 0b01 else 0b10
                }
            })
        }
        if (getHookMode() == "Individual Hook")
            IndividualHooks().handleLoadPackage(lpp)
        else
            PackageManagerService().handleLoadPackage(lpp)
        XSharedPreferences(APPNAME, "Settings").makeWorldReadable()
    }

    fun getHookMode(): String {
        return XSharedPreferences(APPNAME, "Settings").getString("HookMode", "Individual Hook")
    }
}