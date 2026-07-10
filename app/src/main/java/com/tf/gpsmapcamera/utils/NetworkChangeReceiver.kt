package com.tf.gpsmapcamera.utils

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
import com.tf.gpsmapcamera.R


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
        val dialogView =
            LayoutInflater.from(activity).inflate(R.layout.dialog_internet_connection, null)
        val dialog = AlertDialog.Builder(activity).create().apply {
            setView(dialogView)
            setCancelable(false)
        }

        dialogView.findViewById<TextView>(R.id.btnYes).setClickWithTimeout {
            isDialogShowing = false
            dialog.dismiss()
            activity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }

        dialogView.findViewById<TextView>(R.id.btnNo).setClickWithTimeout {
            isDialogShowing = false
            dialog.dismiss()
        }

        dialog.setOnDismissListener { isDialogShowing = false }
        dialog.show()
    }
}
