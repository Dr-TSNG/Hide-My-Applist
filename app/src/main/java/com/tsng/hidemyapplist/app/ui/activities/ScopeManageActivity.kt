package com.tsng.hidemyapplist.app.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tsng.hidemyapplist.R

class ScopeManageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scope_manage)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}