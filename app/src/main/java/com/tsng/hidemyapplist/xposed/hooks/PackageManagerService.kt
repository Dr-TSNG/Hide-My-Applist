package com.tsng.hidemyapplist.xposed.hooks

import android.content.pm.ParceledListSlice
import android.os.Binder
import android.os.Process
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.APPNAME
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.getRecursiveField
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.ld
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.lang.reflect.Method
import kotlin.concurrent.thread

class PackageManagerService : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        if (lpp.packageName != "android") return
        val PKMS = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", lpp.classLoader)
        XposedBridge.hookAllConstructors(PKMS, object : XC_MethodHook() {
            /* 建立Preference缓存 */
            inner class Template(pref: XSharedPreferences) {
                val enableAllHooks = pref.getBoolean("EnableAllHooks", false)
                val applyHooks = pref.getStringSet("ApplyHooks", setOf())
                val hideAllApps = pref.getBoolean("HideAllApps", false)
                val hideApps = pref.getStringSet("HideApps", setOf())
                val excludeSelf = pref.getBoolean("ExcludeSelf", false)
            }

            var hookSelf = false
            var templates: MutableMap<String, Template> = mutableMapOf()
            var scope: Map<String, String> = mapOf()

            fun readPreference() {
                scope = XSharedPreferences(APPNAME, "Scope").all as Map<String, String>
                hookSelf = XSharedPreferences(APPNAME, "Settings").getBoolean("HookSelf", false)
                for (tpl in XSharedPreferences(APPNAME, "Templates").getStringSet("List", setOf()))
                    templates[tpl] = Template(XSharedPreferences(APPNAME, "tpl_$tpl"))
            }

            fun isUseHook(callerName: String?, hookMethod: String): Boolean {
                if (callerName == APPNAME && !hookSelf) return false
                val tplName = scope[callerName] ?: return false
                val template = templates[tplName] ?: return false
                return template.enableAllHooks or template.applyHooks.contains(hookMethod)
            }

            fun isToHide(callerName: String?, pkgstr: String?): Boolean {
                if (callerName == null || pkgstr == null) return false
                val tplName = scope[callerName] ?: return false
                val template = templates[tplName] ?: return false
                if (pkgstr.contains(callerName) && template.excludeSelf) return false
                if (template.hideAllApps) return true
                for (pkg in template.hideApps)
                    if (pkgstr.contains(pkg)) return true
                return false
            }

            /* Hook操作 */
            fun removeList(method: Method, hookName: String, pkgNameObjList: List<String>) {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val callerUid = Binder.getCallingUid()
                        if (callerUid < Process.FIRST_APPLICATION_UID) return
                        val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String
                        if (!isUseHook(callerName, hookName)) return
                        ld("PKMS caller: $callerName")
                        ld("PKMS method: ${param.method.name}")
                        val iterator = (param.result as ParceledListSlice<*>).list.iterator()
                        while (iterator.hasNext())
                            if (isToHide(callerName, (getRecursiveField(iterator.next()!!, pkgNameObjList) as String?))) iterator.remove()
                        ld("PKMS dealt")
                    }
                })
            }

            fun setResult(method: Method, hookName: String, result: Any?) {
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val callerUid = Binder.getCallingUid()
                        if (callerUid < Process.FIRST_APPLICATION_UID) return
                        val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String
                        if (!isUseHook(callerName, hookName)) return
                        ld("PKMS caller: $callerName")
                        ld("PKMS method: ${param.method.name}")
                        if (isToHide(callerName, param.args[0] as String))
                            param.result = result
                        ld("PKMS dealt")
                    }
                })
            }

            /* hook getPackageUid作为通信服务 */
            inner class HMAService : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val callerUid = Binder.getCallingUid()
                    if (callerUid < Process.FIRST_APPLICATION_UID) return
                    val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String
                    /* 服务模式，执行自定义行为 */
                    if (callerName == APPNAME) when (param.args[0]) {
                        "checkHMAServiceVersion" -> {
                            param.result = BuildConfig.VERSION_CODE
                            return
                        }
                        "updatePreference" -> {
                            readPreference()
                            param.result = 1
                            return
                        }
                    }
                    /* 非服务模式，正常hook */
                    if (!isUseHook(callerName, "ID detections")) return
                    ld("PKMS caller: $callerName")
                    ld("PKMS method: ${param.method.name}")
                    if (isToHide(callerName, param.args[0] as String))
                        param.result = -1
                    ld("PKMS dealt")
                }
            }

            /* 载入PackageManagerService */
            override fun afterHookedMethod(param: MethodHookParam) {
                ld("System hook loaded")
                thread {
                    while (true) {
                        readPreference()
                        Thread.sleep(1000)
                    }
                }
                for (method in PKMS.declaredMethods) when (method.name) {
                    "getPackageUid" -> XposedBridge.hookMethod(method, HMAService())

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

                    "getPackagesForUid" -> XposedBridge.hookMethod(method, object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val callerUid = Binder.getCallingUid()
                            if (callerUid < Process.FIRST_APPLICATION_UID) return
                            val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String
                            if (!isUseHook(callerName, "ID detections")) return
                            ld("PKMS caller: $callerName")
                            ld("PKMS method: ${param.method.name}")
                            if (param.result != null) {
                                var change = false
                                val list = mutableListOf<String>()
                                for (str in param.result as Array<String>)
                                    if (isToHide(callerName, str)) change = true
                                    else list.add(str)
                                if (change) param.result = list.toTypedArray()
                            }
                            ld("PKMS dealt")
                        }
                    })
                }
            }
        })
    }
}