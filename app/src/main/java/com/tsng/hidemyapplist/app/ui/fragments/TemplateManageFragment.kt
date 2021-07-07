package com.tsng.hidemyapplist.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentTemplateManageBinding

class TemplateManageFragment : Fragment() {
    class TemplateListAdapter(val templateList: List<String>) :
        RecyclerView.Adapter<TemplateListAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val templateName: TextView = view.findViewById(R.id.template_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_templates, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val template = templateList[position]
            holder.templateName.text = template
        }

        override fun getItemCount() = templateList.size
    }

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
        return binding.root
    }
}