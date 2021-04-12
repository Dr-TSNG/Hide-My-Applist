package com.tsng.hidemyapplist.xposed.hooks

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import com.tsng.hidemyapplist.xposed.XposedBase
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.*
import java.nio.charset.StandardCharsets

class IndividualHooks : IXposedHookLoadPackage, XposedBase() {
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        if (lpp.packageName == APPNAME)
            if(!XSharedPreferences(APPNAME, "Settings").getBoolean("HookSelf", false))
                return
        val pref: XSharedPreferences = getTemplatePref(lpp.packageName) ?: return
        val enable_all_hooks = pref.getBoolean("EnableAllHooks", false)
        val enabled = pref.getStringSet("ApplyHooks", null)
        if (enable_all_hooks || enabled.contains("API requests")) pmHook(lpp, pref)
        if (enable_all_hooks || enabled.contains("API requests")) apiHook(lpp, pref)
        if (enable_all_hooks || enabled.contains("Intent queries")) intentHook(lpp, pref)
        if (enable_all_hooks || enabled.contains("ID detections")) uidHook(lpp, pref)
        if (enable_all_hooks || enabled.contains("File detections")) fileHook(lpp, pref)
    }

    fun pmHook(lpp: LoadPackageParam, pref: XSharedPreferences?) {
        XposedHelpers.findAndHookMethod(Runtime::class.java, "exec", String::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    XposedHelpers.findAndHookMethod(param.result.javaClass, "getInputStream", object : XC_MethodHook() {
                        @Throws(Throwable::class)
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val br = BufferedReader(InputStreamReader(param.result as InputStream, StandardCharsets.UTF_8))
                            var line: String?
                            val sb = StringBuilder()
                            while (br.readLine().also { line = it } != null) if (!isToHide(pref, lpp.packageName, line!!)) sb.append(line).append("\n")
                            val result: InputStream = ByteArrayInputStream(sb.toString().toByteArray(StandardCharsets.UTF_8))
                            param.result = result
                        }
                    })
                } catch (e: Throwable) {
                    le("hooking Runtime.exec ERROR")
                }
            }
        })
    }

    fun apiHook(lpp: LoadPackageParam, pref: XSharedPreferences?) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpp.classLoader, "getInstalledPackages", Int::class.javaPrimitiveType, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val packageInfos: MutableList<PackageInfo> = param.result as MutableList<PackageInfo>
                val iterator = packageInfos.iterator()
                while (iterator.hasNext()) {
                    if (isToHide(pref, lpp.packageName, iterator.next().packageName)) iterator.remove()
                }
                param.result = packageInfos
            }
        })
    }

    fun intentHook(lpp: LoadPackageParam, pref: XSharedPreferences?) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpp.classLoader, "queryIntentActivities", Intent::class.java, Int::class.javaPrimitiveType, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val infos: MutableList<ResolveInfo> = param.result as MutableList<ResolveInfo>
                val iterator = infos.iterator()
                while (iterator.hasNext()) {
                    if (isToHide(pref, lpp.packageName, iterator.next().activityInfo.packageName)) iterator.remove()
                }
                param.result = infos
            }
        })
    }

    fun uidHook(lpp: LoadPackageParam, pref: XSharedPreferences?) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpp.classLoader, "getPackagesForUid", Int::class.javaPrimitiveType, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val list = param.result as Array<String>
                for (pkg in list) if (isToHide(pref, lpp.packageName, pkg)) {
                    param.result = null
                    break
                }
            }
        })
    }

    fun fileHook(lpp: LoadPackageParam, pref: XSharedPreferences) {
        XposedHelpers.findAndHookConstructor(File::class.java, String::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val path = param.args[0] as String
                if (path.contains(lpp.packageName)) return
                if (pref.getBoolean("HideAllApps", false) && path.contains("Android/data/")) {
                    param.args[0] = "fuck/there/is/no/file"
                    return
                }
                for (pkg in pref.getStringSet("HideApps", null)) if (path.contains(pkg!!)) {
                    param.args[0] = "fuck/there/is/no/file"
                    break
                }
            }
        })
    }
}