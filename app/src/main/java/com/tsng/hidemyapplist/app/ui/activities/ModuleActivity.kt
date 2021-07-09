package com.tsng.hidemyapplist.app.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.startFragment
import com.tsng.hidemyapplist.app.ui.fragments.ScopeManageFragment
import com.tsng.hidemyapplist.app.ui.fragments.TemplateManageFragment

class ModuleActivity : AppCompatActivity() {
    enum class Fragment {
        TEMPLATE_MANAGE,
        SCOPE_MANAGE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_module)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        when (intent.extras?.get("Fragment") as Fragment) {
            Fragment.TEMPLATE_MANAGE -> {
                setTitle(R.string.title_template_manage)
                startFragment(TemplateManageFragment(), false)
            }
            Fragment.SCOPE_MANAGE -> {
                setTitle(R.string.title_scope_manage)
                startFragment(ScopeManageFragment(), false)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}