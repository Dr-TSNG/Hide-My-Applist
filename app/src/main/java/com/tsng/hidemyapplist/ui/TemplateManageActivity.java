package com.tsng.hidemyapplist.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tsng.hidemyapplist.R;

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
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        InitTemplateList();
        findViewById(R.id.xposed_btn_new_template).setOnClickListener(v -> {
            final EditText ev = new EditText(this);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.template_new)
                    .setView(ev)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, ((dialog, which) -> {
                        String name = ev.getText().toString();
                        if (templates.contains(name) || name.isEmpty() || name.contains("/"))
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.error)
                                    .setMessage(R.string.template_name_invalid)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show();
                        else {
                            templates.add(name);
                            adapter.add(name);
                            list_pref.edit().putStringSet("List", new HashSet<>(templates)).apply();
                            EditTemplate(name);
                        }
                    })).setCancelable(false).show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void InitTemplateList() {
        list_pref = getSharedPreferences("Templates", Context.MODE_PRIVATE);
        templates = new HashSet<>(list_pref.getStringSet("List", new HashSet<>()));
        ListView lv = findViewById(R.id.xposed_lv_template);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(templates));
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((parent, view, position, id) -> EditTemplate(((TextView) view).getText().toString()));
        lv.setOnItemLongClickListener((parent, view, position, id) -> {
            String s = ((TextView) view).getText().toString();
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.template_delete_confirm)
                    .setMessage(s)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, ((dialog, which) -> {
                        templates.remove(s);
                        adapter.remove(s);
                        deleteSharedPreferences("tpl_" + s);
                        list_pref.edit().putStringSet("List", new HashSet<>(templates)).apply();
                    })).show();
            return true;
        });
    }

    private void EditTemplate(String name) {
        Intent intent = new Intent(this, TemplateSettingsActivity.class);
        intent.putExtra("template", name);
        startActivity(intent);
    }

}