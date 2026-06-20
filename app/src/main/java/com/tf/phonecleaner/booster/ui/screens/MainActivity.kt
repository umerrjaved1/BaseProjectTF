package com.tf.phonecleaner.booster.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.tf.phonecleaner.booster.app.AnalyticsManager
import com.tf.phonecleaner.booster.R
import com.tf.phonecleaner.booster.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var analyticsManager: AnalyticsManager

    @Inject
    lateinit var repository: com.tf.phonecleaner.booster.data.DocumentRepository

    private lateinit var binding: ActivityMainBinding
    private val homeFragment = com.tf.phonecleaner.booster.ui.fragments.HomeFragment()
    private val favoritesFragment = com.tf.phonecleaner.booster.ui.fragments.FavoritesFragment()
    private val settingsFragment = com.tf.phonecleaner.booster.ui.fragments.SettingsFragment()
    private var activeTag = "home"

    private val openDocumentLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
            }
            lifecycleScope.launch {
                repository.addOrUpdateFromUri(uri)
                startActivity(
                    Intent(this@MainActivity, DocumentViewerActivity::class.java)
                        .putExtra(DocumentViewerActivity.EXTRA_DOCUMENT_ID, uri.toString())
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBars()

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "MainActivity")
        analyticsManager.sendAnalytics(AnalyticsManager.Action.ACTION_TYPE, AnalyticsManager.Events.HOME_VIEW)

        if (savedInstanceState != null) {
            activeTag = savedInstanceState.getString("active_tag", "home")
        }

        setupFragments(savedInstanceState == null)
        setupClicks()
        setupBackPress()
        
        // Ensure correct icon visibility and title on recreation
        updateToolbarForFragment(activeTag)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("active_tag", activeTag)
    }

    private fun applySystemBars() {
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
    }

    private fun setupFragments(firstLaunch: Boolean) {
        if (firstLaunch) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, homeFragment, "home")
                .commit()
        } else {
            // Restore fragment reference if needed, but here we use the ones created in class
            // Actually, after recreation, we should find them by tag or just replace
            val fragment = when (activeTag) {
                "home" -> homeFragment
                "favorites" -> favoritesFragment
                else -> settingsFragment
            }
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, fragment, activeTag)
                .commit()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment("home")
                R.id.nav_favorites -> switchFragment("favorites")
                R.id.nav_settings -> switchFragment("settings")
            }
            true
        }
        
        // Correctly set selected item without triggering listener unnecessarily if possible
        binding.bottomNav.selectedItemId = when(activeTag) {
            "favorites" -> R.id.nav_favorites
            "settings" -> R.id.nav_settings
            else -> R.id.nav_home
        }
    }

    private fun switchFragment(tag: String) {
        if (tag == activeTag) return

        val fragment = when (tag) {
            "home" -> homeFragment
            "favorites" -> favoritesFragment
            else -> settingsFragment
        }

        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment, tag)
            .commit()

        activeTag = tag
        updateToolbarForFragment(tag)
    }

    private fun updateToolbarForFragment(tag: String) {
        binding.toolbar.title = when (tag) {
            "favorites" -> getString(R.string.favorites)
            "settings" -> getString(R.string.settings)
            else -> getString(R.string.app_name)
        }

        // Set navigation icon (back arrow) for non-home fragments
        if (tag == "home") {
            binding.toolbar.navigationIcon = null
        } else {
            binding.toolbar.setNavigationIcon(R.drawable.ic_back)
        }

        // Hide settings icon in SettingsFragment and FavoritesFragment
        binding.ivSetting.visibility = if (tag == "settings" || tag == "favorites") View.GONE else View.VISIBLE
    }

    fun switchFragmentTo(tag: String) {
        binding.bottomNav.selectedItemId = when (tag) {
            "home" -> R.id.nav_home
            "favorites" -> R.id.nav_favorites
            "settings" -> R.id.nav_settings
            else -> R.id.nav_home
        }
    }

    private fun setupClicks() {

        binding.ivSetting.setOnClickListener {
            switchFragmentTo("settings")
        }

        binding.toolbar.setNavigationOnClickListener {
            if (activeTag != "home") {
                switchFragmentTo("home")
            } else {
                openDocumentLauncher.launch(arrayOf("*/*"))
            }
        }
    }


    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activeTag != "home") {
                    switchFragmentTo("home")
                } else {
                    finish()
                }
            }
        })
    }
}

