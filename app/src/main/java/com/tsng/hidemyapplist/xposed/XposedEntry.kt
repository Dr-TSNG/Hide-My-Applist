package com.tsng.hidemyapplist.xposed

import com.tsng.hidemyapplist.xposed.XposedUtils.APPNAME
import com.tsng.hidemyapplist.xposed.hooks.IndividualHooks
import com.tsng.hidemyapplist.xposed.hooks.PackageManagerService
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class XposedEntry : IXposedHookLoadPackage, IXposedHookZygoteInit {
    companion object {
        lateinit var modulePath: String
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
    }

    override fun handleLoadPackage(lpp: LoadPackageParam) {
        if (lpp.packageName == APPNAME) {
            XposedHelpers.findAndHookMethod("com.tsng.hidemyapplist.MainActivity", lpp.classLoader, "isModuleActivated", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = true
                }
            })
        }
        if (lpp.packageName == "android")
            PackageManagerService().handleLoadPackage(lpp)
        else
            IndividualHooks().handleLoadPackage(lpp)
    }
}