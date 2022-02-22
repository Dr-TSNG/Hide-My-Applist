/*
 * This file is part of Hide My Applist.
 *
 * Hide My Applist is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with Hide My Applist.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2021 Hide My Applist Contributors
 */

package com.tsng.hidemyapplist.xposed

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.github.kyuubiran.ezxhelper.utils.*
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.JsonConfig
import com.tsng.hidemyapplist.xposed.ServiceUtils.getBinderCaller
import com.tsng.hidemyapplist.xposed.ServiceUtils.getRecursiveField
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.io.File
import java.io.FileNotFoundException
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

object PackageManagerService {
    private const val hmaApp = "com.tsng.hidemyapplist"

    private val customPms = arrayOf(
        "com.android.server.pm.OplusPackageManagerService",
        "com.android.server.pm.OppoPackageManagerService"
    )

    private val allHooks = mutableSetOf<XC_MethodHook.Unhook>()
    private val systemApps = mutableSetOf<String>()
    private var stopped = false
    private var configCached = false
    private var extensionVersion = 0
    private var mLock = Any()

    private lateinit var dataDir: String
    private lateinit var logFile: File
    private lateinit var token: String

    @Volatile
    private var config = JsonConfig()

    @Volatile
    private var configStr = JsonConfig().toString()

    @Volatile
    private var interceptionCount = 0

    private object Log {
        fun d(msg: String) {
            if (!config.detailLog) return
            val s = "[HMA Xposed] [DEBUG] $msg"
            XposedBridge.log(s)
            addLog(s)
        }

        fun i(msg: String) {
            val s = "[HMA Xposed] [INFO] $msg"
            XposedBridge.log(s)
            addLog(s)
        }

        fun e(msg: String) {
            val s = "[HMA Xposed] [ERROR] $msg"
            XposedBridge.log(s)
            addLog(s)
        }
    }

    private fun generateRandomString(length: Int): String {
        val leftLimit = 97   // letter 'a'
        val rightLimit = 122 // letter 'z'
        val random = Random()
        val buffer = StringBuilder(length)
        for (i in 0 until length) {
            val randomLimitedInt = leftLimit + (random.nextFloat() * (rightLimit - leftLimit + 1)).toInt()
            buffer.append(randomLimitedInt.toChar())
        }
        return buffer.toString()
    }

    private fun generateToken() {
        token = generateRandomString(10)
        File("$dataDir/tmp/token").writeText(token)
    }

    private fun addLog(log: String) {
        synchronized(mLock) {
            if (logFile.length() / 1024 > config.maxLogSize) logFile.delete()
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss ", Locale.getDefault()).format(Date())
            logFile.appendText(date + log + '\n')
        }
    }

    private fun provideLogs(): String {
        try {
            synchronized(mLock) {
                if (stopped) throw InterruptedException("Service stopped")
                val sb = StringBuilder()
                val list = logFile.readLines()
                for (line in list) sb.append(line).append("\n")
                return sb.toString()
            }
        } catch (e: Exception) {
            return "Failed to load logs\n" + e.stackTraceToString()
        }
    }

    private fun initConfig() {
        configStr = File("$dataDir/config.json").readText()
        config = JsonConfig.fromJson(configStr)
        if (config.configVersion < BuildConfig.SERVICE_VERSION) {
            config = JsonConfig()
            Log.i("Config cache version too old, need refresh")
            return
        }
        Log.d("Cached config: $config")
        configCached = true
        try {
            interceptionCount = File("$dataDir/interception_cnt").readText().toInt()
        } catch (e: Exception) {
        }
        Log.i("Config initialized")
    }

    private fun updateConfig(json: String) {
        if (configStr == json) return
        configStr = json
        config = JsonConfig.fromJson(json)
        synchronized(mLock) {
            if (stopped) return
            File("$dataDir/config.json").writeText(json)
        }
        if (!configCached) initConfig()
        else Log.d("Update config: $config")
    }

    private fun isUseHook(caller: String?, hookMethod: String): Boolean {
        val appConfig = config.scope[caller] ?: return false
        return appConfig.enableAllHooks || appConfig.applyHooks.contains(hookMethod)
    }

    private fun isToHide(caller: String?, query: String?): Boolean {
        if (caller == null || query == null) return false
        if (caller in query) return false
        val appConfig = config.scope[caller] ?: return false
        if (appConfig.useWhitelist && appConfig.excludeSystemApps && query in systemApps) return false

        if (query in appConfig.extraAppList || query in appConfig.extraQueryParamRules)
            return !appConfig.useWhitelist
        for (tplName in appConfig.applyTemplates) {
            val tpl = config.templates[tplName]!!
            if (query in tpl.appList || query in tpl.queryParamRules)
                return !appConfig.useWhitelist
        }

        return appConfig.useWhitelist
    }

