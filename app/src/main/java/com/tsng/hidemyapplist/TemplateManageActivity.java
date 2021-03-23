package com.tsng.hidemyapplist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TemplateManageActivity extends AppCompatActivity {

    Set<String> templates;
    ArrayAdapter<String> adapter;
    SharedPreferences list_pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template_manage);
        InitTemplateList();
        findViewById(R.id.xposed_btn_new_template).setOnClickListener(v -> {
            final EditText ev = new EditText(this);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.xposed_new_template))
                    .setView(ev)
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setPositiveButton(getString(R.string.accept), ((dialog, which) -> {
                        String name = ev.getText().toString();
                        if (templates.contains(name) || name.isEmpty())
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(getString(R.string.error))
                                    .setMessage(getString(R.string.xposed_template_already_exists))
                                    .setPositiveButton(getString(R.string.accept), null)
                                    .show();
                        else {
                            templates.add(name);
                            adapter.add(name);
                            list_pref.edit().putStringSet("List", templates).apply();
                            EditTemplate(name);
                        }
                    })).setCancelable(false).show();
        });
    }

    private void InitTemplateList() {
        list_pref = getSharedPreferences("Templates", Context.MODE_PRIVATE);
        templates = list_pref.getStringSet("List", new HashSet<>());
        ListView lv = findViewById(R.id.xposed_lv_template);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(templates));
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((parent, view, position, id) -> EditTemplate(((TextView) view).getText().toString()));
    }

    private void EditTemplate(String name) {

    }

}