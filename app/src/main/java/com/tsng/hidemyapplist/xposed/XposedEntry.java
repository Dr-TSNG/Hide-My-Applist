package com.tsng.hidemyapplist.xposed;
import android.util.Log;

import com.tsng.hidemyapplist.BuildConfig;
import com.tsng.hidemyapplist.xposed.hooks.IndividualHooks;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedEntry implements IXposedHookLoadPackage {
    public static final String LOG = "hma_log";
    public static final String APPNAME = BuildConfig.APPLICATION_ID;

    @Override
    public void handleLoadPackage(final LoadPackageParam lpp) {
        new IndividualHooks().handleLoadPackage(lpp);
    }
}