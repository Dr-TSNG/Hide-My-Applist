package com.tsng.hidemyapplist.xposed.hooks;

import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Iterator;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static com.tsng.hidemyapplist.xposed.XposedEntry.APPNAME;
import static com.tsng.hidemyapplist.xposed.XposedEntry.LOG;
import static com.tsng.hidemyapplist.xposed.XposedEntry.getTemplatePref;
import static com.tsng.hidemyapplist.xposed.XposedEntry.isToHide;
import static com.tsng.hidemyapplist.xposed.XposedEntry.isUseHook;

public class PackageManagerService implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(LoadPackageParam lpp) {
        if (!lpp.packageName.equals("android")) return;
        Class PKMS = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", lpp.classLoader);
        for (Method method : PKMS.getDeclaredMethods())
            switch (method.getName()) {
                case "getInstalledPackages":
                case "getInstalledApplications":
                case "getPackagesHoldingPermissions":
                case "queryInstrumentation":
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            String callerName = (String) XposedHelpers.callMethod(param.thisObject, "getNameForUid", Binder.getCallingUid());

                            if (callerName == APPNAME) {
                                Log.d(LOG, "PKMS caller: " + callerName);
                                Log.d(LOG, "PKMS method: " + param.method.getName());
                            }

                            XSharedPreferences pref = getTemplatePref(callerName);
                            if (!isUseHook(pref, callerName, "method_api")) return;
                            for (Iterator iterator = ((ParceledListSlice)param.getResult()).getList().iterator(); iterator.hasNext(); )
                                if (isToHide(pref, callerName, (String) XposedHelpers.getObjectField(iterator.next(), "packageName")))
                                    iterator.remove();
                            Log.d(LOG, "PKMS dealt");
                        }
                    });
                    break;
            }
    }
}