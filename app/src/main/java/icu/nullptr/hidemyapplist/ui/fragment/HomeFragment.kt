package icu.nullptr.hidemyapplist.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialElevationScale
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentHomeBinding
import icu.nullptr.hidemyapplist.data.fetchLatestUpdate
import icu.nullptr.hidemyapplist.hmaApp
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.service.ServiceHelper
import icu.nullptr.hidemyapplist.ui.activity.AboutActivity
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.getColor
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.themeColor
import icu.nullptr.hidemyapplist.ui.util.makeToast
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val binding by viewBinding<FragmentHomeBinding>()

    private val backupSAFLauncher =
        registerForActivityResult(CreateDocument("application/json")) backup@{ uri ->
            if (uri == null) return@backup
            ConfigManager.configFile.inputStream().use { input ->
                hmaApp.contentResolver.openOutputStream(uri).use { output ->
                    if (output == null) makeToast(R.string.home_export_failed)
                    else input.copyTo(output)
                }
            }
            makeToast(R.string.home_exported)
        }

    private val restoreSAFLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) restore@{ uri ->
            if (uri == null) return@restore
            runCatching {
                val backup = hmaApp.contentResolver
                    .openInputStream(uri)?.reader().use { it?.readText() }
                    ?: throw IOException(getString(R.string.home_import_file_damaged))
                ConfigManager.importConfig(backup)
                makeToast(R.string.home_import_successful)
            }.onFailure {
                it.printStackTrace()
                MaterialAlertDialogBuilder(requireContext())
                    .setCancelable(false)
                    .setTitle(R.string.home_import_failed)
                    .setMessage(it.message)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(R.string.show_crash_log) { _, _ ->
                        MaterialAlertDialogBuilder(requireActivity())
                            .setCancelable(false)
                            .setTitle(R.string.home_import_failed)
                            .setMessage(it.stackTraceToString())
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    }
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialElevationScale(false)
        reenterTransition = MaterialElevationScale(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.app_name),
            menuRes = R.menu.menu_about,
            onMenuOptionSelected = {
                startActivity(Intent(requireContext(), AboutActivity::class.java))
            }
        )

        runCatching {
            binding.adBanner.loadAd(AdRequest.Builder().build())
        }
        binding.templateManage.setOnClickListener {
            val extras = FragmentNavigatorExtras(binding.manageCard to "transition_manage")
            navController.navigate(R.id.nav_template_manage, null, null, extras)
        }
        binding.appManage.setOnClickListener {
            navController.navigate(R.id.nav_app_manage)
        }
        binding.detectionTest.setOnClickListener {
            val intent = hmaApp.packageManager.getLaunchIntentForPackage("icu.nullptr.applistdetector")
            if (intent == null) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.home_download_test_app_title)
                    .setMessage(R.string.home_download_test_app_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Dr-TSNG/ApplistDetector/releases")))
                    }
                    .show()
            } else startActivity(intent)
        }
        binding.backupConfig.setOnClickListener {
            backupSAFLauncher.launch("HMA_Config.json")
        }
        binding.restoreConfig.setOnClickListener {
            restoreSAFLauncher.launch("application/json")
        }

        lifecycleScope.launch {
            loadUpdateDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        val serviceVersion = ServiceHelper.getServiceVersion()
        val color = when {
            !hmaApp.isHooked -> getColor(R.color.gray)
            serviceVersion == 0 -> getColor(R.color.invalid)
            else -> themeColor(android.R.attr.colorPrimary)
        }
        binding.statusCard.setCardBackgroundColor(color)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.statusCard.outlineAmbientShadowColor = color
            binding.statusCard.outlineSpotShadowColor = color
        }
        if (hmaApp.isHooked) {
            binding.moduleStatusIcon.setImageResource(R.drawable.outline_done_all_24)
            val versionNameSimple = BuildConfig.VERSION_NAME.substringBefore(".r")
            binding.moduleStatus.text = String.format(getString(R.string.home_xposed_activated), versionNameSimple, BuildConfig.VERSION_CODE)
        } else {
            binding.moduleStatusIcon.setImageResource(R.drawable.outline_extension_off_24)
            binding.moduleStatus.setText(R.string.home_xposed_not_activated)
        }
        if (serviceVersion != 0) {
            if (serviceVersion < icu.nullptr.hidemyapplist.common.BuildConfig.SERVICE_VERSION) {
                binding.serviceStatus.text = String.format(getString(R.string.home_xposed_service_old))
            } else {
                binding.serviceStatus.text = String.format(getString(R.string.home_xposed_service_on), serviceVersion)
            }
            binding.filterCount.visibility = View.VISIBLE
            binding.filterCount.text = String.format(getString(R.string.home_xposed_filter_count), ServiceHelper.getFilterCount())
        } else {
            binding.serviceStatus.setText(R.string.home_xposed_service_off)
            binding.filterCount.visibility = View.GONE
        }
    }

    private suspend fun loadUpdateDialog() {
        if (PrefManager.disableUpdate) return
        val updateInfo = fetchLatestUpdate() ?: return
        if (updateInfo.versionCode > BuildConfig.VERSION_CODE) {
            withContext(Dispatchers.Main) {
                MaterialAlertDialogBuilder(requireContext())
                    .setCancelable(false)
                    .setTitle(getString(R.string.home_new_update, updateInfo.versionName))
                    .setMessage(Html.fromHtml(updateInfo.content, Html.FROM_HTML_MODE_COMPACT))
                    .setPositiveButton("GitHub") { _, _ ->
                        startActivity(Intent(Intent.ACTION_VIEW, updateInfo.downloadUrl.toUri()))
                    }
                    .setNegativeButton("Telegram") { _, _ ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/HideMyApplist")))
                    }
                    .setNeutralButton(android.R.string.cancel, null)
                    .show()
            }
        } else if (updateInfo.versionCode > PrefManager.lastVersion) {
            withContext(Dispatchers.Main) {
                MaterialAlertDialogBuilder(requireContext())
                    .setCancelable(false)
                    .setTitle(getString(R.string.home_update, updateInfo.versionName))
                    .setMessage(Html.fromHtml(updateInfo.content, Html.FROM_HTML_MODE_COMPACT))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        PrefManager.lastVersion = BuildConfig.VERSION_CODE
                    }
                    .show()
            }
        }
    }
}
