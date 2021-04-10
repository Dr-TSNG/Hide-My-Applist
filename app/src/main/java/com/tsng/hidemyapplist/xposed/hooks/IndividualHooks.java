package com.tsng.hidemyapplist.xposed.hooks;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static com.tsng.hidemyapplist.xposed.XposedEntry.LOG;
import static com.tsng.hidemyapplist.xposed.XposedEntry.APPNAME;

public class IndividualHooks implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(final LoadPackageParam lpp) {
        if (lpp.packageName.equals(APPNAME)) {
            XposedHelpers.findAndHookMethod("com.tsng.hidemyapplist.ui.xposed.XposedFragment", lpp.classLoader, "getXposedStatus", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(true);
                }
            });
            if (!new XSharedPreferences(APPNAME, APPNAME + "_preferences").getBoolean("HookSelf", false))
                return;
        }
        //判断模块是否生效

        XSharedPreferences pref = new XSharedPreferences(APPNAME, "Scope");
        final String template = pref.getString(lpp.packageName, null);
        if (template == null) return;
        //判断是否hook该应用

        pref = new XSharedPreferences(APPNAME, "tpl_" + template);
        final boolean enable_all_hooks = pref.getBoolean("EnableAllHooks", false);
        final Set<String> enabled = pref.getStringSet("ApplyHooks", null);
        //获取模板

        if (enable_all_hooks || enabled.contains("method_pm")) pmHook(lpp, pref);
        if (enable_all_hooks || enabled.contains("method_api")) apiHook(lpp, pref);
        if (enable_all_hooks || enabled.contains("method_intent")) intentHook(lpp, pref);
        if (enable_all_hooks || enabled.contains("method_uid")) uidHook(lpp, pref);
        if (enable_all_hooks || enabled.contains("method_datafile")) fileHook(lpp, pref);
        //下钩子
    }

    boolean isToHide(final XSharedPreferences pref, String lppname, String pkgstr) {
        if (pref.getBoolean("ExcludeSelf", false) && pkgstr.contains(lppname)) return false;
        if (pref.getBoolean("HideAllApps", false)) return true;
        Set<String> set = pref.getStringSet("HideApps", new HashSet<>());
        for (String pkg : set)
            if (pkgstr.contains(pkg))
                return true;
        return false;
    }

    void pmHook(final LoadPackageParam lpp, final XSharedPreferences pref) {
        XposedHelpers.findAndHookMethod(Runtime.class, "exec", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    XposedHelpers.findAndHookMethod(param.getResult().getClass(), "getInputStream", new XC_MethodHook() {
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            BufferedReader br = new BufferedReader(new InputStreamReader((InputStream) param.getResult(), StandardCharsets.UTF_8));
                            String line;
                            StringBuilder sb = new StringBuilder();
                            while ((line = br.readLine()) != null)
                                if (!isToHide(pref, lpp.packageName, line))
                                    sb.append(line).append("\n");
                            InputStream result = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
                            param.setResult(result);
                        }
                    });
                } catch (Throwable e) {
                    Log.e(LOG, "hooking Runtime.exec ERROR");
                }
            }
        });
    }

    void apiHook(final LoadPackageParam lpp, final XSharedPreferences pref) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpp.classLoader, "getInstalledPackages", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                List<PackageInfo> packageInfos = (List) param.getResult();
                for (Iterator<PackageInfo> iterator = packageInfos.iterator(); iterator.hasNext(); )
                    if (isToHide(pref, lpp.packageName, iterator.next().packageName))
                        iterator.remove();
                param.setResult(packageInfos);
            }
        });
    }

    void intentHook(final LoadPackageParam lpp, final XSharedPreferences pref) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpp.classLoader, "queryIntentActivities", Intent.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                List<ResolveInfo> infos = (List) param.getResult();
                for (Iterator<ResolveInfo> iterator = infos.iterator(); iterator.hasNext(); )
                    if (isToHide(pref, lpp.packageName, iterator.next().activityInfo.packageName))
                        iterator.remove();
                param.setResult(infos);
            }
        });
    }

    void uidHook(final LoadPackageParam lpp, final XSharedPreferences pref) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpp.classLoader, "getPackagesForUid", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String[] list = (String[]) param.getResult();
                if (list != null)
                    for (String pkg : list)
                        if (isToHide(pref, lpp.packageName, pkg)) {
                            param.setResult(null);
                            break;
                        }
            }
        });
    }

    void fileHook(final LoadPackageParam lpp, final XSharedPreferences pref) {
        XposedHelpers.findAndHookConstructor(File.class, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                String path = (String) param.args[0];
                if (path.contains(lpp.packageName)) return;
                if (pref.getBoolean("HideAllApps", false) && path.contains("Android/data/")) {
                    param.args[0] = "fuck/there/is/no/file";
                    return;
                }
                for (String pkg : pref.getStringSet("HideApps", null))
                    if (path.contains(pkg)) {
                        param.args[0] = "fuck/there/is/no/file";
                        break;
                    }
            }
        });
    }
}
