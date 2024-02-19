package icu.nullptr.hidemyapplist.util

import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.PrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.text.Collator
import java.util.*

object PackageHelper {
    class PackageInfoWithUser(
        val info: PackageInfo,
        val user: Int
    )

    class PackageCache(
        val info: PackageInfo,
        val label: String,
        val icon: Bitmap,
        val user: Int
    )

    class UserInfo (
        val id: Int,
        val name: String,
    )

    private object Comparators {
        val byLabel = Comparator<String> { o1, o2 ->
            val n1 = loadAppLabel(o1).lowercase(Locale.getDefault())
            val n2 = loadAppLabel(o2).lowercase(Locale.getDefault())
            Collator.getInstance(Locale.getDefault()).compare(n1, n2)
        }
        val byPackageName = Comparator<String> { o1, o2 ->
            val n1 = o1.lowercase(Locale.getDefault())
            val n2 = o2.lowercase(Locale.getDefault())
            Collator.getInstance(Locale.getDefault()).compare(n1, n2)
        }
        val byInstallTime = Comparator<String> { o1, o2 ->
            val n1 = loadPackageInfo(o1).firstInstallTime
            val n2 = loadPackageInfo(o2).firstInstallTime
            n2.compareTo(n1)
        }
        val byUpdateTime = Comparator<String> { o1, o2 ->
            val n1 = loadPackageInfo(o1).lastUpdateTime
            val n2 = loadPackageInfo(o2).lastUpdateTime
            n2.compareTo(n1)
        }
    }

    private lateinit var ipm: IPackageManager
    private lateinit var pm: PackageManager

    private val packageCache = MutableSharedFlow<Map<String, PackageCache>>(replay = 1)
    private val mAppList = MutableSharedFlow<List<String>>(replay = 1)
    val appList: SharedFlow<List<String>> = mAppList

    private val mRefreshing = MutableSharedFlow<Boolean>(replay = 1)
    val isRefreshing: SharedFlow<Boolean> = mRefreshing

    init {
        // TODO: PackageManagerDelegate
        pm = hmaApp.packageManager
        invalidateCache()
    }

    fun invalidateCache() {
        hmaApp.globalScope.launch {
            mRefreshing.emit(true)
            val cache = withContext(Dispatchers.IO) {
                // Get packages from all users
                val packages = getInstalledPackagesFromAllUsers()
                mutableMapOf<String, PackageCache>().also {
                    for (packageInfo in packages) {
                        if (packageInfo.info.packageName in Constants.packagesShouldNotHide) continue
                        val label = pm.getApplicationLabel(packageInfo.info.applicationInfo).toString()
                        val icon = hmaApp.appIconLoader.loadIcon(packageInfo.info.applicationInfo)
                        it[packageInfo.info.packageName] = PackageCache(packageInfo.info, label, icon, packageInfo.user)
                    }
                }
            }
            packageCache.emit(cache)
            mAppList.emit(cache.keys.toList())
            mRefreshing.emit(false)
        }
    }

    suspend fun sortList(firstComparator: Comparator<String>) {
        var comparator = when (PrefManager.appFilter_sortMethod) {
            PrefManager.SortMethod.BY_LABEL -> Comparators.byLabel
            PrefManager.SortMethod.BY_PACKAGE_NAME -> Comparators.byPackageName
            PrefManager.SortMethod.BY_INSTALL_TIME -> Comparators.byInstallTime
            PrefManager.SortMethod.BY_UPDATE_TIME -> Comparators.byUpdateTime
        }
        if (PrefManager.appFilter_reverseOrder) comparator = comparator.reversed()
        val list = mAppList.first().sortedWith(firstComparator.then(comparator))
        mAppList.emit(list)
    }

    fun loadPackageInfo(packageName: String): PackageInfo = runBlocking {
        packageCache.first()[packageName]!!.info
    }

    fun loadAppLabel(packageName: String): String = runBlocking {
        packageCache.first()[packageName]!!.label
    }

    fun loadAppIcon(packageName: String): Bitmap = runBlocking {
        packageCache.first()[packageName]!!.icon
    }

    fun isSystem(packageName: String): Boolean = runBlocking {
        packageCache.first()[packageName]!!.info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
    }

    fun shellGetUsers(): List<UserInfo> {
        val users = mutableListOf<UserInfo>()
        val result = Shell.cmd("pm list users").exec()
        if (result.isSuccess) {
            val lines = result.out
            for (line in lines) {
                val trimedLine = line.trim()
                if (trimedLine.startsWith("UserInfo")) {
                    val infos = trimedLine.substringAfter("UserInfo{").substringBefore("}").split(":")
                    val id = infos[0].toInt()
                    val name = infos[1]
                    users.add(UserInfo(id, name))
                }
            }
        }
        return users
    }

    fun grantCrossUserPermissions() {
        SuUtils.execPrivileged("pm grant ${hmaApp.packageName} android.permission.INTERACT_ACROSS_USERS --user 0")
    }

    fun getInstalledPackagesFromUser(user: Int, tryGrantPermission: Boolean = false): List<PackageInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return pm.getInstalledPackages(0)
        }

        try {
            val res = HiddenApiBypass.invoke(PackageManager::class.java, pm, "getInstalledPackagesAsUser", 0, user) as List<*>
            val packages = mutableListOf<PackageInfo>()

            for (i in res.indices) {
                packages += res[i] as PackageInfo
            }

            return packages.toList()
        } catch(e: SecurityException) {
            if (tryGrantPermission) {
                grantCrossUserPermissions()
                return getInstalledPackagesFromUser(user)
            }
            return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun getInstalledPackagesFromAllUsers(deduplicate: Boolean = true): List<PackageInfoWithUser> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return pm.getInstalledPackages(0).map {
                PackageInfoWithUser(it, 0)
            }
        }

        val users = shellGetUsers()

        if (users.isEmpty()) {
            // Fall back to current user
            return pm.getInstalledPackages(0).map {
                PackageInfoWithUser(it, 0)
            }
        }

        val packages = mutableListOf<PackageInfoWithUser>()

        for (user in users) {
            val userPackages = getInstalledPackagesFromUser(user.id)
            for (packageInfo in userPackages) {
                // Deduplicate
                if (deduplicate && packages.any { it.info.packageName == packageInfo.packageName }) continue
                packages += PackageInfoWithUser(packageInfo, user.id)
            }
        }

        return packages.toList()
    }
}
