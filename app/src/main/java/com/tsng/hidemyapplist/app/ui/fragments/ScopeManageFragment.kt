package com.tsng.hidemyapplist.app.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kyuubiran.ezxhelper.utils.runOnMainThread
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.helpers.AppInfoHelper
import com.tsng.hidemyapplist.app.ui.adapters.AppSelectAdapter
import com.tsng.hidemyapplist.databinding.FragmentAppSelectBinding
import java.text.Collator
import java.util.*
import kotlin.concurrent.thread

class ScopeManageFragment : Fragment() {
    private lateinit var binding: FragmentAppSelectBinding
    private var adapter: AppSelectAdapter? = null
    private var isShowSystemApp = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
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
            else -> return super.onOptionsItemSelected(item)
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
        appInfoList.sortWith { o1, o2 ->
            Collator.getInstance(Locale.getDefault()).compare(o1.appName, o2.appName)
        }
        runOnMainThread {
            binding.appSelect.layoutManager = LinearLayoutManager(activity)
            adapter = AppSelectAdapter(isShowSystemApp, false, appInfoList, null) {

            }
            binding.appSelect.adapter = adapter
        }
    }
}