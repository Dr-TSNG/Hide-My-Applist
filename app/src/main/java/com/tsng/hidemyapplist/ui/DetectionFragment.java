package com.tsng.hidemyapplist.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tsng.hidemyapplist.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntFunction;

public class DetectionFragment extends Fragment implements View.OnClickListener {

    View root;
    Activity main;
    Set<String> targets;
    SharedPreferences default_pref;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_detection, container, false);
        default_pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        main = getActivity();
        ReadTargets();
        UpdateTargetPackageView();
        root.findViewById(R.id.detection_btn_AddPackage).setOnClickListener(this);
        root.findViewById(R.id.detection_btn_StartDetect).setOnClickListener(this);
        ((ListView) root.findViewById(R.id.detection_lv_CurrentPackages)).setOnItemLongClickListener((parent, view, position, id) -> {
            String s = ((TextView) view).getText().toString();
            new MaterialAlertDialogBuilder(main)
                    .setTitle(getString(R.string.detection_delete_confirm))
                    .setMessage(s)
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setPositiveButton(getString(R.string.accept), ((dialog, which) -> {
                        targets.remove(s);
                        SaveTargets();
                        UpdateTargetPackageView();
                    })).show();
            return true;
        });
        return root;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.detection_btn_AddPackage:
                EditText et = root.findViewById(R.id.detection_et_DetectionTarget);
                String text = et.getText().toString();
                if (!text.isEmpty())
                    targets.add(text);
                SaveTargets();
                UpdateTargetPackageView();
                et.setText(null);
                break;
            case R.id.detection_btn_StartDetect:
                DetectionFragment.DetectionTask task = new DetectionFragment.DetectionTask();
                task.execute();
                break;
        }
    }

    private class DetectionTask extends AsyncTask<Void, Void, Void> {
        int progress;
        int[][] methodStatus = new int[5][30];
        ProgressDialog dialog;

        final Map<String, Integer> M0 = new LinkedHashMap<String, Integer>() {{
            put("pm", 0);
            put("getInstalledPackages", 1);
            put("getInstalledApplications", 2);
            put("getPackagesHoldingPermissions", 3);
            put("queryInstrumentation", 4);
        }};

        final Map<String, Integer> M1 = new LinkedHashMap<String, Integer>() {{
            put("queryIntentActivities", 0);
        }};

        final Map<String, Integer> M2 = new LinkedHashMap<String, Integer>() {{
            put("getPackagesForUid", 0);
        }};

        final Map<String, Integer> M3 = new LinkedHashMap<String, Integer>() {{
            put("javaFile", 0);
        }};

        @Override
        protected void onPreExecute() {
            progress = 1;
            dialog = new ProgressDialog(main);
            dialog.setCancelable(false);
            dialog.setTitle(getResources().getString(R.string.detection_executing_detections));
            dialog.setMessage(getResources().getString(R.string.detection_using_method) + " 1/5");
            dialog.show();
        }

        @Override
        protected void onProgressUpdate(Void... voids) {
            dialog.setMessage(getResources().getString(R.string.detection_using_method) + " " + (++progress) + "/5");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            method_pm();
            publishProgress();
            checkList(0, M0.get("getInstalledPackages"), main.getPackageManager().getInstalledPackages(0));
            publishProgress();
            checkList(0, M0.get("getInstalledApplications"), main.getPackageManager().getInstalledApplications(0));
            publishProgress();
            method_getPackagesHoldingPermissions();
            publishProgress();
            checkList(0, M0.get("queryInstrumentation"), main.getPackageManager().queryInstrumentation(null, PackageManager.GET_META_DATA));
            publishProgress();

            method_intent();
            publishProgress();

            method_uid();
            publishProgress();

            method_datafile();
            publishProgress();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            dialog.dismiss();

            IntFunction res = (int r) -> r == 1 ? "\uD83D\uDFE5" : r == 0 ? "\uD83D\uDFE9" : "\uD83D\uDFE8";
            StringBuilder br = new StringBuilder();
            br.append("API requests:\n");
            for (Map.Entry<String, Integer> entry : M0.entrySet())
                br.append(res.apply(methodStatus[0][entry.getValue()])).append(entry.getKey()).append('\n');
            br.append("Intent queries:\n");
            for (Map.Entry<String, Integer> entry : M1.entrySet())
                br.append(res.apply(methodStatus[1][entry.getValue()])).append(entry.getKey()).append('\n');
            br.append("UID detections:\n");
            for (Map.Entry<String, Integer> entry : M2.entrySet())
                br.append(res.apply(methodStatus[2][entry.getValue()])).append(entry.getKey()).append('\n');
            br.append("File detections:\n");
            for (Map.Entry<String, Integer> entry : M3.entrySet())
                br.append(res.apply(methodStatus[3][entry.getValue()])).append(entry.getKey()).append('\n');

            new MaterialAlertDialogBuilder(main)
                    .setTitle(getString(R.string.detection_finished))
                    .setMessage(br.toString())
                    .setPositiveButton(getString(R.string.accept), null).show();
        }

        private void checkList(int generalId, int methodId, List list) {
            if (list == null) {
                methodStatus[generalId][methodId] = -1;
                return;
            }
            Set<String> packages = new TreeSet<>();
            try {
                for (Object it : list)
                    packages.add((String) it.getClass().getField("packageName").get(it));
            } catch (Exception ignored) { }
            methodStatus[generalId][methodId] = findPackages(packages);
        }

        private void method_pm() {
            Set<String> packages = new TreeSet<>();
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
            methodStatus[0][M0.get("pm")] = findPackages(packages);
        }

        private void method_getPackagesHoldingPermissions() {
            List<String> permissions = new ArrayList<>();
            for (Field field : Manifest.permission.class.getFields()) {
                try {
                    if (field.getType() == String.class)
                        permissions.add((String) field.get(null));
                } catch (Exception ignored) { }
            }
            checkList(0, M0.get("getPackagesHoldingPermissions"), main.getPackageManager().getPackagesHoldingPermissions(permissions.toArray(new String[0]), 0));
        }

        private void method_intent() {
            Set<String> packages = new TreeSet<>();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            List<ResolveInfo> infos = main.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_ALL);
            for (ResolveInfo i : infos)
                packages.add(i.activityInfo.packageName);
            if (packages.isEmpty()) packages = null;
            methodStatus[1][M1.get("queryIntentActivities")] = findPackages(packages);
        }

        private void method_uid() {
            Set<String> packages = new TreeSet<>();
            for (int i = Process.SYSTEM_UID; i <= Process.LAST_APPLICATION_UID; i++) {
                String[] uid = main.getPackageManager().getPackagesForUid(i);
                if (uid != null)
                    Collections.addAll(packages, uid);
            }
            methodStatus[2][M2.get("getPackagesForUid")] = findPackages(packages);
        }

        private void method_datafile() {
            Set<String> packages = new TreeSet<>();
            try {
                for (String pkg : targets) {
                    File f = new File("/storage/emulated/0/Android/data/" + pkg);
                    if (f.exists()) packages.add(pkg);
                }
            } catch (Throwable t) {
                Log.e("db_method_datafile", t.toString());
            }
            methodStatus[3][M3.get("javaFile")] = findPackages(packages);
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
        ListView lv = root.findViewById(R.id.detection_lv_CurrentPackages);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(main, android.R.layout.simple_list_item_1, targets.toArray(new String[0]));
        lv.setAdapter(adapter);
    }

    private void SaveTargets() {
        default_pref.edit().putStringSet("DetectionSet", targets).apply();
    }

    private void ReadTargets() {
        targets = default_pref.getStringSet("DetectionSet", null);
        if (targets == null)
            targets = new TreeSet<>(Arrays.asList(getResources().getStringArray(R.array.packages)));
        else
            targets = new TreeSet<>(targets);
    }
}