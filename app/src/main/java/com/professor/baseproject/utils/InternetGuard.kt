package com.professor.baseproject.utils

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.professor.baseproject.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * App-wide offline guard: shows a non-dismissable dialog while the device is offline and
 * offers a route to the system network settings.
 *
 * Attach once per screen that requires connectivity:
 * ```
 * InternetGuard.attach(this, connectivityObserver)
 * ```
 *
 * Notes on the design:
 *  - Driven by [ConnectivityObserver], the single connectivity source in this project. An
 *    earlier `NetworkChangeReceiver` existed but was never registered anywhere, so the app
 *    had no offline handling at all despite appearing to.
 *  - Collected inside `repeatOnLifecycle(STARTED)` so it never touches a stopped Activity,
 *    and the dialog is dismissed on the way out to avoid a leaked window.
 *  - The dialog auto-dismisses when connectivity returns; the user does not have to act.
 */
object InternetGuard {

    fun attach(activity: Activity, observer: ConnectivityObserver) {
        val owner = activity as? LifecycleOwner ?: return
        var dialog: AlertDialog? = null

        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    // Seeded with the *current* state. ConnectivityObserver is built on
                    // NetworkCallback, which only reports transitions — so an app launched
                    // while already offline would otherwise never receive an event and the
                    // guard would silently do nothing.
                    if (!observer.isConnected() && !activity.isFinishing) {
                        dialog = showOfflineDialog(activity)
                    }

                    observer.observe()
                        .distinctUntilChanged()
                        .collect { status ->
                            if (status == NetworkStatus.Available) {
                                dialog?.dismiss()
                                dialog = null
                            } else if (dialog == null && !activity.isFinishing) {
                                dialog = showOfflineDialog(activity)
                            }
                        }
                } finally {
                    // repeatOnLifecycle cancels this block on STOP; drop the window so it
                    // cannot outlive the Activity.
                    dialog?.dismiss()
                    dialog = null
                }
            }
        }
    }

    private fun showOfflineDialog(activity: Activity): AlertDialog =
        AlertDialog.Builder(activity)
            .setTitle(R.string.no_internet_title)
            .setMessage(R.string.no_internet_message)
            .setCancelable(false)
            .setPositiveButton(R.string.open_network_settings) { _, _ ->
                openNetworkSettings(activity)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()

    /**
     * Opens Wi-Fi settings, falling back to the top-level settings screen. Some OEM builds
     * and restricted profiles do not expose ACTION_WIFI_SETTINGS, and an unguarded
     * startActivity there throws ActivityNotFoundException.
     */
    private fun openNetworkSettings(activity: Activity) {
        val candidates = listOf(
            Intent(Settings.ACTION_WIFI_SETTINGS),
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in candidates) {
            if (runCatching { activity.startActivity(intent); true }.getOrDefault(false)) return
        }
    }
}
