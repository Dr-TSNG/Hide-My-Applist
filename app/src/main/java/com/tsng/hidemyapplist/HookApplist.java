package com.tsng.hidemyapplist;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HookApplist implements IXposedHookLoadPackage {
    public static final String APPNAME = "hma_log";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpp) {
        if(lpp.packageName.equals("com.tsng.hidemyapplist"))
            XposedHelpers.findAndHookMethod("com.tsng.hidemyapplist.ui.xposed.XposedFragment", lpp.classLoader, "getXposedStatus", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) { param.setResult(true); }
            });
        XSharedPreferences pre = new XSharedPreferences(lpp.packageName, lpp.packageName);
        Set<String> targets = pre.getStringSet("hideSet", new HashSet<>());
        if (targets.isEmpty()) return;
        Log.d(APPNAME, "pkg:" + lpp.packageName);
        pmHook(targets);
        apiHook(lpp, targets);
        intentHook(lpp, targets);
        uidHook(lpp, targets);
        fileHook(targets);
    }

    void pmHook(final Set<String> targets) {
        XposedHelpers.findAndHookMethod(Runtime.class, "exec", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                //Log.d(APPNAME, "hooked into Runtime.exec");
                try {
                    XposedHelpers.findAndHookMethod(param.getResult().getClass(), "getInputStream", new XC_MethodHook() {
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            BufferedReader br = new BufferedReader(new InputStreamReader((InputStream) param.getResult(), "utf-8"));
                            String line = null;
                            StringBuilder sb = new StringBuilder();
                            while ((line = br.readLine()) != null) {
                                boolean flg = false;
                                for (String key : targets)
                                    if (line.contains(key)) {
                                        flg = true;
                                        break;
                                    }
                                if (flg) continue;
                                sb.append(line + "\n");
                            }
                            InputStream result = new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
                            param.setResult(result);
                        }
                    });
                } catch (Throwable e) {
                    Log.e(APPNAME, "hooking Runtime.exec ERROR");
                }
            }
        });
    }

    void apiHook(final LoadPackageParam lpp, final Set<String> targets) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpp.classLoader, "getInstalledPackages", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                //Log.d(APPNAME, "hooked into getInstalledPackages");
                List<PackageInfo> packageInfos = (List) param.getResult();
                for (Iterator<PackageInfo> iterator = packageInfos.iterator(); iterator.hasNext(); ) {
                    String name = iterator.next().packageName;
                    for (String pkg : targets)
                        if (name.equals(pkg)) {
                            iterator.remove();
                            break;
                        }
                }
                param.setResult(packageInfos);
            }
        });
    }

    void intentHook(final LoadPackageParam lpp, final Set<String> targets) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpp.classLoader, "queryIntentActivities", Intent.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                //Log.d(APPNAME, "hooked into queryIntentActivities");
                List<ResolveInfo> infos = (List) param.getResult();
                for (Iterator<ResolveInfo> iterator = infos.iterator(); iterator.hasNext(); ) {
                    String name = iterator.next().activityInfo.packageName;
                    for (String pkg : targets)
                        if (name.equals(pkg)) {
                            iterator.remove();
                            break;
                        }
                }
                param.setResult(infos);
            }
        });
    }

    void uidHook(final LoadPackageParam lpp, final Set<String> targets) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpp.classLoader, "getPackagesForUid", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                //Log.d(APPNAME, "hooked into getPackagesForUid");
                String[] list = (String[]) param.getResult();
                if (list != null)
                    for (String pkg : list)
                        if (targets.contains(pkg)) {
                            param.setResult(null);
                            break;
                        }
            }
        });
    }

    void fileHook(final Set<String> targets) {
        XposedHelpers.findAndHookConstructor(File.class, String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                //Log.d(APPNAME, "hooked into File");
                for (String pkg : targets)
                    if (((String) param.args[0]).contains(pkg)) {
                        param.args[0] = "fuck/there/is/no/file";
                        break;
                    }
            }
        });
    }
}