package com.tf.phonecleaner.booster.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.tf.phonecleaner.booster.adapter.DocumentListAdapter
import com.tf.phonecleaner.booster.adapter.QuickAccessAdapter
import com.tf.phonecleaner.booster.databinding.FragmentHomeBinding
import com.tf.phonecleaner.booster.ui.screens.DocumentViewerActivity
import com.tf.phonecleaner.booster.ui.screens.MainActivity
import com.tf.phonecleaner.booster.ui.screens.RecentFilesActivity
import com.tf.phonecleaner.booster.ui.viewmodel.HomeEvent
import com.tf.phonecleaner.booster.ui.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private val quickAccessAdapter by lazy {
        QuickAccessAdapter { viewModel.onQuickAccessClicked(it.type) }
    }

    private val recentAdapter by lazy {
        DocumentListAdapter(
            onItemClick = { viewModel.onRecentClicked(it.id) },
            onFavoriteClick = { viewModel.toggleFavorite(it.uriString) },
            compactMode = true
        )
    }

    private lateinit var openDocumentLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            viewModel.onUriPicked(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvQuickAccess.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvQuickAccess.adapter = quickAccessAdapter

        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecent.adapter = recentAdapter

        binding.btnBrowseFiles.setOnClickListener { viewModel.onBrowseClicked() }
        binding.tvViewAllRecent.setOnClickListener { viewModel.onRecentAllClicked() }

        collectUiState()
        collectEvents()
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    quickAccessAdapter.submitList(state.quickAccess)
                    recentAdapter.submitList(state.recentDocuments)
                    binding.groupEmptyRecent.visibility = if (state.recentDocuments.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun collectEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is HomeEvent.OpenFilePicker -> {
                            if (::openDocumentLauncher.isInitialized) {
                                openDocumentLauncher.launch(event.mimeTypes.toTypedArray())
                            }
                        }

                        is HomeEvent.OpenDocument -> {
                            val intent = Intent(requireContext(), DocumentViewerActivity::class.java)
                                .putExtra(DocumentViewerActivity.EXTRA_DOCUMENT_ID, event.documentId)
                            startActivity(intent)
                        }

                        HomeEvent.OpenRecentAll -> {
                            startActivity(Intent(requireContext(), RecentFilesActivity::class.java))
                        }
                        
                        HomeEvent.NavigateToFavorites -> {
                            (requireActivity() as? MainActivity)?.switchFragmentTo("favorites")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
