package com.tf.phonecleaner.booster.ui.screens

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.cherry.lib.doc.bean.DocEngine
import com.cherry.lib.doc.bean.DocSourceType
import com.cherry.lib.doc.bean.FileType
import com.cherry.lib.doc.widget.DocView
import com.tf.phonecleaner.booster.R
import com.tf.phonecleaner.booster.databinding.ActivityDocumentViewerBinding
import com.tf.phonecleaner.booster.ui.viewmodel.DocumentViewerViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DocumentViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentViewerBinding
    private val viewModel: DocumentViewerViewModel by viewModels()
    private var currentZoomPercent = 100
    private var renderedUri: String? = null
    private var renderedMimeType: String? = null
    private var officeDocView: DocView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        val documentId = intent.getStringExtra(EXTRA_DOCUMENT_ID).orEmpty()

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnZoomIn.setOnClickListener { viewModel.zoomIn() }
        binding.btnZoomOut.setOnClickListener { viewModel.zoomOut() }
        binding.btnFavorite.setOnClickListener { viewModel.toggleFavorite() }

        viewModel.load(documentId)
        collectState()
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.toolbar.title = state.document?.name ?: getString(R.string.document_viewer)
                    binding.tvZoom.text = "${state.zoomPercent}%"
                    binding.btnFavorite.setImageResource(
                        if (state.document?.isFavorite == true) R.drawable.ic_star else R.drawable.ic_star_non_fil
                    )
                    renderDocument(state.document?.uriString, state.document?.mimeType)
                    applyZoom(state.zoomPercent)
                }
            }
        }
    }

    private fun renderDocument(uriString: String?, mimeType: String?) {
        if (uriString.isNullOrBlank()) {
            showUnsupported()
            return
        }
        if (uriString == renderedUri && mimeType == renderedMimeType) {
            return
        }

        val uri = Uri.parse(uriString)
        val normalizedMime = mimeType.orEmpty().lowercase()

        renderedUri = uriString
        renderedMimeType = mimeType
        currentZoomPercent = 100

        when {
            normalizedMime.contains("pdf") || uriString.endsWith(".pdf", true) -> showPdf(uri)
            normalizedMime.startsWith("image/") || uriString.endsWith(".jpg", true) ||
                uriString.endsWith(".jpeg", true) || uriString.endsWith(".png", true) ||
                uriString.endsWith(".webp", true) || uriString.endsWith(".gif", true) -> showImage(uri)

            normalizedMime.contains("text") || uriString.endsWith(".txt", true) -> showText(uri)
            normalizedMime.contains("word") || normalizedMime.contains("excel") ||
                normalizedMime.contains("powerpoint") || normalizedMime.contains("spreadsheet") ||
                normalizedMime.contains("presentation") || normalizedMime.contains("officedocument") ||
                uriString.endsWith(".doc", true) ||
                uriString.endsWith(".docx", true) || uriString.endsWith(".xls", true) ||
                uriString.endsWith(".xlsx", true) || uriString.endsWith(".ppt", true) ||
                uriString.endsWith(".pptx", true) -> openWithDocViewer(uriString, normalizedMime)

            else -> showUnsupported()
        }
    }

    private fun showPdf(uri: Uri) {
        resetVisibility()
        binding.pdfView.visibility = View.VISIBLE
        binding.btnZoomIn.visibility = View.VISIBLE
        binding.btnZoomOut.visibility = View.VISIBLE
        binding.tvBottomPage.visibility = View.VISIBLE
        binding.tvBottomPage.text = "PDF"
        binding.pdfView.initWithUri(uri)
    }

    private fun showImage(uri: Uri) {
        resetVisibility()
        binding.imageView.visibility = View.VISIBLE
        binding.btnZoomIn.visibility = View.GONE
        binding.btnZoomOut.visibility = View.GONE
        binding.tvBottomPage.visibility = View.GONE
        Glide.with(this)
            .load(uri)
            .into(binding.imageView)
    }

    private fun showText(uri: Uri) {
        resetVisibility()
        binding.textScroll.visibility = View.VISIBLE
        binding.btnZoomIn.visibility = View.GONE
        binding.btnZoomOut.visibility = View.GONE
        binding.tvBottomPage.visibility = View.GONE

        val content = try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            }
        } catch (_: Exception) {
            null
        }

        binding.tvTextContent.text = content ?: getString(R.string.unable_to_load_text)
    }

    private fun openWithDocViewer(uriString: String, normalizedMime: String) {
        val uri = Uri.parse(uriString)
        val resolvedPath = resolveOfficePath(uri)
        val sourceType = if (resolvedPath != null) DocSourceType.PATH else DocSourceType.URI
        val launchPath = resolvedPath ?: uriString
        val fileType = resolveOfficeFileType(uriString, normalizedMime)

        showOfficeDoc(launchPath, sourceType, fileType)
    }

    private fun showOfficeDoc(path: String, sourceType: Int, fileType: Int) {
        resetVisibility()
        binding.btnZoomIn.visibility = View.GONE
        binding.btnZoomOut.visibility = View.GONE
        binding.tvBottomPage.visibility = View.GONE

        val docView = officeDocView ?: DocView(this).also {
            it.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            binding.viewerContainer.addView(it)
            officeDocView = it
        }

        docView.visibility = View.VISIBLE
        docView.openDoc(this, path, sourceType, fileType, false, DocEngine.INTERNAL)
    }

    private fun resolveOfficePath(uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }

        if (uri.scheme != "content") {
            return null
        }

        return copyUriToCache(uri)?.absolutePath
    }

    private fun copyUriToCache(uri: Uri): File? {
        return try {
            val nameFromProvider = contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }

            val sanitizedName = (nameFromProvider ?: "office_${System.currentTimeMillis()}")
                .replace("[^a-zA-Z0-9._-]".toRegex(), "_")

            val targetFile = File(cacheDir, "${System.currentTimeMillis()}_$sanitizedName")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) targetFile else null
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveOfficeFileType(uriString: String, normalizedMime: String): Int {
        return when {
            normalizedMime.contains("officedocument.wordprocessingml") ||
                uriString.endsWith(".docx", true) -> FileType.DOCX

            normalizedMime.contains("word") || uriString.endsWith(".doc", true) -> FileType.DOC
            normalizedMime.contains("officedocument.spreadsheetml") ||
                uriString.endsWith(".xlsx", true) -> FileType.XLSX

            normalizedMime.contains("spreadsheet") || normalizedMime.contains("excel") ||
                uriString.endsWith(".xls", true) -> FileType.XLS

            normalizedMime.contains("officedocument.presentationml") ||
                uriString.endsWith(".pptx", true) -> FileType.PPTX

            normalizedMime.contains("presentation") || normalizedMime.contains("powerpoint") ||
                uriString.endsWith(".ppt", true) -> FileType.PPT

            else -> FileType.NOT_SUPPORT
        }
    }

    private fun showUnsupported() {
        resetVisibility()
        binding.tvUnsupported.visibility = View.VISIBLE
        binding.btnZoomIn.visibility = View.GONE
        binding.btnZoomOut.visibility = View.GONE
        binding.tvBottomPage.visibility = View.GONE
    }

    private fun resetVisibility() {
        binding.pdfView.visibility = View.GONE
        binding.imageView.visibility = View.GONE
        binding.textScroll.visibility = View.GONE
        binding.tvUnsupported.visibility = View.GONE
        officeDocView?.visibility = View.GONE
    }

    override fun onDestroy() {
        officeDocView?.onDestroy()
        officeDocView = null
        super.onDestroy()
    }

    private fun applyZoom(zoomPercent: Int) {
        if (binding.pdfView.visibility != View.VISIBLE) return
        if (zoomPercent == currentZoomPercent) return

        while (currentZoomPercent < zoomPercent) {
            binding.pdfView.zoomIn()
            currentZoomPercent += 10
        }
        while (currentZoomPercent > zoomPercent) {
            binding.pdfView.zoomOut()
            currentZoomPercent -= 10
        }
    }

    companion object {
        const val EXTRA_DOCUMENT_ID = "extra_document_id"
    }
}
