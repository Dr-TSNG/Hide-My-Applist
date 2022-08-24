package icu.nullptr.hidemyapplist.xposed

import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.os.Binder
import android.os.Build
import com.github.kyuubiran.ezxhelper.utils.invokeMethodAutoAs
import de.robv.android.xposed.XposedHelpers
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

    fun getPackageNameFromPackageSettings(packageSettings: Any): String {
        return with(packageSettings.toString()) {
            substring(lastIndexOf(' ') + 1, lastIndexOf('/'))
        }
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
}
