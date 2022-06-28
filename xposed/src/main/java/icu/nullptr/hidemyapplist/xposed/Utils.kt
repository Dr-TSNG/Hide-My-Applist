package icu.nullptr.hidemyapplist.xposed

import android.os.Binder
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
}
