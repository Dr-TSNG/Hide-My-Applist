package com.tsng.hidemyapplist.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.JsonConfigManager.globalConfig
import com.tsng.hidemyapplist.app.ui.adapters.TemplateListAdapter
import com.tsng.hidemyapplist.databinding.FragmentTemplateManageBinding
import java.text.Collator
import java.util.*

class TemplateManageFragment : Fragment() {
    lateinit var binding: FragmentTemplateManageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTemplateManageBinding.inflate(inflater, container, false)
        binding.createBlacklist.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.fragment_container, TemplateSettingsFragment.newInstance(false, null))
                .addToBackStack(null)
                .commit()
        }
        binding.createWhitelist.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.fragment_container, TemplateSettingsFragment.newInstance(true, null))
                .addToBackStack(null)
                .commit()
        }
        buildList()
        return binding.root
    }

    private fun buildList() {
        val adapterList = mutableListOf<Pair<String, Boolean>>()
        for ((name, template) in globalConfig.templates)
            adapterList.add(Pair(name, template.isWhiteList))
        adapterList.sortWith { o1, o2 ->
            if (o1.second != o2.second) if (o1.second) 1 else -1
            else Collator.getInstance(Locale.getDefault()).compare(o1.first, o2.first)
        }
        binding.templateList.layoutManager = LinearLayoutManager(activity)
        binding.templateList.adapter = TemplateListAdapter(adapterList, requireContext())
    }
}