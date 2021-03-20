package com.tsng.hidemyapplist;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class XposedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xposed);
    }
}