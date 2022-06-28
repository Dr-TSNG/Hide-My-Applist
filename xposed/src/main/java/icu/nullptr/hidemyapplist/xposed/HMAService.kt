package icu.nullptr.hidemyapplist.xposed

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.util.Log
import com.github.kyuubiran.ezxhelper.utils.*
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.JsonConfig
import icu.nullptr.hidemyapplist.common.BuildConfig
import icu.nullptr.hidemyapplist.common.IHMAService
import icu.nullptr.hidemyapplist.xposed.Utils.getBinderCaller
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.reflect.Method
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "HMA-Service"

class HMAService(val pms: IPackageManager) : IHMAService.Stub() {

    var hooksInstalled = false

    private lateinit var dataDir: String
    private lateinit var logFile: File
    private lateinit var oldLogFile: File

    private val configLock = Any()
    private val loggerLock = Any()
    private val systemApps = mutableSetOf<String>()
    private val allHooks = mutableSetOf<XC_MethodHook.Unhook>()
    private fun XC_MethodHook.Unhook.yes() { allHooks.add(this) }

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
    }

    fun logD(tag: String, msg: String) = sendLog(Log.DEBUG, tag, msg)
    fun logI(tag: String, msg: String) = sendLog(Log.INFO, tag, msg)
    fun logW(tag: String, msg: String) = sendLog(Log.WARN, tag, msg)
    fun logE(tag: String, msg: String) = sendLog(Log.ERROR, tag, msg)

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
        logFile = File("$dataDir/tmp/runtime.log")
        oldLogFile = File("$dataDir/tmp/old.log")
    }

    private fun loadConfig() {
        filterCount = File("$dataDir/filter_count").readText().toInt()
        val json = File("$dataDir/config.json").readText()
        val loading = runCatching {
            Json.decodeFromString<JsonConfig>(json)
        }.getOrElse {
            logE(TAG, "Failed to parse config.json")
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

        hooksInstalled = true
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
        synchronized(configLock) {
            allHooks.forEach(XC_MethodHook.Unhook::unhook)
            allHooks.clear()
            hooksInstalled = false
        }
        logI(TAG, "Hooks cleared")
    }

    override fun syncConfig(json: String) {
        synchronized(configLock) {
            File("$dataDir/config.json").writeText(json)
            val newConfig = Json.decodeFromString<JsonConfig>(json)
            if (newConfig.configVersion != BuildConfig.SERVICE_VERSION) {
                logW(TAG, "Sync config: version mismatch, need reboot")
                return
            }
        }
        logI(TAG, "Config synced")
    }

    override fun getServiceVersion() = BuildConfig.SERVICE_VERSION

    override fun getFilterCount() = filterCount

    override fun getLogs() = synchronized(loggerLock) { logFile.readText() }

    override fun sendLog(level: Int, tag: String, msg: String) {
        if (level <= Log.DEBUG && !config.detailLog) return
        val levelStr = when (level) {
            Log.DEBUG -> "DEBUG"
            Log.INFO -> " INFO"
            Log.WARN -> " WARN"
            Log.ERROR -> "ERROR"
            else -> "?????"
        }
        val date = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        var fmt = "[$levelStr] $date ($tag) $msg"
        if (!msg.endsWith('\n')) fmt += '\n'
        synchronized(loggerLock) {
            if (logFile.length() / 1024 > config.maxLogSize) clearLogs()
            XposedBridge.log(fmt)
            logFile.appendText(fmt)
        }
    }

    override fun clearLogs() {
        synchronized(loggerLock) {
            oldLogFile.delete()
            logFile.renameTo(oldLogFile)
        }
    }
}
