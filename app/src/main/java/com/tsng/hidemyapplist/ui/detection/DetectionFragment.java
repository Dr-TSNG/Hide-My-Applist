package com.tsng.hidemyapplist.ui.detection;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

    private class DetectionTask extends AsyncTask<Void, String, Void> {
        int progress;
        String Results = "";
        ProgressDialog dialog;

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
        protected void onProgressUpdate(String... str) {
            Results += str[0] + "\n";
            dialog.setMessage(getResources().getString(R.string.detection_using_method) + " " + (++progress) + "/5");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Debug(method_pm(), "pm command");
            Debug(method_api(), "API request");
            Debug(method_intent(), "intent query");
            Debug(method_uid(), "uid getname");
            Debug(method_datafile(), "datafile detection");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            dialog.dismiss();
            new MaterialAlertDialogBuilder(main)
                    .setTitle(getString(R.string.detection_finished))
                    .setMessage(Results)
                    .setPositiveButton(getString(R.string.accept), null).show();
        }

        private void Debug(Set<String> L, String method) {
            String outputText = getResources().getString(R.string.detection_method) + ": " + method + "\n";
            if (L == null)
                outputText += getResources().getString(R.string.detection_permission_denied) + "\n";
            else if (L.isEmpty())
                outputText += getResources().getString(R.string.detection_target_not_found) + "\n";
            else for (String s : L)
                    outputText += getResources().getString(R.string.detection_target_found) + " " + s + "\n";
            publishProgress(outputText);
        }

        private Set<String> method_pm() {
            Set<String> packages = new TreeSet<>();
            try {
                java.lang.Process p = Runtime.getRuntime().exec("pm list packages");
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "utf-8"));
                String line = null;
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
            } catch (Throwable t) {
                Log.e("db_method_pm", t.toString());
            }
            return findPackages(packages);
        }

        private Set<String> method_api() {
            Set<String> packages = new TreeSet<>();
            try {
                List<PackageInfo> packageInfos = main.getPackageManager().getInstalledPackages(0);
                for (PackageInfo info : packageInfos)
                    packages.add(info.packageName);
            } catch (Throwable t) {
                Log.e("db_method_api", t.toString());
            }
            return findPackages(packages);
        }

        private Set<String> method_intent() {
            Set<String> packages = new TreeSet<>();
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                List<ResolveInfo> infos = main.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_ALL);
                for (ResolveInfo i : infos)
                    packages.add(i.activityInfo.packageName);
            } catch (Throwable t) {
                Log.e("db_method_intent", t.toString());
            }
            return findPackages(packages);
        }

        private Set<String> method_uid() {
            Set<String> packages = new TreeSet<>();
            try {
                for (int i = Process.SYSTEM_UID; i <= Process.LAST_APPLICATION_UID; i++) {
                    String[] uid = main.getPackageManager().getPackagesForUid(i);
                    if (uid != null)
                        Collections.addAll(packages, uid);
                }
            } catch (Throwable t) {
                Log.e("db_method_uid", t.toString());
            }
            return findPackages(packages);
        }

        private Set<String> method_datafile() {
            Set<String> packages = new TreeSet<>();
            try {
                for (String pkg : targets) {
                    File f = new File("/storage/emulated/0/Android/data/" + pkg);
                    if (f.exists()) packages.add(pkg);
                }
            } catch (Throwable t) {
                Log.e("db_method_datafile", t.toString());
            }
            return findPackages(packages);
        }

        private Set<String> findPackages(Set<String> packages) {
            if (packages.isEmpty()) return null;
            Set<String> found = new TreeSet<>();
            for (String name : packages) {
                if (targets.contains(name))
                    found.add(name);
            }
            return found;
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