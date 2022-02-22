package com.tsng.hidemyapplist.app.ui.activities

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.startFragment
import com.tsng.hidemyapplist.app.ui.fragments.ScopeManageFragment
import com.tsng.hidemyapplist.app.ui.fragments.SettingsFragment
import com.tsng.hidemyapplist.app.ui.fragments.TemplateManageFragment
import com.tsng.hidemyapplist.databinding.ActivityModuleBinding

class ModuleActivity : AppCompatActivity() {
    enum class Fragment {
        TEMPLATE_MANAGE,
        SCOPE_MANAGE,
        SETTINGS
    }

    private lateinit var binding: ActivityModuleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        binding = ActivityModuleBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
            Fragment.SETTINGS -> {
                setTitle(R.string.title_settings)
                startFragment(SettingsFragment(), false)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}