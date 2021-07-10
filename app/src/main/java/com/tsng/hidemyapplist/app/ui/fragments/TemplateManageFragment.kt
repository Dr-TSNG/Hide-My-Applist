package com.tsng.hidemyapplist.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.tsng.hidemyapplist.app.JsonConfigManager.globalConfig
import com.tsng.hidemyapplist.app.startFragment
import com.tsng.hidemyapplist.app.ui.adapters.TemplateListAdapter
import com.tsng.hidemyapplist.databinding.FragmentTemplateManageBinding
import java.text.Collator
import java.util.*

class TemplateManageFragment : Fragment() {
    private lateinit var binding: FragmentTemplateManageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTemplateManageBinding.inflate(inflater, container, false)
        binding.createBlacklist.setOnClickListener {
            startFragment(TemplateSettingsFragment.newInstance(false, null))
        }
        binding.createWhitelist.setOnClickListener {
            startFragment(TemplateSettingsFragment.newInstance(true, null))
        }
        buildTemplateList()
        return binding.root
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (!hidden) buildTemplateList()
    }

    private fun buildTemplateList() {
        val adapterList = mutableListOf<Pair<String, Boolean>>()
        for ((name, template) in globalConfig.templates)
            adapterList.add(name to template.isWhitelist)
        adapterList.sortWith { o1, o2 ->
            if (o1.second != o2.second) if (o1.second) 1 else -1
            else Collator.getInstance(Locale.getDefault()).compare(o1.first, o2.first)
        }
        binding.templateList.layoutManager = LinearLayoutManager(activity)
        binding.templateList.adapter = TemplateListAdapter(adapterList, this)
    }
}