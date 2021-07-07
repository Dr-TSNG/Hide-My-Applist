package com.tsng.hidemyapplist.app.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.ui.fragments.TemplateSettingsFragment

class TemplateListAdapter(
    private val templateList: List<Pair<String, Boolean>>,
    private val mContext: Context
) : RecyclerView.Adapter<TemplateListAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.template_icon)
        val textView: TextView = view.findViewById(R.id.template_name)
        var isWhitelist = false
        lateinit var templateName: String
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_templates, parent, false)
        val viewHolder = ViewHolder(view)
        viewHolder.itemView.setOnClickListener {
            (mContext as AppCompatActivity).supportFragmentManager
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(
                    R.id.fragment_container,
                    TemplateSettingsFragment.newInstance(
                        viewHolder.isWhitelist,
                        viewHolder.templateName
                    )
                )
                .addToBackStack(null)
                .commit()
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pair = templateList[position]
        holder.imageView.setImageResource(
            if (pair.second) R.drawable.ic_file_white
            else R.drawable.ic_file_black
        )
        holder.textView.text = pair.first
        holder.templateName = pair.first
        holder.isWhitelist = pair.second
    }

    override fun getItemCount() = templateList.size
}