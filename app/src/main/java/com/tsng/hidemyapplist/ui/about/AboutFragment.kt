package com.tsng.hidemyapplist.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsng.hidemyapplist.R
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element

class AboutFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return AboutPage(activity)
                .setDescription(getString(R.string.about_my_description))
                .addItem(Element().apply {
                    title = "Version " + requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
                    iconDrawable = R.drawable.ic_baseline_language_24
                })
                .addItem(Element().apply {
                    title = getString(R.string.about_my_how_to_use_title)
                    iconDrawable = R.drawable.ic_baseline_help_24
                    setOnClickListener {
                        MaterialAlertDialogBuilder(requireContext())
                                .setTitle(title)
                                .setMessage(getString(R.string.about_my_how_to_use_message))
                                .setPositiveButton(getString(R.string.accept), null)
                                .show()
                    }
                })
                .addWebsite("http://www.coolapk.com/u/1911298", getString(R.string.about_my_author))
                .addGitHub("Dr-TSNG/Hide-My-Applist", getString(R.string.about_my_project_address))
                .addItem(Element())
                .create()
    }
}