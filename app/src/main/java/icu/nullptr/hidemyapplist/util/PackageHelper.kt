package icu.nullptr.hidemyapplist.util

import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import icu.nullptr.hidemyapplist.hmaApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PackageHelper {

    class PackageCache(
        val info: PackageInfo,
        val label: String
    )

    private lateinit var ipm: IPackageManager
    private lateinit var pm: PackageManager

    private val _packageCache = MutableSharedFlow<Map<String, PackageCache>>(replay = 1)
    val packageCache: SharedFlow<Map<String, PackageCache>> = _packageCache

    private val _isRefreshing = MutableSharedFlow<Boolean>(replay = 1)
    val isRefreshing: SharedFlow<Boolean> = _isRefreshing

    init {
        // TODO: PackageManagerDelegate
        pm = hmaApp.packageManager
        invalidateCache()
    }

    fun invalidateCache() {
        hmaApp.globalScope.launch {
            _isRefreshing.emit(true)
            val cache = withContext(Dispatchers.IO) {
                val packages = pm.getInstalledPackages(0)
                mutableMapOf<String, PackageCache>().also {
                    for (packageInfo in packages) {
                        val label = pm.getApplicationLabel(packageInfo.applicationInfo).toString()
                        it[packageInfo.packageName] = PackageCache(packageInfo, label)
                    }
                }
            }
            _packageCache.emit(cache)
            _isRefreshing.emit(false)
        }
    }

    fun loadPackageInfo(packageName: String): PackageInfo {
        return packageCache.replayCache[0][packageName]!!.info
    }

    fun loadAppLabel(packageName: String): String {
        return packageCache.replayCache[0][packageName]!!.label
    }

    fun isSystem(packageName: String): Boolean {
        return packageCache.replayCache[0][packageName]!!.info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }
}