    private fun removeList(
        method: Method,
        isParceled: Boolean,
        hookMethod: String,
        pkgNameObjList: List<String>
    ) {
        allHooks.add(method.hookAfter { param ->
            if (param.hasThrowable()) return@hookAfter
            val caller = param.thisObject.getBinderCaller()
            if (!isUseHook(caller, hookMethod)) return@hookAfter

            var isHidden = false
            val list =
                if (isParceled) param.result.invokeMethodAs<MutableList<Any>>("getList")!!
                else param.result as MutableList<Any>
            val iterator = list.iterator()
            val removed = mutableListOf<String>()

            while (iterator.hasNext()) {
                val pkg = getRecursiveField(iterator.next(), pkgNameObjList) as String?
                if (isToHide(caller, pkg)) {
                    iterator.remove()
                    isHidden = true
                    if (config.detailLog) removed.add(pkg!!)
                }
            }

            if (isHidden) {
                interceptionCount++
                Log.i("@Hide PMS caller: $caller method: ${param.method.name}")
                Log.d("removeList $removed")
            }
        })
    }

    private fun setResult(method: Method, hookMethod: String, result: Any?) {
        allHooks.add(method.hookAfter { param ->
            val caller = param.thisObject.getBinderCaller()
            if (!isUseHook(caller, hookMethod)) return@hookAfter

            if (isToHide(caller, param.args[0] as String?)) {
                interceptionCount++
                param.result = result
                Log.i("@Hide PMS caller: $caller method: ${param.method.name} param: ${param.args[0]}")
            }
        })
    }

    private fun resolveIntent(method: Method, hookMethod: String, result: Any?) {
        allHooks.add(method.hookAfter { param ->
            val caller = param.thisObject.getBinderCaller()
            if (!isUseHook(caller, hookMethod)) return@hookAfter

            when (val it = param.args[0]) {
                is Intent -> it.component?.packageName
                is ComponentName -> it.packageName
                else -> null
            }?.let {
                if (isToHide(caller, it)) {
                    interceptionCount++
                    param.result = result
                    Log.i("@Hide PMS caller: $caller method: ${param.method.name} param: ${param.args[0]}")
                    return@hookAfter
                }
            }
        })
    }

    /* Hijack getInstallerPackageName as communication service */
    object HMAService : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val caller = param.thisObject.getBinderCaller()
            var arg = param.args[0] as String? ?: return

            /* Non module calls require token validation */
            if (caller != hmaApp) {
                if (!arg.startsWith(token)) {
                    if (!isUseHook(caller, "API requests")) return
                    if (isToHide(caller, arg)) {
                        interceptionCount++
                        param.result = null
                        Log.i("@Hide PMS caller: $caller method: ${param.method.name} param: ${param.args[0]}")
                    }
                    return
                } else arg = arg.removePrefix("$token#")
            }

