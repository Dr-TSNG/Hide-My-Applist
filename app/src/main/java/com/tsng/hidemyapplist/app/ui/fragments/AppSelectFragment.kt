package com.tsng.hidemyapplist.app.ui.fragments

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import com.tsng.hidemyapplist.app.MyApplication.Companion.appContext
import com.tsng.hidemyapplist.app.ui.adapters.AppSelectAdapter
import com.tsng.hidemyapplist.databinding.FragmentAppSelectBinding

class AppSelectFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance(selectedApps: Array<String>) =
            AppSelectFragment().apply {
                arguments = bundleOf("selectedApps" to selectedApps)
            }
    }

    private lateinit var binding: FragmentAppSelectBinding
    private var appList = mutableListOf<Pair<ApplicationInfo, Boolean>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppSelectBinding.inflate(inflater, container, false)
        initAppList()
        initAppListView()
        return binding.root
    }

    override fun onDestroyView() {
        val set = mutableSetOf<String>()
        for (pair in appList) if (pair.second) set.add(pair.first.packageName)
        setFragmentResult("appSelectResult", bundleOf("selectedApps" to set.toTypedArray()))
        super.onDestroyView()
    }

    private fun initAppList() {
        val selectedApps = arguments?.getStringArray("selectedApps")?.toHashSet() ?: setOf()
        val allApps = appContext.packageManager.getInstalledApplications(0)
        for (appInfo in allApps)
            appList.add(appInfo to selectedApps.contains(appInfo.packageName))
    }

    private fun initAppListView() {
        binding.appSelect.layoutManager = LinearLayoutManager(activity)
        binding.appSelect.adapter = AppSelectAdapter(appList)
    }
}