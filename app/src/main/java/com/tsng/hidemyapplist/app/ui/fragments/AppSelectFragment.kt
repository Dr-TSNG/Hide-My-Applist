package com.tsng.hidemyapplist.app.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kyuubiran.ezxhelper.utils.runOnMainThread
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.helpers.AppInfoHelper
import com.tsng.hidemyapplist.app.ui.adapters.AppSelectAdapter
import com.tsng.hidemyapplist.databinding.FragmentAppSelectBinding
import java.text.Collator
import java.util.*
import kotlin.concurrent.thread

class AppSelectFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance(selectedApps: Array<String>) =
            AppSelectFragment().apply {
                arguments = bundleOf("selectedApps" to selectedApps)
            }
    }

    private lateinit var binding: FragmentAppSelectBinding
    private lateinit var selectedApps: MutableSet<String>
    private var adapter: AppSelectAdapter? = null
    private var isShowSystemApp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        selectedApps = arguments?.getStringArray("selectedApps")?.toHashSet() ?: mutableSetOf()
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

    override fun onDestroyView() {
        setFragmentResult(
            "appSelectResult",
            bundleOf("selectedApps" to selectedApps.toTypedArray())
        )
        super.onDestroyView()
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
        appInfoList.sortWith { o1, o2 ->
            val c1 = selectedApps.contains(o1.packageName)
            val c2 = selectedApps.contains(o2.packageName)
            if (c1 != c2) return@sortWith if (c1) -1 else 1
            Collator.getInstance(Locale.getDefault()).compare(o1.appName, o2.appName)
        }
        runOnMainThread {
            binding.appSelect.layoutManager = LinearLayoutManager(activity)
            adapter = AppSelectAdapter(isShowSystemApp, true, appInfoList, selectedApps)
            binding.appSelect.adapter = adapter
        }
    }
}