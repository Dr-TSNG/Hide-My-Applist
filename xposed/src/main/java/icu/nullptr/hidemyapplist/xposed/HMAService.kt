package icu.nullptr.hidemyapplist.xposed

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.util.Log
import com.github.kyuubiran.ezxhelper.utils.*
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.common.BuildConfig
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.IHMAService
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.xposed.Utils.getBinderCaller
import java.io.File
import java.lang.reflect.Method

class HMAService(val pms: IPackageManager) : IHMAService.Stub() {

    companion object {
        private const val TAG = "HMA-Service"
        var instance: HMAService? = null
    }

    @Volatile
    var logcatAvailable = false

    private lateinit var dataDir: String
    private lateinit var configFile: File
    private lateinit var logFile: File
    private lateinit var oldLogFile: File

    private val configLock = Any()
    private val loggerLock = Any()
    private val systemApps = mutableSetOf<String>()
    private val allHooks = mutableSetOf<XC_MethodHook.Unhook>()
    private fun XC_MethodHook.Unhook.yes() = allHooks.add(this)

    private var config = JsonConfig()
    private var filterCount = 0
        set(value) {
            field = value
            if (field % 100 == 0) {
                synchronized(configLock) {
                    File("$dataDir/filter_count").writeText(field.toString())
                }
            }
        }

    init {
        searchDataDir()
        loadConfig()
        installHooks()
        instance = this
        logI(TAG, "HMAService initialized")
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
            dataDir = "/data/system/hide_my_applist_" + Utils.generateRandomString(16)
        }

        File("$dataDir/log").mkdirs()
        configFile = File("$dataDir/config.json")
        logFile = File("$dataDir/log/runtime.log")
        oldLogFile = File("$dataDir/log/old.log")
        logFile.renameTo(oldLogFile)

