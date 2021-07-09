package com.tsng.hidemyapplist.xposed

import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.init.InitFields.hostPackageName
import com.github.kyuubiran.ezxhelper.utils.getFieldBySig
import com.tsng.hidemyapplist.BuildConfig
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedEntry : IXposedHookZygoteInit, IXposedHookLoadPackage {
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelperInit.initZygote(startupParam)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        EzXHelperInit.initHandleLoadPackage(lpparam)
        EzXHelperInit.setLogTag("HMA Xposed")
        EzXHelperInit.setToastTag("HMA")

        if (hostPackageName == BuildConfig.APPLICATION_ID) {
            getFieldBySig("Lcom/tsng/hidemyapplist/app/MyApplication;->isModuleActivated:Z")
                .setBoolean(null, true)
        }
        if (hostPackageName == "android")
            PackageManagerService.entry()
    }
}