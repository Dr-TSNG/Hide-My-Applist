package com.tsng.hidemyapplist;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Set<String> targets;
    LinearLayout layout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layout = findViewById(R.id.mainLayout);
        targets = new HashSet<>(Arrays.asList(getResources().getStringArray(R.array.packages)));
        Set<String> l1 = method_pm();
        Set<String> l2 = method_api();
        Set<String> l3 = method_intent();
        Set<String> l4 = method_uid();
        Set<String> l5 = method_datafile();

        Debug(l1, "db_method_pm");
        Debug(l2, "db_method_api");
        Debug(l3, "db_method_intent");
        Debug(l4, "db_method_uid");
        Debug(l5, "db_method_datafile");
    }

    void Debug(Set<String> L, String method){
        String outputText = "Detection method: " + method + "\n";
        if(L == null)
            outputText += "Permission denied.\n";
        else if(L.isEmpty())
            outputText += "Target not found.\n";
        else for(String s: L)
            outputText += "Target found: " + s + "\n";
        TextView textView = new TextView(this);
        textView.setText(outputText);
        layout.addView(textView);
    }

    private Set<String> findPackages(Set<String> packages) {
        if(packages.isEmpty()) return null;
        Set<String> found = new HashSet<>();
        for(String name: packages){
            if(targets.contains(name))
                found.add(name);
        }
        return found;
    }

    private Set<String> method_pm() {
        Set<String> packages = new HashSet<>();
        try {
            java.lang.Process p = Runtime.getRuntime().exec("pm list packages");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "utf-8"));
            String line = br.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() > 8) {
                    String prefix = line.substring(0, 8);
                    if (prefix.equalsIgnoreCase("package:")) {
                        line = line.substring(8).trim();
                        if (!TextUtils.isEmpty(line)) {
                            packages.add(line);
                        }
                    }
                }
                line = br.readLine();
            }
            br.close();
            p.destroy();
        } catch (Throwable t) {
            Log.e("db_method_pm", t.toString());
        }
        return findPackages(packages);
    }

    private Set<String> method_api() {
        Set<String> packages = new HashSet<>();
        try {
            List<PackageInfo> packageInfos = getPackageManager().getInstalledPackages(0);
            for (PackageInfo info : packageInfos)
                packages.add(info.packageName);
        } catch (Throwable t) {
            Log.e("db_method_api", t.toString());
        }
        return findPackages(packages);
    }

    private Set<String> method_intent() {
        Set<String> packages = new HashSet<>();
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            List<ResolveInfo> infos = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_ALL);
            for(ResolveInfo i: infos)
                packages.add(i.activityInfo.packageName);
        } catch (Throwable t) {
            Log.e("db_method_intent", t.toString());
        }
        return findPackages(packages);
    }

    private Set<String> method_uid() {
        Set<String> packages = new HashSet<>();
        try {
            for(int i = Process.SYSTEM_UID; i<=Process.LAST_APPLICATION_UID; i++){
                String[] uid = getPackageManager().getPackagesForUid(i);
                if(uid != null)
                    Collections.addAll(packages, uid);
            }
        } catch (Throwable t) {
            Log.e("db_method_uid", t.toString());
        }
        return findPackages(packages);
    }

    private Set<String> method_datafile() {
        Set<String> packages = new HashSet<>();
        try {
            for(String pkg: targets){
                File f = new File("/storage/emulated/0/Android/data/" + pkg);
                if(f.exists())packages.add(pkg);
            }
        } catch (Throwable t) {
            Log.e("db_method_datafile", t.toString());
        }
        return findPackages(packages);
    }
}