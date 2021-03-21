package com.tsng.hidemyapplist;

import androidx.appcompat.app.AppCompatActivity;

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
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.List;
import java.util.Set;

public class DetectionActivity extends AppCompatActivity implements View.OnClickListener {

    private Set<String> targets;
    LinearLayout ResultLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ReadTargets();
        TextView tv_CurrentPackages = findViewById(R.id.detection_tv_CurrentPackages);
        tv_CurrentPackages.setMovementMethod(new ScrollingMovementMethod());
        UpdateTargetPackageView();
        ResultLayout = findViewById(R.id.detection_ResultLayout);
        Button btn_AddPackage = findViewById(R.id.detection_btn_AddPackage);
        btn_AddPackage.setOnClickListener(this);
        Button btn_StartDetect = findViewById(R.id.detection_btn_StartDetect);
        btn_StartDetect.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.detection_btn_AddPackage:
                EditText et = findViewById(R.id.detection_et_DetectionTarget);
                String text = et.getText().toString();
                if (!text.isEmpty())
                    targets.add(text);
                SaveTarget();
                UpdateTargetPackageView();
                et.setText(null);
                break;
            case R.id.detection_btn_StartDetect:
                DetectionTask task = new DetectionTask();
                task.execute();
                break;
        }
    }

    private class DetectionTask extends AsyncTask<Void, TextView, Void> {
        int progress;
        List<String> Results;
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            ResultLayout.removeAllViews();
            progress = 1;
            dialog = new ProgressDialog(DetectionActivity.this);
            dialog.setCancelable(false);
            dialog.setTitle(getResources().getString(R.string.detection_executing_detections));
            dialog.setMessage(getResources().getString(R.string.detection_using_method) + " 1/5");
            dialog.show();
        }

        @Override
        protected void onProgressUpdate(TextView... tv) {
            ResultLayout.addView(tv[0]);
            dialog.setMessage(getResources().getString(R.string.detection_using_method) + " " + (++progress) + "/5");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Debug(method_pm(), "db_method_pm");
            Debug(method_api(), "db_method_api");
            Debug(method_intent(), "db_method_intent");
            Debug(method_uid(), "db_method_uid");
            Debug(method_datafile(), "db_method_datafile");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            dialog.dismiss();
        }

        private void Debug(Set<String> L, String method) {
            String outputText = getResources().getString(R.string.detection_method) + ": " + method + "\n";
            if (L == null)
                outputText += getResources().getString(R.string.detection_permission_denied) + "\n";
            else if (L.isEmpty())
                outputText += getResources().getString(R.string.detection_target_not_found) + "\n";
            else for (String s : L)
                    outputText += getResources().getString(R.string.detection_target_found) + " " + s + "\n";
            TextView tv = new TextView(DetectionActivity.this);
            tv.setText(outputText);
            publishProgress(tv);
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
                List<PackageInfo> packageInfos = getPackageManager().getInstalledPackages(0);
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
                List<ResolveInfo> infos = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_ALL);
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
                    String[] uid = getPackageManager().getPackagesForUid(i);
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
        TextView tv = findViewById(R.id.detection_tv_CurrentPackages);
        String str = "";
        for (String name : targets)
            str += name + "\n";
        tv.setText(str);
    }

    private void SaveTarget() {
        SharedPreferences.Editor editor = getSharedPreferences("DetectionTarget", MODE_PRIVATE).edit();
        editor.putStringSet("targetSet", targets);
        editor.apply();
    }

    private void ReadTargets() {
        targets = getSharedPreferences("target", MODE_PRIVATE).getStringSet("DetectionTarget", null);
        if(targets == null)
            targets = new TreeSet<>(Arrays.asList(getResources().getStringArray(R.array.packages)));
        else
            targets = new TreeSet<>(targets);
        SharedPreferences self = getSharedPreferences("com.tsng.hidemyapplist", MODE_PRIVATE);
        if(self.getStringSet("hideSet", new HashSet<>()).isEmpty())
            self.edit().putStringSet("hideSet", targets).apply();
    }
}