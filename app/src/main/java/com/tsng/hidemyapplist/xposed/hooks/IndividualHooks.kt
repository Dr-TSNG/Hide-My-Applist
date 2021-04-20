package com.tsng.hidemyapplist.xposed.hooks

import android.app.Application
import android.content.Context
import com.tsng.hidemyapplist.xposed.XposedEntry.Companion.modulePath
import com.tsng.hidemyapplist.xposed.XposedUtils
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.ld
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.le
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.li
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.File
import kotlin.concurrent.thread

class IndividualHooks : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        if (lpp.appInfo == null || lpp.appInfo.isSystemApp) return
        var loadedNativeLib = false
        try {
            System.load(modulePath.substring(0, modulePath.lastIndexOf('/'))
                    + if (android.os.Process.is64Bit()) "/lib/arm64/libnative_hooks.so" else "/lib/arm/libnative_hooks.so")
            loadedNativeLib = true
        } catch (e: Throwable) {
            le("Load native_hooks library failed | caller: ${lpp.packageName}")
        }
        XposedHelpers.findAndHookMethod(Application::class.java, "attach", Context::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val context = param.args[0] as Context
                if (loadedNativeLib) nativeHook(context, lpp.packageName)
                else fileHook(context, lpp.packageName)
            }
        })
    }

    fun fileHook(context: Context, pkgName: String) {
        XposedHelpers.findAndHookConstructor(File::class.java, String::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (XposedUtils.callServiceIsUseHook(context, pkgName, "File detections"))
                    if (XposedUtils.callServiceIsToHide(context, pkgName, param.args[0] as String?, true)) {
                        li("@Hide javaFile caller: $pkgName param: ${param.args[0]}")
                        param.args[0] = "fuck/there/is/no/file"
                    }
            }
        })
    }

    fun nativeHook(context: Context, pkgName: String) {
        initNative(pkgName)
        thread {
            while (true) {
                val json = XposedUtils.getServicePreference(context)
                if (json != null) {
                    var last = "/"
                    val messages = nativeBridge(json)
                    val iterator = messages.iterator()
                    while (iterator.hasNext()) {
                        when (val str = iterator.next()) {
                            "DEBUG" -> last = "d"
                            "INFO" -> last = "i"
                            "ERROR" -> last = "e"
                            else -> when (last) {
                                "d" -> ld(str)
                                "i" -> li(str)
                                "e" -> le(str)
                            }
                        }
                    }
                }
                Thread.sleep(1000)
            }
        }
    }

    private external fun initNative(pkgName: String)
    private external fun nativeBridge(json: String): Array<String>
}