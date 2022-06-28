package icu.nullptr.hidemyapplist.xposed

import android.content.pm.IPackageManager
import android.os.ServiceManager
import android.util.Log
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.hookAllConstructorAfter
import com.github.kyuubiran.ezxhelper.utils.loadClass
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import icu.nullptr.hidemyapplist.common.Constants
import kotlin.concurrent.thread

private const val TAG = "HMA-XposedEntry"

private fun waitSystemService(name: String) {
    while (ServiceManager.getService(name) == null) {
        try {
            Log.i(TAG, "service $name is not started, wait 1s.")
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            Log.i(TAG, Log.getStackTraceString(e))
        }
    }
}

class XposedEntry : IXposedHookZygoteInit, IXposedHookLoadPackage {
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        EzXHelperInit.initZygote(startupParam)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "android") {
            EzXHelperInit.initHandleLoadPackage(lpparam)

            var pms: IPackageManager? = null
            val pmsClass = loadClass(Constants.CLASS_PMS)
            pmsClass.hookAllConstructorAfter { param ->
                pms = param.thisObject as IPackageManager
                Log.d(TAG, "Got pms: $pms")
            }
            Constants.CLASS_EXT_PMS.forEach {
                runCatching {
                    hookAllConstructorAfter(it) { param ->
                        pms = param.thisObject as IPackageManager
                        Log.d(TAG, "Got custom pms: $pms")
                    }
                }
            }
            thread {
                waitSystemService("package")
                if (pms != null) {
                    BridgeService.start(pms!!)
                } else {
                    Log.e(TAG, "Package service started, but instance is not captured")
                }
            }
        }
    }
}
