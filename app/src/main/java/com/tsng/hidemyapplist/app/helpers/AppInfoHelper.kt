package com.tsng.hidemyapplist.app.helpers

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import com.tsng.hidemyapplist.app.MyApplication

object AppInfoHelper {
    data class MyAppInfo(
        val appName: String,
        val packageName: String,
        val icon: Drawable,
        val isSystemApp: Boolean
    )

    private lateinit var mAppInfoList: MutableList<MyAppInfo>

    fun getAppInfoList(): MutableList<MyAppInfo> {
        if (AppInfoHelper::mAppInfoList.isInitialized) return mAppInfoList.toMutableList()
        mAppInfoList = mutableListOf()
        val pm = MyApplication.appContext.packageManager
        for (appInfo in pm.getInstalledApplications(0)) {
            mAppInfoList.add(
                MyAppInfo(
                    appInfo.loadLabel(pm).toString(),
                    appInfo.packageName,
                    appInfo.loadIcon(pm),
                    appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                )
            )
        }
        return mAppInfoList.toMutableList()
    }
}