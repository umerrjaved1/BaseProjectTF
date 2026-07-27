package com.professor.baseproject.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.professor.baseproject.app.AnalyticsManager
import com.professor.baseproject.databinding.ActivityUninstallBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UninstallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUninstallBinding

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUninstallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        analyticsManager.sendAnalytics(AnalyticsManager.Action.OPENED, "activity_uninstall")

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnKeepApp.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        binding.btnUninstall.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
