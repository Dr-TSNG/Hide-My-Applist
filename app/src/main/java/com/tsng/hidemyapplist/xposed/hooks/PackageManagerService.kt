package com.tsng.hidemyapplist.xposed.hooks

import android.content.pm.ParceledListSlice
import android.os.Binder
import android.os.Process
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.APPNAME
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.getRecursiveField
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.li
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
                if (pkgstr.contains(callerName)) return false
                val tplName = scope[callerName] ?: return false
                val template = templates[tplName] ?: return false
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
                        var isHidden = false
                        val iterator = (param.result as ParceledListSlice<*>).list.iterator()
                        while (iterator.hasNext())
                            if (isToHide(callerName, (getRecursiveField(iterator.next()!!, pkgNameObjList) as String?)))
                                iterator.remove().also { isHidden = true }
                        if (isHidden) li("@Hide PKMS caller: $callerName method: ${param.method.name}")
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
                        if (isToHide(callerName, param.args[0] as String)) {
                            param.result = result
                            li("@Hide PKMS caller: $callerName method: ${param.method.name}")
                        }
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
                    val arg = param.args[0] as String
                    when {
                        arg == "checkHMAServiceVersion" -> {
                            param.result = BuildConfig.VERSION_CODE
                            return
                        }
                        arg == "updatePreference" -> {
                            readPreference()
                            param.result = 1
                            return
                        }
                        arg.contains("callIsUseHook") -> {
                            val split = arg.split("#")
                            if (split.size != 3) param.result = 2
                            else param.result = if (isUseHook(split[1], split[2])) 1 else 2
                        }
                        arg.contains("callIsToHide") -> {
                            val split = arg.split("#")
                            if (split.size != 3) param.result = 2
                            else param.result = if (isToHide(split[1], split[2])) 1 else 2
                        }
                    }
                    /* 非服务模式，正常hook */
                    if (!isUseHook(callerName, "ID detections")) return
                    if (isToHide(callerName, param.args[0] as String)) {
                        param.result = -1
                        li("@Hide PKMS caller: $callerName method: ${param.method.name}")
                    }
                }
            }

            /* 载入PackageManagerService */
            override fun afterHookedMethod(param: MethodHookParam) {
                li("System hook installed")
                thread {
                    while (true) {
                        readPreference()
                        Thread.sleep(2000)
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
                            if (param.result != null) {
                                var change = false
                                val list = mutableListOf<String>()
                                for (str in param.result as Array<String>)
                                    if (isToHide(callerName, str)) change = true
                                    else list.add(str)
                                if (change) {
                                    param.result = list.toTypedArray()
                                    li("@Hide PKMS caller: $callerName method: ${param.method.name}")
                                }
                            }
                        }
                    })
                }
            }
        })
    }
}