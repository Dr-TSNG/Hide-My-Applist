package com.tsng.hidemyapplist.xposed.hooks

import android.content.pm.ParceledListSlice
import android.os.Binder
import com.tsng.hidemyapplist.xposed.XposedBase
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class PackageManagerService : XposedBase(), IXposedHookLoadPackage {
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        if (lpp.packageName != "android") return
        val PKMS = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", lpp.classLoader)
        for (method in PKMS.declaredMethods) when (method.name) {
            "getInstalledPackages",
            "getInstalledApplications",
            "getPackagesHoldingPermissions",
            "queryInstrumentation" ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", Binder.getCallingUid()) as String
                        ld("PKMS caller: $callerName")
                        ld("PKMS method: " + param.method.name)
                        val pref = getTemplatePref(callerName)
                        if (!isUseHook(pref, callerName, "API requests")) return
                        val iterator: MutableIterator<*> = (param.result as ParceledListSlice<*>).list.iterator()
                        while (iterator.hasNext()) {
                            if (isToHide(pref, callerName, (XposedHelpers.getObjectField(iterator.next(), "packageName") as String))) iterator.remove()
                        }
                        ld("PKMS dealt")
                    }
                })
        }
    }
}