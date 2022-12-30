package icu.nullptr.hidemyapplist.xposed

import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.os.Build
import icu.nullptr.hidemyapplist.common.*
import icu.nullptr.hidemyapplist.xposed.hook.*
import java.io.File

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
    private val frameworkHooks = mutableSetOf<IFrameworkHook>()

    var config = JsonConfig().apply { detailLog = true }
        private set

    var filterCount = 0
        @JvmName("getFilterCountInternal") get
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
        instance = this
        loadConfig()
        installHooks()
        logI(TAG, "HMA service initialized")
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
        logFile.createNewFile()

        logcatAvailable = true
        logI(TAG, "Data dir: $dataDir")
    }

    private fun loadConfig() {
        File("$dataDir/filter_count").also {
            runCatching {
                if (it.exists()) filterCount = it.readText().toInt()
            }.onFailure { e ->
                logW(TAG, "Failed to load filter count, set to 0", e)
                it.writeText("0")
            }
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
        if (loading.configVersion != BuildConfig.CONFIG_VERSION) {
            logW(TAG, "Config version mismatch, need to reload")
            return
        }
        config = loading
        logI(TAG, "Config loaded")
    }

    private fun installHooks() {
        Utils.getInstalledApplicationsCompat(pms, 0, 0).mapNotNullTo(systemApps) {
            if (it.flags and ApplicationInfo.FLAG_SYSTEM != 0) it.packageName else null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            frameworkHooks.add(PmsHookTarget33(this))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            frameworkHooks.add(PmsHookTarget30(this))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            frameworkHooks.add(PmsHookTarget28(this))
        } else {
            frameworkHooks.add(PmsHookLegacy(this))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            frameworkHooks.add(ZygoteArgsHook(this))
        }

        frameworkHooks.forEach(IFrameworkHook::load)
        logI(TAG, "Hooks installed")
    }

    fun isHookEnabled(packageName: String) = config.scope.containsKey(packageName)

    fun shouldHide(caller: String?, query: String?): Boolean {
        if (caller == null || query == null) return false
        if (caller in Constants.packagesShouldNotHide || query in Constants.packagesShouldNotHide) return false
        if ((caller == Constants.GMS_PACKAGE_NAME || caller == Constants.GSF_PACKAGE_NAME) && query == Constants.APP_PACKAGE_NAME) return false // If apply hide on gms, hma app will crash 😓
        if (caller in query) return false
        val appConfig = config.scope[caller] ?: return false
        if (appConfig.useWhitelist && appConfig.excludeSystemApps && query in systemApps) return false

        if (query in appConfig.extraAppList) return !appConfig.useWhitelist
        for (tplName in appConfig.applyTemplates) {
            val tpl = config.templates[tplName]!!
            if (query in tpl.appList) return !appConfig.useWhitelist
        }

        return appConfig.useWhitelist
    }

    override fun stopService(cleanEnv: Boolean) {
        logI(TAG, "Stop service")
        synchronized(loggerLock) {
            logcatAvailable = false
        }
        synchronized(configLock) {
            frameworkHooks.forEach(IFrameworkHook::unload)
            frameworkHooks.clear()
            if (cleanEnv) {
                logI(TAG, "Clean runtime environment")
                File(dataDir).deleteRecursively()
                return
            }
        }
        instance = null
    }

    fun addLog(parsedMsg: String) {
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
            if (newConfig.configVersion != BuildConfig.CONFIG_VERSION) {
                logW(TAG, "Sync config: version mismatch, need reboot")
                return
            }
            config = newConfig
            frameworkHooks.forEach(IFrameworkHook::onConfigChanged)
        }
        logD(TAG, "Config synced")
    }

    override fun getServiceVersion() = BuildConfig.SERVICE_VERSION

    override fun getFilterCount() = filterCount

    override fun getLogs() = synchronized(loggerLock) {
        logFile.readText()
    }

    override fun clearLogs() {
        synchronized(loggerLock) {
            oldLogFile.delete()
            logFile.renameTo(oldLogFile)
            logFile.createNewFile()
        }
    }
}
