package com.tf.phonecleaner.booster.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.TextView
import com.tf.phonecleaner.booster.R


class NetworkChangeReceiver(private val activity: Activity) : BroadcastReceiver() {

    /** Prevents stacking duplicate dialogs if connectivity toggled rapidly. */
    private var isDialogShowing = false

    override fun onReceive(context: Context, intent: Intent) {
        if (!isConnected(context) && !isDialogShowing) {
            showInternetConnectionDialog()
        }
    }

    private fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true  // Can't determine — assume connected to avoid false alarms
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun showInternetConnectionDialog() {
        // FIX: avoid WindowManager$BadTokenException on a finishing/destroyed Activity
        if (activity.isFinishing || activity.isDestroyed) return

        isDialogShowing = true
        val dialog = AlertDialog.Builder(activity)
            .setTitle("No Internet Connection")
            .setMessage("Please check your internet connection to continue.")
            .setCancelable(false)
            .setPositiveButton("Settings") { d, _ ->
                isDialogShowing = false
                d.dismiss()
                activity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            }
            .setNegativeButton("Cancel") { d, _ ->
                isDialogShowing = false
                d.dismiss()
            }
            .create()

        dialog.setOnDismissListener { isDialogShowing = false }
        dialog.show()
    }
}
