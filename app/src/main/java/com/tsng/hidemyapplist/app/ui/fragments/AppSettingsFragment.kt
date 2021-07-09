package com.tsng.hidemyapplist.app.ui.fragments

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentAppSettingsBinding

class AppSettingsFragment : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance(packageName: String) =
            AppSettingsFragment().apply {
                arguments = bundleOf("packageName" to packageName)
            }
    }

    private lateinit var binding: FragmentAppSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppSettingsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.delete_and_save, menu)
        menu.findItem(R.id.toolbar_delete).isVisible = false
    }
}