            when {
                arg == "getServiceVersion" ->
                    param.result = BuildConfig.SERVICE_VERSION.toString()

                arg == "getExtensionVersion" ->
                    param.result = extensionVersion.toString()

                arg == "getServeTimes" ->
                    param.result = interceptionCount.toString()

                arg == "getLogs" ->
                    param.result = provideLogs()

                arg == "cleanLogs" -> {
                    synchronized(mLock) { logFile.apply { delete(); createNewFile() } }
                    param.result = "OK"
                }

                arg.startsWith("addLog") -> {
                    addLog(arg.substring(7))
                    param.result = "OK"
                }

                arg.startsWith("submitConfig") -> {
                    updateConfig(arg.split("#")[1])
                    param.result = "OK"
                }

                arg.startsWith("stopSystemService") -> {
                    val split = arg.split("#")
                    stopService(split[1] == "true")
                    param.result = "OK"
                }
            }
        }
    }

    /* Remove all hooks */
    private fun stopService(cleanEnv: Boolean) {
        stopped = true
        File("$dataDir/tmp/ext_run").delete()
        Log.i("Receive stop system service signal")
        Log.i("Start to remove all hooks")
        for (hook in allHooks) {
            Log.i("Remove hook at ${hook.hookedMethod.name}")
            hook.unhook()
        }
        Log.i("System service stopped")
        synchronized(mLock) {
            if (cleanEnv) {
                Log.i("Clean runtime environment")
                File(dataDir).deleteRecursively()
            }
        }
    }

    private fun syncWithExtension() {
        /* If extension not installed, make tmp by the service */
        File("$dataDir/tmp/ext_ver").apply {
            if (exists()) {
                var minApkVersion: Int
                try {
                    val lines = readLines()
                    extensionVersion = lines[0].toInt()
                    minApkVersion = lines[1].toInt()
                } catch (e: Exception) {
                    extensionVersion = 0
                    minApkVersion = 0
                }
                if (extensionVersion < BuildConfig.MIN_EXTENSION_VERSION) {
                    extensionVersion = -1
                    File("$dataDir/tmp/ext_run").delete()
                    Log.e("Magisk extension version too old to work with the new system service")
                }
                if (BuildConfig.VERSION_CODE < minApkVersion) {
                    extensionVersion = -2
                    File("$dataDir/tmp/ext_run").delete()
                    Log.e("System service version too old to work with the new Magisk extension")
                }
                delete()
            } else File("$dataDir/tmp").apply {
                deleteRecursively()
                mkdirs()
            }
        }
    }

    private fun searchDataDir() {
        File("/data/misc/hide_my_applist").deleteRecursively()
        File("/data/system").list()?.forEach {
            if (it.startsWith("hide_my_applist")) {
                if (this::dataDir.isInitialized) File("/data/system/$it").deleteRecursively()
                else dataDir = "/data/system/$it"
            }
        }
        if (!this::dataDir.isInitialized) {
            dataDir = "/data/system/hide_my_applist_" + generateRandomString(16)
        }
        logFile = File("$dataDir/tmp/runtime.log")
    }

    /* Load system service */
    fun entry() {
        searchDataDir()
        syncWithExtension()
        generateToken()
        try {
            initConfig()
        } catch (e: FileNotFoundException) {
            Log.i("Config not cached, waiting for preference provider")
        } catch (e: Exception) {
            config = JsonConfig()
            configStr = config.toString()
            Log.e("Failed to read cached config, waiting for preference provider\n${e.stackTraceToString()}")
        }
        thread {
            while (!stopped) {
                if (configCached) synchronized(mLock) {
                    if (stopped) return@thread
                    File("$dataDir/interception_cnt").writeText(interceptionCount.toString())
                }
                Thread.sleep(2000)
            }
        }

        val pms = loadClass("com.android.server.pm.PackageManagerService")
        allHooks.addAll(pms.hookAllConstructorAfter { param ->
            /* Cache system app list */
            val mSettings = param.thisObject.getObject("mSettings")
            val mPackages = mSettings.getObjectAs<Map<String, *>>("mPackages")
            for ((name, ps) in mPackages) {
                if (ps != null && (ps.getObjectAs<Int>("pkgFlags") and ApplicationInfo.FLAG_SYSTEM != 0)) {
                    systemApps.add(name)
                }
            }
            for (pkg in systemApps)
                File("$dataDir/tmp/system_apps.list").appendText("$pkg\n")

            Log.i("System hook installed (Version ${BuildConfig.SERVICE_VERSION})")
            Log.i("Data directory is at $dataDir")
        })

        /* ---Deal with 💩 ROMs--- */
        var extPms: Class<*>? = null
        for (clazz in customPms) {
            try {
                extPms = loadClass(clazz)
                break
            } catch (e: ClassNotFoundException) {
            }
        }
        val pmMethods = mutableSetOf<Method>()
        val methodNames = mutableSetOf<String>()
        if (extPms != null) {
            allHooks.addAll(extPms.hookAllConstructorAfter { param ->
                Log.i("Non-AOSP PMS ${param.method.declaringClass.name}")
            })
            for (method in extPms.declaredMethods) {
                pmMethods.add(method)
                methodNames.add(method.name)
            }
        }
        /* ----------------------- */
        for (method in pms.declaredMethods)
            if (method.name !in methodNames)
                pmMethods.add(method)

        for (method in pmMethods) when (method.name) {
            "getInstallerPackageName"
            -> allHooks.add(XposedBridge.hookMethod(method, HMAService))

            "getAllPackages"
            -> removeList(method, false, "API requests", listOf())

            "getInstalledPackages",
            "getInstalledApplications",
            "getPackagesHoldingPermissions",
            "queryInstrumentation"
            -> removeList(method, true, "API requests", listOf("packageName"))

            "getPackageInfo",
            "getPackageGids",
            "getApplicationInfo",
            "getInstallSourceInfo",
            "getLaunchIntentForPackage",
            "getLeanbackLaunchIntentForPackage"
            -> setResult(method, "API requests", null)

            "queryIntentActivities",
            "queryIntentActivityOptions",
            "queryIntentReceivers",
            "queryIntentServices",
            "queryIntentContentProviders"
            -> removeList(method, true, "Intent queries", listOf("activityInfo", "packageName"))

            "getActivityInfo",
            "resolveActivity",
            "resolveActivityAsUser"
            -> resolveIntent(method, "Intent queries", null)

            "getPackageUid"
            -> setResult(method, "ID detections", -1)

            "getPackagesForUid"
            -> allHooks.add(method.hookAfter { param ->
                if (param.hasThrowable()) return@hookAfter
                val caller = param.thisObject.getBinderCaller()
                if (!isUseHook(caller, "ID detections")) return@hookAfter
                if (param.result != null) {
                    var change = false
                    val list = mutableListOf<String>()
                    val removed = mutableListOf<String>()
                    for (str in param.result as Array<String>)
                        if (isToHide(caller, str)) {
                            change = true
                            if (config.detailLog) removed.add(str)
                        } else list.add(str)
                    if (change) {
                        interceptionCount++
                        param.result = list.toTypedArray()
                        Log.i("@Hide PMS caller: $caller method: ${param.method.name}")
                        Log.d("removeList $removed")
                    }
                }
            })
        }
    }
}
