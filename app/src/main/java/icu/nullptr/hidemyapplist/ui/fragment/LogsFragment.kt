package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentLogsBinding
import icu.nullptr.hidemyapplist.service.ServiceHelper
import icu.nullptr.hidemyapplist.ui.adapter.LogAdapter
import icu.nullptr.hidemyapplist.ui.util.setupToolbar


class LogsFragment : Fragment(R.layout.fragment_logs) {

    private val binding by viewBinding<FragmentLogsBinding>()
    private val adapter by lazy { LogAdapter(requireContext()) }

    private fun updateLogs() {
        val raw = ServiceHelper.getLogs()?.split("\n")
        if (raw == null) {
            binding.serviceOff.visibility = View.VISIBLE
        } else {
            binding.serviceOff.visibility = View.GONE
            adapter.logs = buildList {
                val cur = StringBuilder()
                for (line in raw) {
                    if (line.startsWith('[')) {
                        if (cur.isNotEmpty()) add(cur.toString())
                        cur.clear()
                    }
                    cur.append(line)
                }
                if (cur.isNotEmpty()) add(cur.toString())
                reverse()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupToolbar(binding.toolbar, getString(R.string.title_logs))
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = adapter
        binding.list.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        updateLogs()
    }
}
