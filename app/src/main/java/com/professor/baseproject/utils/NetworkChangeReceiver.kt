package com.professor.baseproject.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.TextView
import com.professor.baseproject.R


class NetworkChangeReceiver(private val activity: Activity) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!isConnected(context)) {
            showInternetConnectionDialog()
        }
    }

    private fun isConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }

    private fun showInternetConnectionDialog() {
        val dialogView =
            LayoutInflater.from(activity).inflate(R.layout.dialog_internet_connection, null)
        val dialog = AlertDialog.Builder(activity).create().apply {
            setView(dialogView)
            setCancelable(false)
        }

        dialogView.findViewById<TextView>(R.id.btnYes).setClickWithTimeout {
            dialog.dismiss()
            activity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }

        dialogView.findViewById<TextView>(R.id.btnNo).setClickWithTimeout {
            dialog.dismiss()
        }

        dialog.show()
    }
}
