package com.tsng.hidemyapplist.app.ui.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tsng.hidemyapplist.BuildConfig;
import com.tsng.hidemyapplist.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntFunction;

public class DetectionActivity extends AppCompatActivity implements View.OnClickListener {
    Set<String> targets;
    SharedPreferences default_pref;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        default_pref = PreferenceManager.getDefaultSharedPreferences(this);
        ReadTargets();
        UpdateTargetPackageView();
        findViewById(R.id.detection_btn_AddPackage).setOnClickListener(this);
        findViewById(R.id.detection_btn_StartDetect).setOnClickListener(this);
        ((ListView) findViewById(R.id.detection_lv_CurrentPackages)).setOnItemLongClickListener((parent, view, position, id) -> {
            String s = ((TextView) view).getText().toString();
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.detection_delete_confirm)
                    .setMessage(s)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, ((dialog, which) -> {
                        targets.remove(s);
                        SaveTargets();
                        UpdateTargetPackageView();
                    })).show();
            return true;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.detection_btn_AddPackage:
                EditText et = findViewById(R.id.detection_et_DetectionTarget);
                String text = et.getText().toString();
                if (!text.isEmpty())
                    targets.add(text);
                SaveTargets();
                UpdateTargetPackageView();
                et.setText(null);
                break;
            case R.id.detection_btn_StartDetect:
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.detection_hint_how_to_use_title)
                        .setMessage(Html.fromHtml(getString(R.string.detection_hint_how_to_use_message), Html.FROM_HTML_MODE_COMPACT))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, ((dialog, which) -> {
                            DetectionActivity.DetectionTask task = new DetectionActivity.DetectionTask();
                            task.execute();
                        })).show();
                break;
        }
    }

    private class DetectionTask extends AsyncTask<Void, Void, Void> {
        int progress;
        int[][] methodStatus = new int[5][30];
        ProgressDialog dialog;

        final Map<String, Integer> M0 = new LinkedHashMap<String, Integer>() {{
            put("pm list packages", 0);
            put("getInstalledPackages", 1);
            put("getInstalledApplications", 2);
            put("getPackagesHoldingPermissions", 3);
        }};

        final Map<String, Integer> M1 = new LinkedHashMap<String, Integer>() {{
            put("queryIntentActivities", 0);
        }};

        final Map<String, Integer> M2 = new LinkedHashMap<String, Integer>() {{
            put("getPackageUid", 0);
        }};

        final Map<String, Integer> M3 = new LinkedHashMap<String, Integer>() {{
            put("java File", 0);
            put("libc access", 1);
            put("libc stat", 2);
            put("libc fstat", 3);
            put("syscall stat", 4);
            put("syscall fstat", 5);
        }};

        final Map<String, Integer> M4 = new LinkedHashMap<String, Integer>() {{
            put("maps scan", 0);
        }};

        final int ALL_METHODS = M0.size() + M1.size() + M2.size() + M3.size() + M4.size();

        @Override
        protected void onPreExecute() {
            progress = 1;
            dialog = new ProgressDialog(DetectionActivity.this);
            dialog.setCancelable(false);
            dialog.setTitle(getResources().getString(R.string.detection_executing_detections));
            dialog.setMessage(getResources().getString(R.string.detection_using_method) + " 1/" + ALL_METHODS);
            dialog.show();
        }

        @Override
        protected void onProgressUpdate(Void... voids) {
            dialog.setMessage(getResources().getString(R.string.detection_using_method) + " " + (++progress) + "/" + ALL_METHODS);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            method_pm();
            publishProgress();
            checkList(0, M0.get("getInstalledPackages"), getPackageManager().getInstalledPackages(0));
            publishProgress();
            checkList(0, M0.get("getInstalledApplications"), getPackageManager().getInstalledApplications(0));
            publishProgress();
            method_getPackagesHoldingPermissions();
            publishProgress();

            method_intent();
            publishProgress();

            method_getPackageUid();
            publishProgress();

            method_file();
            publishProgress();
            publishProgress();
            publishProgress();
            publishProgress();
            publishProgress();
            publishProgress();

            method_maps();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            dialog.dismiss();

            IntFunction res = (int r) -> r == 1 ? "[F] " : r == 0 ? "[N] " : "[D] ";
            StringBuilder br = new StringBuilder();
            br.append(getString(R.string.detection_color_means)).append("<br/>");
            br.append("<h5><b>API requests</b></h5>");
            for (Map.Entry<String, Integer> entry : M0.entrySet())
                br.append(res.apply(methodStatus[0][entry.getValue()])).append(entry.getKey()).append("<br/>");
            br.append("<h5><b>Intent queries</b></h5>");
            for (Map.Entry<String, Integer> entry : M1.entrySet())
                br.append(res.apply(methodStatus[1][entry.getValue()])).append(entry.getKey()).append("<br/>");
            br.append("<h5><b>ID detections</b></h5>");
            for (Map.Entry<String, Integer> entry : M2.entrySet())
                br.append(res.apply(methodStatus[2][entry.getValue()])).append(entry.getKey()).append("<br/>");
            br.append("<h5><b>File detections</b></h5>");
            for (Map.Entry<String, Integer> entry : M3.entrySet())
                br.append(res.apply(methodStatus[3][entry.getValue()])).append(entry.getKey()).append("<br/>");
            br.append("<h5><b>Characteristics</b></h5>");
            for (Map.Entry<String, Integer> entry : M4.entrySet())
                br.append(res.apply(methodStatus[4][entry.getValue()])).append(entry.getKey()).append("<br/>");


            new MaterialAlertDialogBuilder(DetectionActivity.this)
                    .setTitle(R.string.detection_finished)
                    .setMessage(Html.fromHtml(br.toString(), Html.FROM_HTML_MODE_COMPACT))
                    .setPositiveButton(android.R.string.ok, null).show();
        }

        private void checkList(int generalId, int methodId, List list) {
            if (list == null) {
                methodStatus[generalId][methodId] = -1;
                return;
            }
            Set<String> packages = new HashSet<>();
            try {
                for (Object it : list)
                    packages.add((String) it.getClass().getField("packageName").get(it));
            } catch (Exception ignored) { }
            if (packages.isEmpty()) packages = null;
            methodStatus[generalId][methodId] = findPackages(packages);
        }

        private void method_pm() {
            Set<String> packages = new HashSet<>();
            try {
                java.lang.Process p = Runtime.getRuntime().exec("pm list packages");
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.length() > 8) {
                        String prefix = line.substring(0, 8);
                        if (prefix.equalsIgnoreCase("package:")) {
                            line = line.substring(8).trim();
                            if (!TextUtils.isEmpty(line))
                                packages.add(line);
                        }
                    }
                }
                br.close();
                p.destroy();
            } catch (Exception ignored) { }
            if (packages.isEmpty()) packages = null;
            methodStatus[0][M0.get("pm list packages")] = findPackages(packages);
        }

        private void method_getPackagesHoldingPermissions() {
            List<String> permissions = new ArrayList<>();
            for (Field field : Manifest.permission.class.getFields()) {
                try {
                    if (field.getType() == String.class)
                        permissions.add((String) field.get(null));
                } catch (Exception ignored) { }
            }
            checkList(0, M0.get("getPackagesHoldingPermissions"), getPackageManager().getPackagesHoldingPermissions(permissions.toArray(new String[0]), 0));
        }

        private void method_intent() {
            Set<String> packages = new HashSet<>();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            List<ResolveInfo> infos = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_ALL);
            for (ResolveInfo i : infos)
                packages.add(i.activityInfo.packageName);
            if (packages.isEmpty()) packages = null;
            methodStatus[1][M1.get("queryIntentActivities")] = findPackages(packages);
        }

        private void method_getPackageUid() {
            for (String pkg : targets)
                try {
                    getPackageManager().getPackageUid(pkg, 0);
                    methodStatus[2][M2.get("getPackageUid")] = 1;
                    return;
                } catch (PackageManager.NameNotFoundException ignored) { }
            methodStatus[2][M2.get("getPackageUid")] = 0;
        }

        private native int[] nativeFile(String path);

        private void method_file() {
            for (String pkg : targets) {
                final String path1 = "/storage/emulated/0/Android/data/" + pkg;
                final String path2 = "/data/data/" + pkg;
                int[] nativeResult1 = nativeFile(path1);
                int[] nativeResult2 = nativeFile(path2);
                methodStatus[3][M3.get("java File")]     |= new File(path1).exists() ? 1 : 0;
                methodStatus[3][M3.get("java File")]     |= new File(path2).exists() ? 1 : 0;
                methodStatus[3][M3.get("libc access")]   |= nativeResult1[0] | nativeResult2[0];
                methodStatus[3][M3.get("libc stat")]     |= nativeResult1[1] | nativeResult2[1];
                methodStatus[3][M3.get("libc fstat")]    |= nativeResult1[2] | nativeResult2[2];
                methodStatus[3][M3.get("syscall stat")]  |= nativeResult1[3] | nativeResult2[3];
                methodStatus[3][M3.get("syscall fstat")] |= nativeResult1[4] | nativeResult2[4];
            }
        }

        private void method_maps() {
            try (BufferedReader br = new BufferedReader(new FileReader("/proc/self/maps"))) {
                String str;
                while ((str = br.readLine()) != null) {
                    if (str.contains(BuildConfig.APPLICATION_ID)) {
                        methodStatus[4][M4.get("maps scan")] = 1;
                        return;
                    }
                }
                methodStatus[4][M4.get("maps scan")] = 0;
            } catch (Exception e) {
                Log.w("[HMA Detections]", "Read maps failed: " + e.getMessage());
                e.printStackTrace();
                methodStatus[4][M4.get("maps scan")] = -1;
            }
        }

        private int findPackages(Set<String> packages) {
            if (packages == null) return -1;
            for (String name : packages)
                if (targets.contains(name))
                    return 1;
            return 0;
        }
    }

    private void UpdateTargetPackageView() {
        ListView lv = findViewById(R.id.detection_lv_CurrentPackages);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, targets.toArray(new String[0]));
        lv.setAdapter(adapter);
    }

    private void SaveTargets() {
        default_pref.edit().putStringSet("detectionSet", targets).apply();
    }

    private void ReadTargets() {
        targets = default_pref.getStringSet("detectionSet", null);
        if (targets == null)
            targets = new TreeSet<>(Arrays.asList(getResources().getStringArray(R.array.packages)));
        else targets = new TreeSet<>(targets);
    }
}