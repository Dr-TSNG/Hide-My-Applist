package com.tsng.hidemyapplist.xposed

import com.tsng.hidemyapplist.xposed.XposedUtils.APPNAME
import com.tsng.hidemyapplist.xposed.hooks.PackageManagerService
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class XposedEntry : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        if (lpp.packageName == APPNAME) {
            val entry = XposedHelpers.findClass("com.tsng.hidemyapplist.MainActivity", lpp.classLoader)
            XposedHelpers.setStaticBooleanField(entry, "isModuleActivated", true)
        }
        if (lpp.packageName == "android")
            PackageManagerService().handleLoadPackage(lpp)
    }
}