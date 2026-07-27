package com.professor.baseproject.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val appUpdateManager = AppUpdateManagerFactory.create(context)
    private var installStateListener: InstallStateUpdatedListener? = null

    /**
     * Keyed by owner rather than a single slot. With one slot, StartActivity set it,
     * MainActivity overwrote it, and StartActivity.onDestroy — which runs *after*
     * MainActivity.onCreate — then nulled it, so the "update ready" dialog never
     * appeared anywhere.
     */
    private val downloadCompleteListeners = linkedMapOf<Any, () -> Unit>()

    private fun notifyDownloadComplete() {
        // Copy first: a listener may remove itself while being dispatched.
        downloadCompleteListeners.values.toList().forEach { it.invoke() }
    }

    sealed class UpdateCheckResult {
        data object NoUpdate : UpdateCheckResult()
        data class Available(
            val updateType: Int,
            val isImmediate: Boolean
        ) : UpdateCheckResult()
        data object Error : UpdateCheckResult()
    }

    /**
     * Bounded by [UPDATE_CHECK_TIMEOUT_MS]. Play's task can fail to call back at all
     * when the Play Store is in a bad state, and startup is gated on this — without a
     * timeout the splash screen hangs with no way out.
     */
    suspend fun checkForUpdate(): UpdateCheckResult {
        val appUpdateInfo = withTimeoutOrNull(UPDATE_CHECK_TIMEOUT_MS) { fetchAppUpdateInfo() }
            ?: return UpdateCheckResult.Error

        if (appUpdateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
            return UpdateCheckResult.NoUpdate
        }

        val updateType = resolveUpdateType(appUpdateInfo) ?: return UpdateCheckResult.NoUpdate

        return UpdateCheckResult.Available(
            updateType = updateType,
            isImmediate = updateType == AppUpdateType.IMMEDIATE
        )
    }

    fun startUpdate(
        activity: AppCompatActivity,
        updateType: Int,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (!appUpdateInfo.isUpdateTypeAllowed(updateType)) {
                Log.w(TAG, "Update type $updateType not allowed, opening Play Store")
                openPlayStore(activity)
                return@addOnSuccessListener
            }

            if (updateType == AppUpdateType.FLEXIBLE) {
                registerFlexibleUpdateListener()
            }

            try {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    launcher,
                    AppUpdateOptions.newBuilder(updateType).build()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start in-app update", e)
                openPlayStore(activity)
            }
        }.addOnFailureListener { error ->
            Log.e(TAG, "Failed to fetch update info for starting update", error)
            openPlayStore(activity)
        }
    }

    fun handleOnResume(
        activity: AppCompatActivity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            when {
                appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED -> {
                    notifyDownloadComplete()
                }

                appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                launcher,
                                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to resume immediate update", e)
                            openPlayStore(activity)
                        }
                    }
                }
            }
        }
    }

    /**
     * [onProceed] MUST be invoked on every path where the caller is expected to carry
     * on with startup. Previously only RESULT_OK/flexible and RESULT_CANCELED/immediate
     * were handled, so cancelling a *flexible* update — or any
     * RESULT_IN_APP_UPDATE_FAILED — left the splash screen waiting forever.
     */
    fun handleUpdateResult(
        resultCode: Int,
        isImmediate: Boolean,
        onImmediateCanceled: () -> Unit,
        onProceed: () -> Unit
    ) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                // An immediate update restarts the app on success, so there is
                // nothing to continue to in that case.
                if (!isImmediate) onProceed()
            }

            Activity.RESULT_CANCELED -> {
                if (isImmediate) onImmediateCanceled() else onProceed()
            }

            // RESULT_IN_APP_UPDATE_FAILED (1) and anything else Play may add later.
            else -> {
                Log.w(TAG, "In-app update finished with unexpected resultCode=$resultCode")
                if (isImmediate) onImmediateCanceled() else onProceed()
            }
        }
    }

    fun completeFlexibleUpdate() {
        appUpdateManager.completeUpdate()
    }

    /** [owner] is typically the Activity; pass the same instance to remove. */
    fun addOnFlexibleDownloadCompleteListener(owner: Any, listener: () -> Unit) {
        downloadCompleteListeners[owner] = listener
    }

    /**
     * Removes only [owner]'s listener. The Play [InstallStateUpdatedListener] is torn
     * down only once nobody is listening — the old code unregistered it from
     * StartActivity.onDestroy, abandoning tracking for a download it had just started.
     */
    fun removeOnFlexibleDownloadCompleteListener(owner: Any) {
        downloadCompleteListeners.remove(owner)
        if (downloadCompleteListeners.isEmpty()) {
            unregisterFlexibleUpdateListener()
        }
    }

    fun unregisterFlexibleUpdateListener() {
        installStateListener?.let { appUpdateManager.unregisterListener(it) }
        installStateListener = null
    }

    fun openPlayStore(context: Context) {
        val packageName = context.packageName
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(marketIntent)
        } catch (e: ActivityNotFoundException) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
        }
    }

    private fun registerFlexibleUpdateListener() {
        if (installStateListener != null) return

        installStateListener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                notifyDownloadComplete()
            }
        }
        installStateListener?.let { appUpdateManager.registerListener(it) }
    }

    private suspend fun fetchAppUpdateInfo(): AppUpdateInfo? =
        suspendCancellableCoroutine { continuation ->
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener {
                    Log.e(TAG, "Failed to check for update", it)
                    continuation.resume(null)
                }
        }

    private fun resolveUpdateType(appUpdateInfo: AppUpdateInfo): Int? {
        val prefersImmediate = appUpdateInfo.updatePriority() >= IMMEDIATE_UPDATE_PRIORITY

        return when {
            prefersImmediate && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                AppUpdateType.IMMEDIATE

            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                AppUpdateType.FLEXIBLE

            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                AppUpdateType.IMMEDIATE

            else -> null
        }
    }

    companion object {
        private const val TAG = "AppUpdateManager"
        private const val IMMEDIATE_UPDATE_PRIORITY = 4
        private const val UPDATE_CHECK_TIMEOUT_MS = 4_000L
    }
}
