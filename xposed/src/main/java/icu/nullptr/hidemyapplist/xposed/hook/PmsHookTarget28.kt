package icu.nullptr.hidemyapplist.xposed.hook

import android.annotation.TargetApi
import android.content.pm.ResolveInfo
import android.os.Build
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
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

    private val hooks = mutableListOf<XC_MethodHook.Unhook>()

    @Suppress("UNCHECKED_CAST")
    override fun load() {
        logI(TAG, "Load hook")
        hooks += findMethod(service.pms::class.java, findSuper = true) {
            name == "filterAppAccessLPr" && parameterCount == 5
        }.hookBefore { param ->
            runCatching {
                val callingUid = param.args[1] as Int
                if (callingUid == Constants.UID_SYSTEM) return@hookBefore
                val callingApps = Utils.binderLocalScope {
                    service.pms.getPackagesForUid(callingUid)
                } ?: return@hookBefore
                val packageSettings = param.args[0] ?: return@hookBefore
                val targetApp = Utils.getPackageNameFromPackageSettings(packageSettings)
                for (caller in callingApps) {
                    if (service.shouldHide(caller, targetApp)) {
                        param.result = true
                        service.filterCount++
                        logI(TAG, "@filterAppAccessLPr caller: $callingUid $caller, target: $targetApp")
                        return@hookBefore
                    }
                }
            }.onFailure {
                logE(TAG, "Fatal error occurred, disable hooks", it)
                unload()
            }
        }
        hooks += findMethod(service.pms::class.java, findSuper = true) {
            name == "applyPostResolutionFilter"
        }.hookAfter { param ->
            runCatching {
                val callingUid = param.args[3] as Int
                if (callingUid == Constants.UID_SYSTEM) return@hookAfter
                val callingApps = Utils.binderLocalScope {
                    service.pms.getPackagesForUid(callingUid)
                } ?: return@hookAfter
                val list = param.result as MutableCollection<ResolveInfo>
                val listToRemove = mutableSetOf<ResolveInfo>()
                for (resolveInfo in list) {
                    val targetApp = with(resolveInfo) {
                        activityInfo?.packageName ?: serviceInfo?.packageName ?: providerInfo?.packageName ?: resolvePackageName
                    }
                    for (caller in callingApps) {
                        if (service.shouldHide(caller, targetApp)) {
                            logI(TAG, "@applyPostResolutionFilter caller: $callingUid $caller, target: $targetApp")
                            listToRemove.add(resolveInfo)
                            break
                        }
                    }
                }
                list.removeAll(listToRemove)
            }.onFailure {
                logE(TAG, "Fatal error occurred, disable hooks", it)
                unload()
            }
        }
    }

    override fun unload() {
        hooks.forEach(XC_MethodHook.Unhook::unhook)
        hooks.clear()
    }
}