        logcatAvailable = true
        logI(TAG, "Data dir: $dataDir")
    }

    private fun loadConfig() {
        File("$dataDir/filter_count").also {
            if (it.exists()) filterCount = it.readText().toInt()
        }
        if (!configFile.exists()) {
            logI(TAG, "Config file not found")
            return
        }
        val loading = runCatching {
            val json = configFile.readText()
            JsonConfig.parse(json)
        }.getOrElse {
            logE(TAG, "Failed to parse config.json", it)
            return
        }
        if (loading.configVersion != BuildConfig.SERVICE_VERSION) {
            logW(TAG, "Config version mismatch, need to reload")
            return
        }
        config = loading
        logI(TAG, "Config loaded")
    }

    private fun installHooks() {
        val mSettings = pms.findFieldObject(findSuper = true) { name == "mSettings" }
        val mPackages = mSettings.getObjectAs<Map<String, *>>("mPackages")
        for ((name, ps) in mPackages) {
            if (ps != null && (ps.getObjectAs<Int>("pkgFlags") and ApplicationInfo.FLAG_SYSTEM != 0)) {
                systemApps.add(name)
            }
        }

        val pmMethods = buildSet<Method> {
            addAll(pms::class.java.declaredMethods)
            val pmsClass = loadClass(Constants.CLASS_PMS)
            if (pms::class.java != pmsClass) { // Has custom pms
                val added = mapTo(mutableSetOf()) { it.name }
                pmsClass.declaredMethods.forEach {
                    if (!added.contains(it.name)) add(it)
                }
            }
        }
        for (method in pmMethods) when (method.name) {
            "getAllPackages"
            -> removeList(method, false, "API requests", listOf()).yes()

            "getInstalledPackages",
            "getInstalledApplications",
            "getPackagesHoldingPermissions",
            "queryInstrumentation"
            -> removeList(method, true, "API requests", listOf("packageName")).yes()

            "getPackageInfo",
            "getPackageGids",
            "getApplicationInfo",
            "getInstallSourceInfo",
            "getInstallerPackageName",
            "getLaunchIntentForPackage",
            "getLeanbackLaunchIntentForPackage"
            -> setResult(method, "API requests", null).yes()

            "queryIntentActivities",
            "queryIntentActivityOptions",
            "queryIntentReceivers",
            "queryIntentServices",
            "queryIntentContentProviders"
            -> removeList(method, true, "Intent queries", listOf("activityInfo", "packageName")).yes()

            "getActivityInfo",
            "resolveActivity",
            "resolveActivityAsUser"
            -> resolveIntent(method, "Intent queries", null).yes()

            "getPackageUid"
            -> setResult(method, "ID detections", -1).yes()
        }

        logI(TAG, "Hooks installed")
    }

    private fun shouldHook(caller: String?, hookMethod: String): Boolean {
        val appConfig = config.scope[caller] ?: return false
        return appConfig.enableAllHooks || appConfig.applyHooks.contains(hookMethod)
    }

    private fun shouldHide(caller: String?, query: String?): Boolean {
        if (caller == null || query == null) return false
        if (caller in query) return false
        val appConfig = config.scope[caller] ?: return false
        if (appConfig.useWhitelist && appConfig.excludeSystemApps && query in systemApps) return false

        if (query in appConfig.extraAppList || query in appConfig.extraQueryParamRules) {
            return !appConfig.useWhitelist
        }
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
    ) = method.hookAfter { param ->
        if (param.hasThrowable()) return@hookAfter
        val caller = param.thisObject.getBinderCaller()
        if (!shouldHook(caller, hookMethod)) return@hookAfter

        var isHidden = false
        val list =
            if (isParceled) param.result.invokeMethodAs<MutableList<Any>>("getList")!!
            else param.result as MutableList<Any>
        val iterator = list.iterator()
        val removed = mutableListOf<String>()

        while (iterator.hasNext()) {
            val pkg = Utils.getRecursiveField(iterator.next(), pkgNameObjList) as String?
            if (shouldHide(caller, pkg)) {
                iterator.remove()
                isHidden = true
                if (config.detailLog) removed.add(pkg!!)
            }
        }

        if (isHidden) {
            filterCount++
            logI(TAG, "@Hide PMS caller: $caller method: ${param.method.name}")
            logD(TAG, "RemoveList $removed")
        }
    }

    private fun setResult(
        method: Method,
        hookMethod: String,
        result: Any?
    ) = method.hookAfter { param ->
        val caller = param.thisObject.getBinderCaller()
        if (!shouldHook(caller, hookMethod)) return@hookAfter

        if (shouldHide(caller, param.args[0] as String?)) {
            filterCount++
            param.result = result
            logI(TAG, "@Hide PMS caller: $caller method: ${param.method.name} param: ${param.args[0]}")
        }
    }

    private fun resolveIntent(
        method: Method,
        hookMethod: String,
        result: Any?
    ) = method.hookAfter { param ->
        val caller = param.thisObject.getBinderCaller()
        if (!shouldHook(caller, hookMethod)) return@hookAfter

        when (val it = param.args[0]) {
            is Intent -> listOf(it.action, it.component?.packageName)
            is ComponentName -> listOf(it.packageName, it.className)
            else -> emptyList()
        }.forEach {
            if (shouldHide(caller, it)) {
                filterCount++
                param.result = result
                logI(TAG, "@Hide PMS caller: $caller method: ${param.method.name} param: ${param.args[0]}")
                return@hookAfter
            }
        }
    }

    override fun stopService(cleanEnv: Boolean) {
        logI(TAG, "Stop service")
        synchronized(loggerLock) {
            logcatAvailable = false
        }
        synchronized(configLock) {
            allHooks.forEach(XC_MethodHook.Unhook::unhook)
            allHooks.clear()
            if (cleanEnv) {
                logI(TAG, "Clean runtime environment")
                File(dataDir).deleteRecursively()
                return
            }
        }
        instance = null
    }

    fun addLog(level: Int, parsedMsg: String) {
        if (level <= Log.DEBUG && !config.detailLog) return
        synchronized(loggerLock) {
            if (!logcatAvailable) return
            if (logFile.length() / 1024 > config.maxLogSize) clearLogs()
            logFile.appendText(parsedMsg)
        }
    }

    override fun syncConfig(json: String) {
        synchronized(configLock) {
            configFile.writeText(json)
            val newConfig = JsonConfig.parse(json)
            if (newConfig.configVersion != BuildConfig.SERVICE_VERSION) {
                logW(TAG, "Sync config: version mismatch, need reboot")
                return
            }
            config = newConfig
        }
        logD(TAG, "Config synced")
    }

    override fun getServiceVersion() = BuildConfig.SERVICE_VERSION

    override fun getFilterCount() = filterCount

    override fun getLogs() = synchronized(loggerLock) { logFile.readText() }

    override fun clearLogs() {
        synchronized(loggerLock) {
            oldLogFile.delete()
            logFile.renameTo(oldLogFile)
        }
    }
}
