package icu.nullptr.hidemyapplist.xposed.hook

import android.content.ComponentName
import android.content.Intent
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.github.kyuubiran.ezxhelper.utils.invokeMethodAs
import com.github.kyuubiran.ezxhelper.utils.loadClass
import de.robv.android.xposed.XC_MethodHook
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.xposed.HMAService
import icu.nullptr.hidemyapplist.xposed.Utils
import icu.nullptr.hidemyapplist.xposed.Utils.getBinderCaller
import icu.nullptr.hidemyapplist.xposed.logD
import icu.nullptr.hidemyapplist.xposed.logI
import java.lang.reflect.Method

class FrameworkLegacy(private val service: HMAService) : IFrameworkHook {

    companion object {
        private const val TAG = "LegacyHook"
    }

    private val hooks = mutableSetOf<XC_MethodHook.Unhook>()
    private fun XC_MethodHook.Unhook.yes() = hooks.add(this)

    @Suppress("UNCHECKED_CAST")
    private fun removeList(
        method: Method,
        isParceled: Boolean,
        pkgNameObjList: List<String>
    ) = method.hookAfter { param ->
        if (param.hasThrowable()) return@hookAfter
        val caller = param.thisObject.getBinderCaller()
        if (caller !in service.config.scope) return@hookAfter

        var isHidden = false
        val list =
            if (isParceled) param.result.invokeMethodAs<MutableList<Any>>("getList")!!
            else param.result as MutableList<Any>
        val iterator = list.iterator()
        val removed = mutableListOf<String>()

        while (iterator.hasNext()) {
            val pkg = Utils.getRecursiveField(iterator.next(), pkgNameObjList) as String?
            if (service.shouldHide(caller, pkg)) {
                iterator.remove()
                isHidden = true
                if (service.config.detailLog) removed.add(pkg!!)
            }
        }

        if (isHidden) {
            service.filterCount++
            logI(TAG, "@Hide PMS caller: $caller method: ${param.method.name}")
            logD(TAG, "RemoveList $removed")
        }
    }

    private fun setResult(
        method: Method,
        result: Any?
    ) = method.hookAfter { param ->
        val caller = param.thisObject.getBinderCaller()
        if (caller !in service.config.scope) return@hookAfter

        if (service.shouldHide(caller, param.args[0] as String?)) {
            service.filterCount++
            param.result = result
            logI(TAG, "@Hide PMS caller: $caller method: ${param.method.name} param: ${param.args[0]}")
        }
    }

    private fun resolveIntent(
        method: Method,
        result: Any?
    ) = method.hookAfter { param ->
        val caller = param.thisObject.getBinderCaller()
        if (caller !in service.config.scope) return@hookAfter

        when (val it = param.args[0]) {
            is Intent -> listOf(it.action, it.component?.packageName)
            is ComponentName -> listOf(it.packageName, it.className)
            else -> emptyList()
        }.forEach {
            if (service.shouldHide(caller, it)) {
                service.filterCount++
                param.result = result
                logI(TAG, "@Hide PMS caller: $caller method: ${param.method.name} param: ${param.args[0]}")
                return@hookAfter
            }
        }
    }

    override fun load() {
        logI(TAG, "Load hook")
        val pmMethods = buildSet<Method> {
            addAll(service.pms::class.java.declaredMethods)
            val pmsClass = loadClass(Constants.CLASS_PMS)
            if (service.pms::class.java != pmsClass) { // Has custom pms
                val added = mapTo(mutableSetOf()) { it.name }
                pmsClass.declaredMethods.forEach {
                    if (!added.contains(it.name)) add(it)
                }
            }
        }
        for (method in pmMethods) when (method.name) {
            "getAllPackages"
            -> removeList(method, false, listOf()).yes()

            "getInstalledPackages",
            "getInstalledApplications",
            "getPackagesHoldingPermissions",
            "queryInstrumentation"
            -> removeList(method, true, listOf("packageName")).yes()

            "getPackageInfo",
            "getPackageGids",
            "getApplicationInfo",
            "getInstallSourceInfo",
            "getInstallerPackageName",
            "getLaunchIntentForPackage",
            "getLeanbackLaunchIntentForPackage"
            -> setResult(method, null).yes()

            "queryIntentActivities",
            "queryIntentActivityOptions",
            "queryIntentReceivers",
            "queryIntentServices",
            "queryIntentContentProviders"
            -> removeList(method, true, listOf("activityInfo", "packageName")).yes()

            "getActivityInfo",
            "resolveActivity",
            "resolveActivityAsUser"
            -> resolveIntent(method, null).yes()

            "getPackageUid"
            -> setResult(method, -1).yes()
        }
    }

    override fun unHook() = hooks.forEach(XC_MethodHook.Unhook::unhook)
}
