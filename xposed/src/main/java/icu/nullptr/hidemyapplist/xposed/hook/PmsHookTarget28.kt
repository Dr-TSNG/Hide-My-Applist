package icu.nullptr.hidemyapplist.xposed.hook

import android.annotation.TargetApi
import android.os.Build
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils
import icu.nullptr.hidemyapplist.xposed.logE
import icu.nullptr.hidemyapplist.xposed.logI

@TargetApi(Build.VERSION_CODES.P)
class PmsHookTarget28(private val service: HMAService) : IFrameworkHook {

    companion object {
        private const val TAG = "PmsHookTarget28"
    }

    private var hook: XC_MethodHook.Unhook? = null

    override fun load() {
        logI(TAG, "Load hook")
        hook = findMethod(service.pms::class.java, findSuper = true) {
            name == "filterAppAccessLPr"
        }.hookBefore { param ->
            runCatching {
                val callingUid = param.args[1] as Int
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore
                val callingApps = Utils.binderLocalScope {
                    service.pms.getPackagesForUid(callingUid)
                } ?: return@hookBefore
                val targetApp = Utils.getPackageNameFromPackageSettings(param.args[0])
                for (caller in callingApps) {
                    if (service.shouldHide(caller, targetApp)) {
                        param.result = true
                        service.filterCount++
                        logI(TAG, "@Filter caller: $callingUid $caller, target: $targetApp")
                        return@hookBefore
                    }
                }
            }.onFailure {
                logE(TAG, "Fatal error occurred, disable hooks", it)
                unload()
            }
        }
    }

    override fun unload() {
        hook?.unhook()
        hook = null
    }
}
