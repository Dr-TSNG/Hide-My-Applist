package com.tsng.hidemyapplist.xposed.hooks

import android.content.pm.ParceledListSlice
import android.os.Binder
import android.os.Process
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.APPNAME
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.getRecursiveField
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.getTemplatePref
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.isToHide
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.isUseHook
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.ld
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.lang.reflect.Method

class PackageManagerService : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        if (lpp.packageName != "android") return
        val PKMS = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", lpp.classLoader)
        for (method in PKMS.declaredMethods) when (method.name) {
            "getInstalledPackages",
            "getInstalledApplications",
            "getPackagesHoldingPermissions",
            "queryInstrumentation" -> removeList(method, "API requests", listOf("packageName"))

            "getPackageInfo",
            "getPackageGids",
            "getApplicationInfo",
            "getInstallerPackageName" -> setResult(method, "API requests", null)

            "queryIntentActivities",
            "queryIntentActivityOptions",
            "queryIntentReceivers",
            "queryIntentServices",
            "queryIntentContentProviders" -> removeList(method, "Intent queries", listOf("activityInfo", "packageName"))

            "getPackageUid" -> setResult(method, "ID detections", -1)
            "getPackagesForUid" -> XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val callerUid = Binder.getCallingUid()
                    if (callerUid <= Process.SYSTEM_UID) return
                    val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String
                    val pref = getTemplatePref(callerName)
                    if (!isUseHook(pref, callerName, "ID detections")) return
                    ld("PKMS caller: $callerName")
                    ld("PKMS method: ${param.method.name}")
                    if (param.result != null) {
                        var change = false
                        val list = mutableListOf<String>()
                        for (str in param.result as Array<String>)
                            if (isToHide(pref, callerName, str)) change = true
                            else list.add(str)
                        if (change) param.result = list.toTypedArray()
                    }
                    ld("PKMS dealt")
                }
            })
        }
    }

    private fun removeList(method: Method, hookName: String, pkgNameObjList: List<String>) {
        XposedBridge.hookMethod(method, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val callerUid = Binder.getCallingUid()
                if (callerUid <= Process.SYSTEM_UID) return
                val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String
                val pref = getTemplatePref(callerName)
                if (!isUseHook(pref, callerName, hookName)) return
                ld("PKMS caller: $callerName")
                ld("PKMS method: ${param.method.name}")
                val iterator = (param.result as ParceledListSlice<*>).list.iterator()
                while (iterator.hasNext())
                    if (isToHide(pref, callerName, (getRecursiveField(iterator.next()!!, pkgNameObjList) as String?))) iterator.remove()
                ld("PKMS dealt")
            }
        })
    }

    private fun setResult(method: Method, hookName: String, result: Any?) {
        XposedBridge.hookMethod(method, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val callerUid = Binder.getCallingUid()
                if (callerUid <= Process.SYSTEM_UID) return
                val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String
                if (callerName == APPNAME && param.args[0] == "checkHMAServiceStatus") {
                    param.result = 1
                    return
                }
                val pref = getTemplatePref(callerName)
                if (!isUseHook(pref, callerName, hookName)) return
                ld("PKMS caller: $callerName")
                ld("PKMS method: ${param.method.name}")
                if (isToHide(pref, callerName, param.args[0] as String))
                    param.result = result
                ld("PKMS dealt")
            }
        })
    }
}