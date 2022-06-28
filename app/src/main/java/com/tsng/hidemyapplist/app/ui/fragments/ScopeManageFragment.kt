package com.tsng.hidemyapplist.app.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tsng.hidemyapplist.BuildConfig
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.JsonConfigManager.globalConfig
import com.tsng.hidemyapplist.app.helpers.AppInfoHelper
import com.tsng.hidemyapplist.app.startFragment
import com.tsng.hidemyapplist.app.ui.adapters.AppSelectAdapter
import com.tsng.hidemyapplist.app.ui.views.Ads
import com.tsng.hidemyapplist.databinding.FragmentAppSelectBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.*

class ScopeManageFragment : Fragment() {

    private lateinit var binding: FragmentAppSelectBinding
    private var adapter: AppSelectAdapter? = null
    private var isShowSystemApp = false
    private var touchedItemPosition = -1

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
        binding.adBanner.loadAd(Ads.appSelectAd)
        binding.refreshLayout.setOnRefreshListener {
            lifecycleScope.launch { refresh() }
        }
        binding.refreshLayout.isRefreshing = true
        lifecycleScope.launch { refresh() }
        return binding.root
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden && touchedItemPosition != -1)
            adapter?.notifyItemChanged(touchedItemPosition)
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

    private suspend fun refresh() {
            initAppListView()
            withContext(Dispatchers.Main) {
                binding.refreshLayout.isRefreshing = false
            }
    }

    private suspend fun initAppListView() {
        val appInfoList = AppInfoHelper.getAppInfoList()
        appInfoList.removeIf { it.packageName == BuildConfig.APPLICATION_ID }
        val selectedApps = globalConfig.scope.keys
        appInfoList.sortWith { o1, o2 ->
            val b1 = selectedApps.contains(o1.packageName)
            val b2 = selectedApps.contains(o2.packageName)
            if (b1 != b2) return@sortWith if (b1) -1 else 1
            Collator.getInstance(Locale.getDefault()).compare(o1.appName, o2.appName)
        }
        withContext(Dispatchers.Main) {
            binding.appSelect.layoutManager = LinearLayoutManager(activity)
            adapter = AppSelectAdapter(isShowSystemApp, false, appInfoList, selectedApps) {
                itemView.setOnClickListener {
                    touchedItemPosition = layoutPosition
                    val packageName = packageNameTextView.text.toString()
                    startFragment(AppSettingsFragment.newInstance(packageName))
                }
            }
            binding.appSelect.adapter = adapter
        }
    }
}
