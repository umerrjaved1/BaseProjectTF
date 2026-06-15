package com.mzalogics.docuview.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.mzalogics.docuview.R
import com.mzalogics.docuview.adapter.DocumentListAdapter
import com.mzalogics.docuview.data.DocumentRepository
import com.mzalogics.docuview.databinding.ActivityRecentFilesBinding
import com.mzalogics.docuview.model.DocumentItem
import com.mzalogics.docuview.ui.viewmodel.RecentFilesEvent
import com.mzalogics.docuview.ui.viewmodel.RecentFilesViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import java.util.Locale
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecentFilesActivity : AppCompatActivity() {

    @Inject
    lateinit var repository: DocumentRepository

    private lateinit var binding: ActivityRecentFilesBinding
    private val viewModel: RecentFilesViewModel by viewModels()

    private val adapter by lazy {
        DocumentListAdapter(
            onItemClick = { viewModel.onFileClicked(it.id) },
            onFavoriteClick = { viewModel.toggleFavorite(it.uriString) },
            onMoreClick = { anchor, item -> showFileMoreMenu(anchor, item) }
        )
    }

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
            }
            lifecycleScope.launch {
                repository.addOrUpdateFromUri(uri)
                startActivity(
                    Intent(this@RecentFilesActivity, DocumentViewerActivity::class.java)
                        .putExtra(DocumentViewerActivity.EXTRA_DOCUMENT_ID, uri.toString())
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecentFilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.fabAdd.setOnClickListener { viewModel.onOpenPickerClicked() }

        binding.rvRecentAll.layoutManager = LinearLayoutManager(this)
        binding.rvRecentAll.adapter = adapter

        collectState()
        collectEvents()
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentFiles.collect {
                    adapter.submitList(it)
                }
            }
        }
    }

    private fun collectEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is RecentFilesEvent.OpenDocument -> {
                            startActivity(
                                Intent(this@RecentFilesActivity, DocumentViewerActivity::class.java)
                                    .putExtra(DocumentViewerActivity.EXTRA_DOCUMENT_ID, event.documentId)
                            )
                        }

                        RecentFilesEvent.OpenFilePicker -> {
                            openDocumentLauncher.launch(arrayOf("*/*"))
                        }
                    }
                }
            }
        }
    }

    private fun showFileMoreMenu(anchor: View, item: DocumentItem) {
        val dialog = BottomSheetDialog(this)
        val content = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_file_actions, null)
        dialog.setContentView(content)

        content.findViewById<TextView>(R.id.tv_sheet_title).text = item.name
        content.findViewById<TextView>(R.id.action_toggle_favorite).text =
            getString(if (item.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites)

        content.findViewById<TextView>(R.id.action_open).setOnClickListener {
            dialog.dismiss()
            viewModel.onFileClicked(item.id)
        }

        content.findViewById<TextView>(R.id.action_toggle_favorite).setOnClickListener {
            dialog.dismiss()
            viewModel.toggleFavorite(item.uriString)
        }

        content.findViewById<TextView>(R.id.action_share).setOnClickListener {
            dialog.dismiss()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uriString))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_file)))
        }

        content.findViewById<TextView>(R.id.action_details).setOnClickListener {
            dialog.dismiss()
            showFileDetailsSheet(item)
        }

        dialog.show()
    }

    private fun showFileDetailsSheet(item: DocumentItem) {
        val detailsDialog = BottomSheetDialog(this)
        val detailsView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_file_details, null)
        detailsDialog.setContentView(detailsView)

        detailsView.findViewById<TextView>(R.id.tv_file_name).text =
            getString(R.string.file_name_value, item.name)
        detailsView.findViewById<TextView>(R.id.tv_file_type).text =
            getString(R.string.file_type_value, item.mimeType.ifBlank { "-" })
        detailsView.findViewById<TextView>(R.id.tv_file_size).text =
            getString(R.string.file_size_value, formatSize(item.sizeBytes))
        detailsView.findViewById<TextView>(R.id.tv_file_last_opened).text =
            getString(R.string.file_last_opened_value, formatDate(item.lastOpenedAt))

        detailsDialog.show()
    }

    private fun formatDate(timestamp: Long): String {
        return try {
            SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(timestamp))
        } catch (_: Exception) {
            "-"
        }
    }

    private fun formatSize(sizeBytes: Long): String {
        if (sizeBytes <= 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        val df = DecimalFormat("#.##")
        return when {
            sizeBytes >= gb -> "${df.format(sizeBytes / gb)} GB"
            sizeBytes >= mb -> "${df.format(sizeBytes / mb)} MB"
            sizeBytes >= kb -> "${df.format(sizeBytes / kb)} KB"
            else -> "$sizeBytes B"
        }
    }
}
