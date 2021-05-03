package com.tsng.hidemyapplist.xposed.hooks

import android.content.pm.ParceledListSlice
import android.os.Binder
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.JsonConfig
import com.tsng.hidemyapplist.xposed.XposedUtils.APPNAME
import com.tsng.hidemyapplist.xposed.XposedUtils.getRecursiveField
import com.tsng.hidemyapplist.xposed.XposedUtils.ld
import com.tsng.hidemyapplist.xposed.XposedUtils.li
import com.tsng.hidemyapplist.xposed.XposedUtils.resultIllegal
import com.tsng.hidemyapplist.xposed.XposedUtils.resultNo
import com.tsng.hidemyapplist.xposed.XposedUtils.resultYes
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.lang.reflect.Method

class PackageManagerService : IXposedHookLoadPackage {
    companion object {
        var initialized = false

        @Volatile
        var config = JsonConfig()

        fun updateConfig(str: String) {
            config = JsonConfig.fromJson(str)
            if (config.DetailLog) ld("Receive json: $config")
            if (!initialized) {
                initialized = true
                li("Preference initialized")
            }
        }

        fun isUseHook(callerName: String?, hookMethod: String): Boolean {
            if (callerName == APPNAME && !config.HookSelf) return false
            val tplName = config.Scope[callerName] ?: return false
            val template = config.Templates[tplName] ?: return false
            return template.EnableAllHooks or template.ApplyHooks.contains(hookMethod)
        }

        fun isToHide(callerName: String?, pkgstr: String?): Boolean {
            if (callerName == null || pkgstr == null) return false
            if (callerName in pkgstr) return false
            val tplName = config.Scope[callerName] ?: return false
            val template = config.Templates[tplName] ?: return false
            if (template.ExcludeWebview && pkgstr.contains(Regex("[Ww]ebview"))) return false
            if (template.HideAllApps) return true
            for (pkg in template.HideApps)
                if (pkg in pkgstr) return true
            return false
        }

        fun isHideFile(callerName: String?, path: String?): Boolean {
            if (callerName == null || path == null) return false
            if (callerName in path) return false
            val tplName = config.Scope[callerName] ?: return false
            val template = config.Templates[tplName] ?: return false
            if (template.ExcludeWebview && path.contains(Regex("[Ww]ebview"))) return false
            if (template.HideTWRP && path.contains(Regex("/storage/emulated/(.*)/TWRP"))) return true
            if (template.HideAllApps && path.contains(Regex("/storage/emulated/(.*)/Android/data/"))) return true
            for (pkg in template.HideApps)
                if (pkg in path) return true
            return false
        }

        fun removeList(method: Method, hookName: String, pkgNameObjList: List<String>) {
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val callerUid = Binder.getCallingUid()
                    val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String?
                    if (!isUseHook(callerName, hookName)) return
                    var isHidden = false
                    val iterator = (param.result as ParceledListSlice<*>).list.iterator()
                    val removed = mutableListOf<String>()
                    while (iterator.hasNext()) {
                        val str = getRecursiveField(iterator.next(), pkgNameObjList) as String?
                        if (isToHide(callerName, str)) {
                            iterator.remove()
                            isHidden = true
                            if (config.DetailLog) removed.add(str!!)
                        }
                    }
                    if (isHidden) li("@Hide PKMS caller: $callerName method: ${param.method.name}")
                    if (isHidden && config.DetailLog) ld("removeList $removed")
                }
            })
        }

        fun setResult(method: Method, hookName: String, result: Any?) {
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val callerUid = Binder.getCallingUid()
                    val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String?
                    if (!isUseHook(callerName, hookName)) return
                    if (isToHide(callerName, param.args[0] as String?)) {
                        param.result = result
                        li("@Hide PKMS caller: $callerName method: ${param.method.name} param: ${param.args[0]}")
                    }
                }
            })
        }

        /* 劫持 getInstallerPackageName 作为通信服务 */
        class HMAService : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val callerUid = Binder.getCallingUid()
                val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String?
                val arg = param.args[0] as String? ?: return
                when {
                    /* 服务模式，执行自定义行为 */
                    arg == "checkHMAServiceVersion" -> param.result = BuildConfig.VERSION_CODE.toString()
                    arg == "getPreference" -> param.result = config.toString()
                    arg.contains("providePreference") -> {
                        updateConfig(arg.split("#")[1])
                        param.result = resultYes
                    }
                    arg.contains("callIsUseHook") -> {
                        val split = arg.split("#")
                        if (split.size != 3) param.result = resultIllegal
                        else param.result = if (isUseHook(split[1], split[2])) resultYes else resultNo
                    }
                    arg.contains("callIsToHide") -> {
                        val split = arg.split("#")
                        if (split.size != 3) param.result = resultIllegal
                        else param.result = if (isToHide(split[1], split[2])) resultYes else resultNo
                    }
                    arg.contains("callIsHideFile") -> {
                        val split = arg.split("#")
                        if (split.size != 3) param.result = resultIllegal
                        else param.result = if (isHideFile(split[1], split[2])) resultYes else resultNo
                    }
                    /* 非服务模式，正常hook */
                    else -> {
                        if (!isUseHook(callerName, "API requests")) return
                        if (isToHide(callerName, param.args[0] as String?)) {
                            param.result = null
                            li("@Hide PKMS caller: $callerName method: ${param.method.name} param: ${param.args[0]}")
                        }
                    }
                }
            }
        }
    }

    /* Load system service */
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        if (lpp.packageName != "android") return
        val PKMS = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", lpp.classLoader)
        XposedBridge.hookAllConstructors(PKMS, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                li("System hook installed (Version ${BuildConfig.VERSION_CODE})")
                li("Waiting for preference provider")
            }
        })
        /* ---Deal with 💩 ROMs--- */
        val extPKMS = try {
            when (android.os.Build.BRAND) {
                "Oppo",
                "realme" -> XposedHelpers.findClass("com.android.server.pm.OppoPackageManagerService", lpp.classLoader)
                else -> null
            }
        } catch (e: XposedHelpers.ClassNotFoundError) {
            null
        }
        val pmMethods = mutableSetOf<Method>()
        val methodNames = mutableSetOf<String>()
        if (extPKMS != null) {
            XposedBridge.hookAllConstructors(extPKMS, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    li("Non-AOSP PKMS ${param.method.declaringClass.name}")
                }
            })
            for (method in extPKMS.declaredMethods) {
                pmMethods.add(method)
                methodNames.add(method.name)
            }
        }
        /* ----------------------- */
        for (method in PKMS.declaredMethods)
            if (method.name !in methodNames)
                pmMethods.add(method)
        for (method in pmMethods) when (method.name) {
            "getInstallerPackageName" -> XposedBridge.hookMethod(method, HMAService())

            "getInstalledPackages",
            "getInstalledApplications",
            "getPackagesHoldingPermissions",
            "queryInstrumentation" -> removeList(method, "API requests", listOf("packageName"))

            "getPackageInfo",
            "getPackageGids",
            "getApplicationInfo" -> setResult(method, "API requests", null)

            "queryIntentActivities",
            "queryIntentActivityOptions",
            "queryIntentReceivers",
            "queryIntentServices",
            "queryIntentContentProviders" -> removeList(method, "Intent queries", listOf("activityInfo", "packageName"))

            "getPackageUid" -> setResult(method, "ID detections", -1)
            "getPackagesForUid" -> XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val callerUid = Binder.getCallingUid()
                    val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String?
                    if (!isUseHook(callerName, "ID detections")) return
                    if (param.result != null) {
                        var change = false
                        val list = mutableListOf<String>()
                        val removed = mutableListOf<String>()
                        for (str in param.result as Array<String>)
                            if (isToHide(callerName, str)) {
                                change = true
                                if (config.DetailLog) removed.add(str)
                            } else list.add(str)
                        if (change) {
                            param.result = list.toTypedArray()
                            li("@Hide PKMS caller: $callerName method: ${param.method.name}")
                            if (config.DetailLog) ld("removeList $removed")
                        }
                    }
                }
            })
        }
    }
}