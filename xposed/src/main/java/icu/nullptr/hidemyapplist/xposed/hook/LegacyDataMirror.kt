package icu.nullptr.hidemyapplist.xposed.hook

import android.os.Process
import com.github.kyuubiran.ezxhelper.utils.findField
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getObject
import com.github.kyuubiran.ezxhelper.utils.getObjectAs
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.common.LDMP
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI
import java.lang.reflect.Field
import java.lang.reflect.Method

class LegacyDataMirror(private val service: HMAService) : IFrameworkHook {

    companion object {
        private const val TAG = "LegacyDataMirror"
        private const val PACKAGE_SETTINGS_CLASS = "com.android.server.pm.PackageSetting"
        private const val PACKAGE_USER_STATE_CLASS = "android.content.pm.PackageUserState"
    }

    private lateinit var mPackages: Map<String, *>
    private lateinit var readUserStateMethod: Method
    private lateinit var volumeUuidField: Field
    private lateinit var ceDataInodeField: Field

    override fun load() {
        if (!service.config.legacyDataMirror) return
        logI(TAG, "Load hook")
        runCatching {
            mPackages = service.pms.getObject("mSettings").getObjectAs("mPackages")
            readUserStateMethod = findMethod(PACKAGE_SETTINGS_CLASS, findSuper = true) {
                name == "readUserState" && parameterTypes.size == 1
            }
            volumeUuidField = findField(PACKAGE_SETTINGS_CLASS, findSuper = true) { name == "volumeUuid" }
            ceDataInodeField = findField(PACKAGE_USER_STATE_CLASS) { name == "ceDataInode" }

            service.serviceDelegate.getLDMPProxy = this::getLDMP
        }.onFailure {
            logE(TAG, "Fatal error occurred, disable hooks", it)
        }
    }

    override fun unload() {
        service.serviceDelegate.getLDMPProxy = null
    }

    override fun onConfigChanged() {
        if (service.config.legacyDataMirror) load()
        else unload()
    }

    private fun getLDMP(uid: Int): Map<String, LDMP>? {
        runCatching {
            val userId = uid / Constants.PER_USER_RANGE
            val appId = uid % Constants.PER_USER_RANGE
            if (appId !in Process.FIRST_APPLICATION_UID .. Process.LAST_APPLICATION_UID) return null
            val packages = service.pms.getPackagesForUid(uid)
            return packages.associateWith { getPackageLDMP(it, userId) }
        }.onFailure {
            logE(TAG, "Fatal error occurred, disable hooks", it)
            unload()
        }
        return null
    }

    private fun getPackageLDMP(packageName: String, userId: Int): LDMP {
        val ps = mPackages[packageName]
        val state = readUserStateMethod.invoke(ps, userId)
        val volumeUuid = volumeUuidField.get(ps) as String?
        val ceDataInode = ceDataInodeField.get(state) as Long
        return LDMP().apply {
            this.volumeUuid = volumeUuid
            this.inode = ceDataInode
        }
    }
}
