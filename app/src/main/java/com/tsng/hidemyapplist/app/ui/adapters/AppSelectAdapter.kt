package com.tsng.hidemyapplist.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.app.helpers.AppInfoHelper.MyAppInfo
import java.util.*

class AppSelectAdapter(
    var isShowSystemApp: Boolean,
    private val hasCheckBox: Boolean,
    private val appList: List<MyAppInfo>,
    private val selectedApps: MutableSet<String>,
    private val onClickListener: View.OnClickListener? = null
) : Filterable,
    RecyclerView.Adapter<AppSelectAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.app_icon)
        val appNameTextView: TextView = view.findViewById(R.id.app_name)
        val packageNameTextView: TextView = view.findViewById(R.id.app_package_name)
        val summaryTextView: TextView = view.findViewById(R.id.app_summary)
        val checkBox: CheckBox = view.findViewById(R.id.checkbox)
    }

    private var mFilteredList: List<MyAppInfo> = listOf()

    init {
        filter.filter("")
    }

    override fun getFilter() = object : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            val filteredList = mutableListOf<MyAppInfo>()
            for (appInfo in appList) {
                if (!appInfo.isSystemApp || isShowSystemApp)
                    if (constraint.isEmpty() ||
                        appInfo.appName.lowercase(Locale.getDefault()).contains(constraint) ||
                        appInfo.packageName.lowercase(Locale.getDefault()).contains(constraint)
                    ) filteredList.add(appInfo)
            }
            return FilterResults().apply { values = filteredList }
        }

        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            mFilteredList = results.values as List<MyAppInfo>
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_app_select, parent, false)
        val viewHolder = ViewHolder(view)
        viewHolder.itemView.setOnClickListener(onClickListener)
        if (!hasCheckBox) viewHolder.checkBox.visibility = View.GONE
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appInfo = mFilteredList[position]
        holder.imageView.setImageDrawable(appInfo.icon)
        holder.appNameTextView.text = appInfo.appName
        holder.packageNameTextView.text = appInfo.packageName
        if (appInfo.isSystemApp) {
            holder.summaryTextView.visibility = View.VISIBLE
            holder.summaryTextView.setText(R.string.system_app)
        } else holder.summaryTextView.visibility = View.INVISIBLE

        if (hasCheckBox) {
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedApps.add(appInfo.packageName)
                else selectedApps.remove(appInfo.packageName)
            }
            holder.checkBox.isChecked = selectedApps.contains(appInfo.packageName)
        }
    }

    override fun getItemCount() = mFilteredList.size
}