package com.tsng.hidemyapplist;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    public static final String LOG = "hma_log";
    public static final String APPNAME = "com.tsng.hidemyapplist";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpp) {
        if (lpp.packageName.equals(APPNAME))
            XposedHelpers.findAndHookMethod("com.tsng.hidemyapplist.ui.xposed.XposedFragment", lpp.classLoader, "getXposedStatus", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(true);
                }
            });
        //判断模块是否生效用

        XSharedPreferences pref = new XSharedPreferences(APPNAME, "Scope");
        final String template = pref.getString(lpp.packageName, null);
        if (template == null) return;
        //判断是否hook该应用

        pref = new XSharedPreferences(APPNAME, "tpl_" + template);
        final boolean enable_all_hooks = pref.getBoolean("EnableAllHooks", false);
        final Set<String> enabled = pref.getStringSet("ApplyHooks", null);
        //获取模板

        if (enable_all_hooks || enabled.contains("method_pm")) pmHook(pref);
        if (enable_all_hooks || enabled.contains("method_api")) apiHook(lpp, pref);
        if (enable_all_hooks || enabled.contains("method_intent")) intentHook(lpp, pref);
        if (enable_all_hooks || enabled.contains("method_uid")) uidHook(lpp, pref);
        if (enable_all_hooks || enabled.contains("method_datafile")) fileHook(lpp, pref);
        //下钩子
    }

    void pmHook(final XSharedPreferences pref) {
        if (pref.getBoolean("HideAllApps", false))
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", String.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (((String) param.args[0]).contains("pm list packages"))
                        param.args[0] = "echo Permission Denied";
                }
            });
        else
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        XposedHelpers.findAndHookMethod(param.getResult().getClass(), "getInputStream", new XC_MethodHook() {
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                BufferedReader br = new BufferedReader(new InputStreamReader((InputStream) param.getResult(), StandardCharsets.UTF_8));
                                String line;
                                StringBuilder sb = new StringBuilder();
                                while ((line = br.readLine()) != null) {
                                    boolean flg = false;
                                    for (String key : pref.getStringSet("HideApps", null))
                                        if (line.contains(key)) {
                                            flg = true;
                                            break;
                                        }
                                    if (flg) continue;
                                    sb.append(line).append("\n");
                                }
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
                if (pref.getBoolean("HideAllApps", false)) {
                    param.setResult(null);
                    return;
                }
                List<PackageInfo> packageInfos = (List) param.getResult();
                for (Iterator<PackageInfo> iterator = packageInfos.iterator(); iterator.hasNext(); ) {
                    String name = iterator.next().packageName;
                    for (String pkg : pref.getStringSet("HideApps", null))
                        if (name.equals(pkg)) {
                            iterator.remove();
                            break;
                        }
                }
                param.setResult(packageInfos);
            }
        });
    }

    void intentHook(final LoadPackageParam lpp, final XSharedPreferences pref) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpp.classLoader, "queryIntentActivities", Intent.class, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (pref.getBoolean("HideAllApps", false)) {
                    param.setResult(null);
                    return;
                }
                List<ResolveInfo> infos = (List) param.getResult();
                for (Iterator<ResolveInfo> iterator = infos.iterator(); iterator.hasNext(); ) {
                    String name = iterator.next().activityInfo.packageName;
                    for (String pkg : pref.getStringSet("HideApps", null))
                        if (name.equals(pkg)) {
                            iterator.remove();
                            break;
                        }
                }
                param.setResult(infos);
            }
        });
    }

    void uidHook(final LoadPackageParam lpp, final XSharedPreferences pref) {
        XposedHelpers.findAndHookMethod("android.app.ApplicationPackageManager", lpp.classLoader, "getPackagesForUid", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (pref.getBoolean("HideAllApps", false)) {
                    param.setResult(null);
                    return;
                }
                String[] list = (String[]) param.getResult();
                if (list != null)
                    for (String pkg : list)
                        if (pref.getStringSet("HideApps", null).contains(pkg)) {
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
                if (pref.getBoolean("HideAllApps", false) &&
                        ((String) param.args[0]).contains("Android/data/") &&
                        !((String) param.args[0]).contains(lpp.packageName)) {
                    param.args[0] = "fuck/there/is/no/file";
                    return;
                }
                for (String pkg : pref.getStringSet("HideApps", null))
                    if (((String) param.args[0]).contains(pkg)) {
                        param.args[0] = "fuck/there/is/no/file";
                        break;
                    }
            }
        });
    }
}