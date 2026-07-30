package com.professor.baseproject.utils

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.professor.baseproject.R

/**
 * Runtime-permission helper that handles the "permanently denied" case, which is where
 * hand-rolled permission code usually breaks.
 *
 * Android gives no direct "was this permanently denied?" signal. The reliable test is:
 *
 *   not granted  AND  shouldShowRequestPermissionRationale == false  AND  we have asked before
 *
 * The "asked before" part cannot be derived from the framework — it must be remembered — so
 * this class persists it. Without that memory, a first-ever request looks identical to a
 * permanent denial (both report `false`), and apps wrongly send a first-time user straight
 * to the system Settings screen.
 *
 * Usage — construct in `onCreate` (the launcher must be registered before STARTED):
 * ```
 * private val cameraPermission by lazy {
 *     PermissionRequester(this, Manifest.permission.CAMERA, appPreferences)
 * }
 * ...
 * cameraPermission.request(
 *     onGranted = { openCamera() },
 *     onDenied = { showInlineHint() }
 * )
 * ```
 *
 * `onPermanentlyDenied` is optional — the Settings dialog is already offered before it runs.
 */
class PermissionRequester(
    private val activity: AppCompatActivity,
    private val permission: String,
    private val preferences: com.professor.baseproject.app.AppPreferences,
    /** Shown in the "go to Settings" dialog; defaults to a generic message. */
    private val rationaleMessageRes: Int = R.string.permission_required_message
) {

    private var onGranted: (() -> Unit)? = null
    private var onDenied: (() -> Unit)? = null
    private var onPermanentlyDenied: (() -> Unit)? = null

    private val askedKey = "$PREF_ASKED_PREFIX$permission"

    private val launcher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            preferences.setBoolean(askedKey, true)
            when {
                granted -> onGranted?.invoke()
                isPermanentlyDenied() -> {
                    showSettingsDialog()
                    onPermanentlyDenied?.invoke()
                }
                else -> onDenied?.invoke()
            }
        }

    fun isGranted(): Boolean =
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * True when the OS will no longer show the system prompt, so the only route left is the
     * app's Settings page. Requires [askedKey] because the framework reports `false` both
     * for "never asked" and "permanently denied".
     */
    fun isPermanentlyDenied(): Boolean {
        if (isGranted()) return false
        val hasAsked = preferences.getBoolean(askedKey, false)
        return hasAsked && !activity.shouldShowRequestPermissionRationale(permission)
    }

    fun request(
        onGranted: () -> Unit,
        onDenied: (() -> Unit)? = null,
        onPermanentlyDenied: (() -> Unit)? = null
    ) {
        this.onGranted = onGranted
        this.onDenied = onDenied
        this.onPermanentlyDenied = onPermanentlyDenied

        when {
            isGranted() -> onGranted()
            // Second (and later) denial: the system prompt is a no-op now, so go straight
            // to the app's Settings page instead of appearing to do nothing.
            isPermanentlyDenied() -> {
                showSettingsDialog()
                onPermanentlyDenied?.invoke()
            }
            else -> launcher.launch(permission)
        }
    }

    private fun showSettingsDialog() {
        if (activity.isFinishing || activity.isDestroyed) return
        AlertDialog.Builder(activity)
            .setTitle(R.string.permission_required_title)
            .setMessage(rationaleMessageRes)
            .setPositiveButton(R.string.open_app_settings) { _, _ -> openAppSettings() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null)
        )
        runCatching { activity.startActivity(intent) }
    }

    private companion object {
        const val PREF_ASKED_PREFIX = "perm_asked_"
    }
}
