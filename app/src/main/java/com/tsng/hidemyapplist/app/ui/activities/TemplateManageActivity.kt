package com.tsng.hidemyapplist.app.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.ui.fragments.TemplateManageFragment

class TemplateManageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_manage)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, TemplateManageFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}