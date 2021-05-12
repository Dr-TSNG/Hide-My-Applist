package com.tsng.hidemyapplist.xposed.hooks

import android.content.pm.ParceledListSlice
import android.os.Binder
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.JsonConfig
import com.tsng.hidemyapplist.xposed.XposedUtils.APPNAME
import com.tsng.hidemyapplist.xposed.XposedUtils.L
import com.tsng.hidemyapplist.xposed.XposedUtils.getRecursiveField
import com.tsng.hidemyapplist.xposed.XposedUtils.resultIllegal
import com.tsng.hidemyapplist.xposed.XposedUtils.resultNo
import com.tsng.hidemyapplist.xposed.XposedUtils.resultYes
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Method
import kotlin.concurrent.thread

class PackageManagerService : IXposedHookLoadPackage {
    private val dataDir = "/data/misc/hide_my_applist"
    private val allHooks = mutableSetOf<XC_MethodHook.Unhook>()
    private var stopped = false
    private var initialized = false
    private var mLock = Any()

    @Volatile
    private var config = JsonConfig()

    @Volatile
    private var configStr = "{}"

    @Volatile
    private var interceptionCount = 0

    fun addLog(log: String) {
        synchronized(mLock) {
            FileWriter("$dataDir/runtime.log", true).use {
                it.appendLine(log)
            }
        }
    }

    private fun provideLogs(): String {
        try {
            synchronized(mLock) {
                if (stopped) throw InterruptedException("Service stopped")
                FileReader("$dataDir/runtime.log").use {
                    val sb = StringBuilder()
                    val list = it.readLines()
                    for (line in list) sb.append(line)
                    return sb.toString()
                }
            }
        } catch (e: Exception) {
            return "Failed to load logs\n" + e.stackTraceToString()
        }
    }

    private fun initConfig() {
        FileReader("$dataDir/config.json").use {
            configStr = it.readText()
            config = JsonConfig.fromJson(configStr)
        }
        if (config.DetailLog) L.d("Cached config: $config", this)
        initialized = true
        try {
            FileReader("$dataDir/interception_cnt").use {
                interceptionCount = it.readText().toInt()
            }
        } catch (e: Exception) { }
        L.i("Config initialized", this)
    }

    private fun updateConfig(json: String) {
        if (configStr == json) return
        configStr = json
        config = JsonConfig.fromJson(json)
        synchronized(mLock) {
            if (stopped) return
            FileWriter("$dataDir/config.json").use {
                it.write(json)
            }
        }
        if (!initialized) initConfig()
        else if (config.DetailLog) L.d("Update config: $config", this)
    }

    private fun isUseHook(callerName: String?, hookMethod: String): Boolean {
        if (callerName == APPNAME && !config.HookSelf) return false
        val tplName = config.Scope[callerName] ?: return false
        val template = config.Templates[tplName] ?: return false
        return template.EnableAllHooks or template.ApplyHooks.contains(hookMethod)
    }

