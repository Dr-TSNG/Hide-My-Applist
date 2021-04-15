package com.tsng.hidemyapplist.xposed.hooks

import android.app.Application
import android.content.Context
import com.tsng.hidemyapplist.xposed.XposedUtils
import com.tsng.hidemyapplist.xposed.XposedUtils.Companion.li
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.File

class IndividualHooks : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpp: LoadPackageParam) {
        if (lpp.appInfo.isSystemApp) return
        XposedHelpers.findAndHookMethod(Application::class.java, "attach", Context::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val context = param.args[0] as Context
                fileHook(context, lpp.packageName)
            }
        })
    }

    fun fileHook(context: Context, pkgName: String) {
        XposedHelpers.findAndHookConstructor(File::class.java, String::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val path = param.args[0] as String
                if (path.contains("Android/data"))
                    if (XposedUtils.callServiceIsUseHook(context, pkgName, "File detections"))
                        if (XposedUtils.callServiceIsToHide(context, pkgName, path)) {
                            param.args[0] = "fuck/there/is/no/file"
                            li("@Hide javaFile caller: $pkgName")
                        }
            }
        })
    }
}