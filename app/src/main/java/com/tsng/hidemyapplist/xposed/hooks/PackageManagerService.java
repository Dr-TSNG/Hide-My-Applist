package com.tsng.hidemyapplist.xposed.hooks;

import android.os.Binder;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import com.tsng.hidemyapplist.xposed.XposedEntry;

public class PackageManagerService implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(LoadPackageParam lpp) {
        if(!lpp.packageName.equals("android"))return;
        Class PMS = XposedHelpers.findClass("com.android.server.pm.PackageManagerService", lpp.classLoader);
        XC_MethodHook apiHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param){
                String callerName = (String) XposedHelpers.callMethod(param.thisObject, "getNameForUid", Binder.getCallingUid());
                Log.d(XposedEntry.LOG, "caller: " + callerName);
            }
        };
    }
}