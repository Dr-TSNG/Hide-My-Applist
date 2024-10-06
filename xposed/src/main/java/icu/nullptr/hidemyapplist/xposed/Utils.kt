package icu.nullptr.hidemyapplist.xposed

import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageInfo
import android.os.Binder
import android.os.Build
import com.android.apksig.ApkVerifier
import com.github.kyuubiran.ezxhelper.utils.findField
import com.github.kyuubiran.ezxhelper.utils.invokeMethodAutoAs
import de.robv.android.xposed.XposedHelpers
import icu.nullptr.hidemyapplist.Magic
import java.io.File
import java.util.*

object Utils {

    fun generateRandomString(length: Int): String {
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

    fun verifyAppSignature(path: String): Boolean {
        val verifier = ApkVerifier.Builder(File(path))
            .setMinCheckedPlatformVersion(24)
            .build()
        val result = verifier.verify()
        if (!result.isVerified) return false
        val mainCert = result.signerCertificates[0]
        return mainCert.encoded.contentEquals(Magic.magicNumbers)
    }

    fun getRecursiveField(entry: Any, list: List<String>): Any? {
        var field: Any? = entry
        for (it in list)
            field = XposedHelpers.getObjectField(field, it) ?: return null
        return field
    }

    fun Any.getBinderCaller(): String? {
        return this.invokeMethodAutoAs<String>("getNameForUid", Binder.getCallingUid())
    }

    fun <T> binderLocalScope(block: () -> T): T {
        val identity = Binder.clearCallingIdentity()
        val result = block()
        Binder.restoreCallingIdentity(identity)
        return result
    }

    fun getPackageNameFromPackageSettings(packageSettings: Any): String? {
        return runCatching {
            findField(packageSettings::class.java, true) {
                name == if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "mName" else "name"
            }.get(packageSettings) as? String
        }.getOrNull()
    }

    fun getInstalledApplicationsCompat(pms: IPackageManager, flags: Long, userId: Int): List<ApplicationInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pms.getInstalledApplications(flags, userId)
        } else {
            pms.getInstalledApplications(flags.toInt(), userId)
        }.list
    }

    fun getPackageUidCompat(pms: IPackageManager, packageName: String, flags: Long, userId: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pms.getPackageUid(packageName, flags, userId)
        } else {
            pms.getPackageUid(packageName, flags.toInt(), userId)
        }
    }

    fun getPackageInfoCompat(pms: IPackageManager, packageName: String, flags: Long, userId: Int): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pms.getPackageInfo(packageName, flags, userId)
        } else {
            pms.getPackageInfo(packageName, flags.toInt(), userId)
        }
    }
}
