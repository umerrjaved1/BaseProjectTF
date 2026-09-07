package com.example.message.recovery.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import com.example.message.recovery.R

class NetworkChangeReceiver(private val activity: Activity) : BroadcastReceiver() {

    private var isDialogShowing = false

    override fun onReceive(context: Context, intent: Intent) {
        if (!isConnected(context) && !isDialogShowing) {
            showInternetConnectionDialog()
        }
    }

    private fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun showInternetConnectionDialog() {
        if (activity.isFinishing || activity.isDestroyed) return
        isDialogShowing = true
        val dialog = AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.no_internet_connection))
            .setMessage(activity.getString(R.string.do_you_want_to_open_network_settings))
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.yes)) { d, _ ->
                isDialogShowing = false
                d.dismiss()
                activity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            }
            .setNegativeButton(activity.getString(R.string.no)) { d, _ ->
                isDialogShowing = false
                d.dismiss()
            }
            .create()
        dialog.setOnDismissListener { isDialogShowing = false }
        dialog.show()
    }
}