    private fun isToHide(callerName: String?, pkgstr: String?): Boolean {
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

    private fun isHideFile(callerName: String?, path: String?): Boolean {
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

    private fun removeList(method: Method, hookName: String, pkgNameObjList: List<String>) {
        allHooks.add(XposedBridge.hookMethod(method, object : XC_MethodHook() {
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
                if (isHidden) {
                    interceptionCount++
                    L.i("@Hide PKMS caller: $callerName method: ${param.method.name}", this@PackageManagerService)
                }
                if (isHidden && config.DetailLog) L.d("removeList $removed", this@PackageManagerService)
            }
        }))
    }

    private fun setResult(method: Method, hookName: String, result: Any?) {
        allHooks.add(XposedBridge.hookMethod(method, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val callerUid = Binder.getCallingUid()
                val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String?
                if (!isUseHook(callerName, hookName)) return
                if (isToHide(callerName, param.args[0] as String?)) {
                    interceptionCount++
                    param.result = result
                    L.i("@Hide PKMS caller: $callerName method: ${param.method.name} param: ${param.args[0]}", this@PackageManagerService)
                }
            }
        }))
    }

    /* 劫持 getInstallerPackageName 作为通信服务 */
    inner class HMAService : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val callerUid = Binder.getCallingUid()
            val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String?
            val arg = param.args[0] as String? ?: return
            when {
                /* 服务模式，执行自定义行为 */
                arg == "checkHMAServiceVersion" -> param.result = BuildConfig.SERVICE_VERSION.toString()
                arg == "getServeTimes" -> param.result = interceptionCount.toString()
                arg == "getPreference" -> param.result = configStr
                arg == "getLogs" -> param.result = provideLogs()
                arg.startsWith("addLog") -> addLog(arg.substring(7)).also { param.result = resultYes }
                arg.startsWith("stopSystemService") -> {
                    val split = arg.split("#")
                    stopService(split[1] == "true")
                    param.result = resultYes
                }
                arg.startsWith("providePreference") -> {
                    updateConfig(arg.split("#")[1])
                    param.result = resultYes
                }
                arg.startsWith("callIsUseHook") -> {
                    val split = arg.split("#")
                    if (split.size != 3) param.result = resultIllegal
                    else param.result = if (isUseHook(split[1], split[2])) resultYes else resultNo
                }
                arg.startsWith("callIsToHide") -> {
                    val split = arg.split("#")
                    if (split.size != 3) param.result = resultIllegal
                    else param.result = if (isToHide(split[1], split[2])) resultYes else resultNo
                }
                arg.startsWith("callIsHideFile") -> {
                    val split = arg.split("#")
                    if (split.size != 3) param.result = resultIllegal
                    else param.result = if (isHideFile(split[1], split[2])) resultYes else resultNo
                }
                /* 非服务模式，正常hook */
                else -> {
                    if (!isUseHook(callerName, "API requests")) return
                    if (isToHide(callerName, param.args[0] as String?)) {
                        interceptionCount++
                        param.result = null
                        L.i("@Hide PKMS caller: $callerName method: ${param.method.name} param: ${param.args[0]}", this@PackageManagerService)
                    }
                }
            }
        }
    }

    /* Remove all hooks */
    private fun stopService(cleanEnv: Boolean) {
        stopped = true
        L.i("Receive stop system service signal", this)
        synchronized(mLock) {
            if (cleanEnv) {
                L.i("Clean runtime environment", this)
                File(dataDir).deleteRecursively()
            }
        }
        L.i("Start to remove all hooks", this)
        for (hook in allHooks) {
            L.i("Remove hook at ${hook.hookedMethod.name}", this)
            hook.unhook()
        }
        L.i("System service stopped", this)
    }

    /* Load system service */
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        File(dataDir).let { if (!it.exists()) it.mkdir() }
        File("$dataDir/runtime.log").let { if (it.exists()) it.delete() }
        try {
            initConfig()
        } catch (e: FileNotFoundException) {
            L.i("Config not cached, waiting for preference provider", this)
        } catch (e: Exception) {
            configStr = "{}"
            config = JsonConfig()
            L.e("Failed to read cached config, waiting for preference provider\n${e.stackTraceToString()}", this)
        }
        thread {
            while (!stopped) {
                if (initialized) synchronized(mLock) {
                    if (stopped) return@thread
                    FileWriter("$dataDir/interception_cnt").use {
                        it.write(interceptionCount.toString())
                    }
                }
                Thread.sleep(2000)
            }
        }

        val PKMS = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", lpp.classLoader)
        allHooks.addAll(XposedBridge.hookAllConstructors(PKMS, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                L.i("System hook installed (Version ${BuildConfig.SERVICE_VERSION})", this@PackageManagerService)
            }
        }))
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
            allHooks.addAll(XposedBridge.hookAllConstructors(extPKMS, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    L.i("Non-AOSP PKMS ${param.method.declaringClass.name}", this@PackageManagerService)
                }
            }))
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
            "getInstallerPackageName" -> allHooks.add(XposedBridge.hookMethod(method, HMAService()))

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
            "getPackagesForUid" -> allHooks.add(XposedBridge.hookMethod(method, object : XC_MethodHook() {
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
                            interceptionCount++
                            param.result = list.toTypedArray()
                            L.i("@Hide PKMS caller: $callerName method: ${param.method.name}", this@PackageManagerService)
                            if (config.DetailLog) L.d("removeList $removed", this@PackageManagerService)
                        }
                    }
                }
            }))
        }
    }
}