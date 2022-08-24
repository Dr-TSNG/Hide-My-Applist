package icu.nullptr.hidemyapplist.xposed

import android.content.pm.IPackageManager
import android.os.ServiceManager
import android.util.Log
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.getFieldByDesc
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorAfter
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.callbacks.XC_LoadPackage
import icu.nullptr.hidemyapplist.common.Constants
import kotlin.concurrent.thread

private const val TAG = "HMA-XposedEntry"

private fun waitSystemService(name: String) {
    while (ServiceManager.getService(name) == null) {
        try {
            logD(TAG, "service $name is not started, wait 1s")
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            logE(TAG, Log.getStackTraceString(e))
        }
    }
}

class XposedEntry : IXposedHookZygoteInit, IXposedHookLoadPackage {

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelperInit.initZygote(startupParam)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == Constants.APP_PACKAGE_NAME) {
            EzXHelperInit.initHandleLoadPackage(lpparam)
            hookAllConstructorAfter("icu.nullptr.hidemyapplist.MyApp") {
                getFieldByDesc("Licu/nullptr/hidemyapplist/MyApp;->isHooked:Z").setBoolean(it.thisObject, true)
            }
        } else if (lpparam.packageName == "android") {
            EzXHelperInit.initHandleLoadPackage(lpparam)
            logI(TAG, "Hook entry")

            var pms: IPackageManager? = null
            var serviceManagerHook: XC_MethodHook.Unhook? = null
            serviceManagerHook = findMethod("android.os.ServiceManager") {
                name == "addService"
            }.hookAfter { param ->
                if (param.args[0] == "package") {
                    pms = param.args[1] as IPackageManager
                    logI(TAG, "Got pms: $pms")
                    serviceManagerHook?.unhook()
                }
            }

            logD(TAG, "ServiceManager hook installed")
            thread {
                runCatching {
                    waitSystemService("package")
                    logI(TAG, "PackageManagerService ready, load bridge service")
                    if (pms != null) {
                        BridgeService.start(pms!!)
                    } else {
                        logE(TAG, "Package service started, but instance is not captured")
                    }
                }.onFailure {
                    logE(TAG, "System service crashed", it)
                }
            }
        }
    }
}
