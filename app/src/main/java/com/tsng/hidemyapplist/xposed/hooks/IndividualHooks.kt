package com.tsng.hidemyapplist.xposed.hooks

import android.app.Application
import android.content.Context
import com.tsng.hidemyapplist.JSONPreference
import com.tsng.hidemyapplist.xposed.XposedUtils
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.li
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.File
import kotlin.concurrent.thread

class IndividualHooks : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        if (lpp.appInfo.isSystemApp) return
        XposedHelpers.findAndHookMethod(Application::class.java, "attach", Context::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val context = param.args[0] as Context
                fileHook(context, lpp.packageName)
                nativeHook(context)
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

    fun nativeHook(context: Context) {
        thread {
            while (true) {

                Thread.sleep(1000)
            }
        }
    }

    external fun nativeBridge(pref: JSONPreference): String
}