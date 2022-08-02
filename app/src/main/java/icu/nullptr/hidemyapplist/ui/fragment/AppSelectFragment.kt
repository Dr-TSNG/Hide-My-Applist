package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentAppSelectBinding
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.ui.viewmodel.AppSelectViewModel

class AppSelectFragment : Fragment(R.layout.fragment_app_select) {

    private val binding by viewBinding<FragmentAppSelectBinding>()
    private val args by navArgs<AppSelectFragmentArgs>()
    private val viewModel by viewModels<AppSelectViewModel> {
        AppSelectViewModel.Factory(args)
    }

    private fun onBack() {
        if (viewModel.isMultiSelect) {
            setFragmentResult("app_select", Bundle().apply {
                putStringArrayList("checked", ArrayList(viewModel.checked))
            })
        }
        navController.navigateUp()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBack() }
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_app_select),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { onBack() }
        )

        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = viewModel.adapter
    }
}
