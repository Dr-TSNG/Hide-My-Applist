package com.tsng.hidemyapplist.xposed

import android.os.Binder
import com.github.kyuubiran.ezxhelper.utils.invokeMethodAutoAs
import de.robv.android.xposed.XposedHelpers

object ServiceUtils {
    @JvmStatic
    fun getRecursiveField(entry: Any, list: List<String>): Any? {
        var field: Any? = entry
        for (it in list)
            field = XposedHelpers.getObjectField(field, it) ?: return null
        return field
    }

    @JvmStatic
    fun Any.getBinderCaller(): String? {
        return this.invokeMethodAutoAs<String>("getNameForUid", Binder.getCallingUid())
    }
}