package icu.nullptr.hidemyapplist.xposed.hook

import android.annotation.TargetApi
import android.os.Build
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI

@TargetApi(Build.VERSION_CODES.R)
class FrameworkTarget30(private val service: HMAService) : IFrameworkHook {

    companion object {
        private const val TAG = "FrameworkTarget30"
    }

    private val hooks = mutableSetOf<XC_MethodHook.Unhook>()
    private fun XC_MethodHook.Unhook.yes() = hooks.add(this)

    override fun load() {
        logI(TAG, "Load hook")
        findMethod("com.android.server.pm.AppsFilter") {
            name == "shouldFilterApplication"
        }.hookBefore { param ->
            runCatching {
                val callingUid = Utils.getAppId(param.args[0] as Int)
                val callingApps = service.pms.getPackagesForUid(callingUid) ?: return@hookBefore
                val targetApp = Utils.getPackageNameFromPackageSettings(param.args[2])
                for (caller in callingApps) {
                    if (service.shouldHide(caller, targetApp)) {
                        param.result = true
                        service.filterCount++
                        logI(TAG, "@Filter caller: $caller, target: $targetApp")
                        return@hookBefore
                    }
                }
            }.onFailure {
                logE(TAG, "Fatal error occurred, disable hooks", it)
                unHook()
            }
        }.yes()
    }

    override fun unHook() = hooks.forEach(XC_MethodHook.Unhook::unhook)
}
