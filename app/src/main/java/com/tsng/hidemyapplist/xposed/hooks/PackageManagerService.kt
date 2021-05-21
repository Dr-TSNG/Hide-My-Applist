package com.tsng.hidemyapplist.xposed.hooks

import android.content.pm.ApplicationInfo
import android.content.pm.ParceledListSlice
import android.os.Binder
import android.util.ArrayMap
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.JsonConfig
import com.tsng.hidemyapplist.xposed.XposedUtils.APPNAME
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class PackageManagerService : IXposedHookLoadPackage {
    private val dataDir = "/data/misc/hide_my_applist"
    private val allHooks = mutableSetOf<XC_MethodHook.Unhook>()
    private val systemApps = mutableSetOf<String>()
    private var stopped = false
    private var initialized = false
    private var mLock = Any()

    private lateinit var token: String

    @Volatile
    private var config = JsonConfig()

    @Volatile
    private var configStr = "{}"

    @Volatile
    private var interceptionCount = 0

    private fun ld(log: String) {
        val s = "[HMA Xposed] [DEBUG] $log"
        XposedBridge.log(s)
        addLog(s)
    }

    private fun li(log: String) {
        val s = "[HMA Xposed] [INFO] $log"
        XposedBridge.log(s)
        addLog(s)
    }

    private fun le(log: String) {
        val s = "[HMA Xposed] [ERROR] $log"
        XposedBridge.log(s)
        addLog(s)
    }

    private fun generateToken() {
        val leftLimit = 97 // letter 'a'
        val rightLimit = 122 // letter 'z'
        val targetStringLength = 10
        val random = Random()
        val buffer = StringBuilder(targetStringLength)
        for (i in 0 until targetStringLength) {
            val randomLimitedInt = leftLimit + (random.nextFloat() * (rightLimit - leftLimit + 1)).toInt()
            buffer.append(randomLimitedInt.toChar())
        }
        token = buffer.toString()
        FileWriter("$dataDir/token").use { it.write(token) }
    }

    private fun addLog(log: String) {
        synchronized(mLock) {
            val logFile = File("$dataDir/runtime.log")
            if (logFile.length() / 1024 > config.MaxLogSize) logFile.delete()
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss ", Locale.getDefault()).format(Date())
            FileWriter(logFile, true).use { it.appendLine(date + log) }
        }
    }

    private fun provideLogs(): String {
        try {
            synchronized(mLock) {
                if (stopped) throw InterruptedException("Service stopped")
                FileReader("$dataDir/runtime.log").use {
                    val sb = StringBuilder()
                    val list = it.readLines()
                    for (line in list) sb.append(line).append("\n")
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
        if (config.DetailLog) ld("Cached config: $config")
        initialized = true
        try {
            FileReader("$dataDir/interception_cnt").use {
                interceptionCount = it.readText().toInt()
            }
        } catch (e: Exception) {
        }
        li("Config initialized")
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
        else if (config.DetailLog) ld("Update config: $config")
    }

    private fun isUseHook(callerName: String?, hookMethod: String): Boolean {
        if (callerName == APPNAME && !config.HookSelf) return false
        val tplName = config.Scope[callerName] ?: return false
        val template = config.Templates[tplName] ?: return false
        return template.EnableAllHooks or template.ApplyHooks.contains(hookMethod)
    }

    private fun isToHide(callerName: String?, queryName: String?): Boolean {
        if (callerName == null || queryName == null) return false
        if (callerName in queryName) return false
        val tplName = config.Scope[callerName] ?: return false
        val template = config.Templates[tplName] ?: return false
        if (template.WhiteList && template.ExcludeSystemApps && queryName in systemApps) return false
        val inList = queryName in template.HideApps
        return template.WhiteList xor inList
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
                    li("@Hide PKMS caller: $callerName method: ${param.method.name}")
                }
                if (isHidden && config.DetailLog) ld("removeList $removed")
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
                    li("@Hide PKMS caller: $callerName method: ${param.method.name} param: ${param.args[0]}")
                }
            }
        }))
    }

    /* 劫持 getInstallerPackageName 作为通信服务 */
    inner class HMAService : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val callerUid = Binder.getCallingUid()
            val callerName = XposedHelpers.callMethod(param.thisObject, "getNameForUid", callerUid) as String?
            var arg = param.args[0] as String? ?: return

            /* 非模块调用需要验证token */
            if (callerName != APPNAME) {
                if (!arg.startsWith(token)) {
                    if (!isUseHook(callerName, "API requests")) return
                    if (isToHide(callerName, arg)) {
                        interceptionCount++
                        param.result = null
                        li("@Hide PKMS caller: $callerName method: ${param.method.name} param: ${param.args[0]}")
                    }
                    return
                } else arg = arg.removePrefix("$token#")
            }

            when {
                /* 服务模式，执行自定义行为 */
                arg == "checkHMAServiceVersion" -> param.result = BuildConfig.SERVICE_VERSION.toString()
                arg == "getServeTimes" -> param.result = interceptionCount.toString()
                arg == "getPreference" -> param.result = configStr
                arg == "getLogs" -> param.result = provideLogs()
                arg == "cleanLogs" -> {
                    synchronized(mLock) { File("$dataDir/runtime.log").let { it.delete(); it.createNewFile() } }
                    param.result = resultYes
                }
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
            }
        }
    }

    /* Remove all hooks */
    private fun stopService(cleanEnv: Boolean) {
        stopped = true
        li("Receive stop system service signal")
        li("Start to remove all hooks")
        for (hook in allHooks) {
            li("Remove hook at ${hook.hookedMethod.name}")
            hook.unhook()
        }
        li("System service stopped")
        synchronized(mLock) {
            if (cleanEnv) {
                li("Clean runtime environment")
                File(dataDir).deleteRecursively()
            }
        }
    }

    /* Load system service */
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        File(dataDir).let { if (!it.exists()) it.mkdir() }
        File("$dataDir/runtime.log").let { if (it.exists()) it.delete() }
        generateToken()
        try {
            initConfig()
        } catch (e: FileNotFoundException) {
            li("Config not cached, waiting for preference provider")
        } catch (e: Exception) {
            configStr = "{}"
            config = JsonConfig()
            le("Failed to read cached config, waiting for preference provider\n${e.stackTraceToString()}")
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
                li("System hook installed (Version ${BuildConfig.SERVICE_VERSION})")
                /* Cache system app list */
                val mSettings = XposedHelpers.getObjectField(param.thisObject, "mSettings")
                val mPackages = XposedHelpers.getObjectField(mSettings, "mPackages") as ArrayMap<String, *>
                for ((name, ps) in mPackages) {
                    if (XposedHelpers.getIntField(ps, "pkgFlags") and ApplicationInfo.FLAG_SYSTEM != 0) {
                        systemApps.add(name)
                    }
                }
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
                    li("Non-AOSP PKMS ${param.method.declaringClass.name}")
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
                            li("@Hide PKMS caller: $callerName method: ${param.method.name}")
                            if (config.DetailLog) ld("removeList $removed")
                        }
                    }
                }
            }))
        }
    }
}