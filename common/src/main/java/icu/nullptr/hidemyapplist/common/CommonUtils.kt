package icu.nullptr.hidemyapplist.common

import android.os.SystemProperties

object CommonUtils {

    val isAppDataIsolationEnabled: Boolean
        get() = SystemProperties.getBoolean(Constants.ANDROID_APP_DATA_ISOLATION_ENABLED_PROPERTY, true)

    val isVoldAppDataIsolationEnabled: Boolean
        get() = SystemProperties.getBoolean(Constants.ANDROID_VOLD_APP_DATA_ISOLATION_ENABLED_PROPERTY, false)
}
