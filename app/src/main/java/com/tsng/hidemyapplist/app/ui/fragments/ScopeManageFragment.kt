package com.tsng.hidemyapplist.app.ui.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kyuubiran.ezxhelper.utils.runOnMainThread
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.JsonConfigManager.globalConfig
import com.tsng.hidemyapplist.app.helpers.AppInfoHelper
import com.tsng.hidemyapplist.app.startFragment
import com.tsng.hidemyapplist.app.ui.adapters.AppSelectAdapter
import com.tsng.hidemyapplist.databinding.FragmentAppSelectBinding
import java.text.Collator
import java.util.*
import kotlin.concurrent.thread

class ScopeManageFragment : Fragment() {
    private lateinit var binding: FragmentAppSelectBinding
    private var adapter: AppSelectAdapter? = null
    private var isShowSystemApp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppSelectBinding.inflate(inflater, container, false)
        binding.refreshLayout.setOnRefreshListener { refresh() }.autoRefresh()
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.appselect, menu)
        val searchView = menu.findItem(R.id.toolbar_search).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = false

            override fun onQueryTextChange(newText: String): Boolean {
                adapter?.filter?.filter(newText.lowercase(Locale.getDefault()))
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.toolbar_show_system_apps -> {
                isShowSystemApp = !isShowSystemApp
                item.isChecked = isShowSystemApp
                adapter?.isShowSystemApp = isShowSystemApp
                adapter?.filter?.filter("")
            }
            else -> return false
        }
        return true
    }

    private fun refresh() {
        thread {
            initAppListView()
            runOnMainThread { binding.refreshLayout.finishRefresh() }
        }
    }

    private fun initAppListView() {
        val appInfoList = AppInfoHelper.getAppInfoList()
        val selectedApps = globalConfig.scope.keys
        appInfoList.sortWith { o1, o2 ->
            val b1 = selectedApps.contains(o1.packageName)
            val b2 = selectedApps.contains(o2.packageName)
            if (b1 != b2) return@sortWith if (b1) -1 else 1
            Collator.getInstance(Locale.getDefault()).compare(o1.appName, o2.appName)
        }
        runOnMainThread {
            binding.appSelect.layoutManager = LinearLayoutManager(activity)
            adapter = AppSelectAdapter(isShowSystemApp, false, appInfoList, selectedApps) {
                val packageName = it.findViewById<TextView>(R.id.app_package_name).text.toString()
                startFragment(AppSettingsFragment.newInstance(packageName))
            }
            binding.appSelect.adapter = adapter
        }
    }
}