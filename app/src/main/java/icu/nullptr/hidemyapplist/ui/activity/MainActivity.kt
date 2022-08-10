package icu.nullptr.hidemyapplist.ui.activity

import android.content.res.Resources
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.google.android.gms.ads.MobileAds
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.ActivityMainBinding
import rikka.material.app.MaterialActivity

class MainActivity : MaterialActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        setupWithNavController(binding.bottomNav, navController)

        MobileAds.initialize(this)
    }

    override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
        theme.applyStyle(rikka.material.preference.R.style.ThemeOverlay_Rikka_Material3_Preference, true)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
