package com.tf.phonecleaner.booster.utils

import android.R
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding


object StatusBarUtils {

    const val STATUS_BAR_TAG = "status_bar"

    private fun AppCompatActivity.setupEdgeToEdge() {
        // Enable edge-to-edge by setting decor fits system windows to false
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout() or
                        WindowInsetsCompat.Type.ime()
            )
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            val themeBuilder = true
            // Configure system bar appearance
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = themeBuilder
            controller.isAppearanceLightNavigationBars = themeBuilder

            insets
        }
    }

    /**
     * Applies edge-to-edge display to an activity, handling system bars and display cutouts.
     * @param activity The activity to apply edge-to-edge to.
     * @param statusBarDrawable Optional drawable for the status bar background.
     * @param lightStatusBar Whether to use light status bar icons (dark icons on light background).
     * @param lightNavigationBar Whether to use light navigation bar icons.
     */

    @JvmStatic
    fun applyEdgeToEdge(
        activity: Activity,
        statusBarDrawable: Drawable? = null,
        statusBarColor: Int = Color.TRANSPARENT
    ) {

        val themeBuilder = true

        val window = activity.window ?: return
        val contentView = activity.findViewById<View>(R.id.content) ?: return

        // Enable edge-to-edge and make system bars transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            if (activity is ComponentActivity) {
                activity.enableEdgeToEdge()
            }
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//        window.statusBarColor = statusBarColor
        // Set navigation bar color for all APIs
        window.navigationBarColor = activity.getColor(R.color.white)


        // Configure system bar appearance
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = themeBuilder
        controller.isAppearanceLightNavigationBars = themeBuilder
        if (activity.javaClass.simpleName == "AdActivity" || activity.javaClass.simpleName == "StartActivity" || activity.javaClass.simpleName == "PremiumActivity") {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        Log.d(STATUS_BAR_TAG, "applyEdgeToEdge: is dark theme: ${themeBuilder}")

        // Apply insets to content view
        ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout() or
                        WindowInsetsCompat.Type.ime()
            )



            Log.d(STATUS_BAR_TAG, "applyEdgeToEdge: activityName: ${activity.javaClass.simpleName}")
            val statusBarHeight =
                if (activity.javaClass.simpleName == "AdActivity" || activity.javaClass.simpleName == "StartActivity" || activity.javaClass.simpleName == "PremiumActivity") {
                    0
                } else {
                    windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                }
            Log.d(
                STATUS_BAR_TAG,
                "applyEdgeToEdge: systemBars: $systemBars, statusBarHeight: $statusBarHeight"
            )
            // Apply custom status bar background if provided
            /*if (statusBarDrawable != null) {
                applyStatusBarBackground(window, statusBarDrawable, statusBarHeight)
            }*/

            // Update padding to avoid overlapping with system bars
            view.updatePadding(
                left = systemBars.left,
                top = statusBarHeight,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            windowInsets
        }
    }

    /**
     * Applies a custom background to the status bar area.
     * @param window The window to apply the background to.
     * @param background The drawable to use as the status bar background.
     * @param height The height of the status bar.
     * @return The created or updated status bar view.
     */
    private fun applyStatusBarBackground(window: Window, background: Drawable, height: Int): View {
        val parent =
            window.findViewById<ViewGroup>(R.id.content) ?: window.decorView as ViewGroup
        var statusBarView = parent.findViewWithTag<View>(STATUS_BAR_TAG)

        if (statusBarView != null) {
            statusBarView.visibility = View.VISIBLE
            statusBarView.background = background
        } else {
            statusBarView = createStatusBarView(window.context, background, height)
            parent.addView(statusBarView)
        }

        return statusBarView
    }

    /**
     * Creates a view to simulate the status bar background.
     * @param context The context to create the view.
     * @param background The drawable for the view background.
     * @param height The height of the status bar.
     * @return The created view.
     */
    private fun createStatusBarView(context: Context, background: Drawable?, height: Int): View {
        val statusBarView = View(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
            this.background = background
            tag = STATUS_BAR_TAG
        }
        return statusBarView
    }

    @JvmStatic
    fun hideSystemBars(
        activity: Activity,
        isShowNav: Boolean,
        transparentStatusBar: Boolean = false
    ) {
        // Allow content to draw behind system bars (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        windowInsetsController.apply {
            // Keep status bar visible, make it transparent
            activity.window.statusBarColor = Color.TRANSPARENT
            // Handle navigation bar based on isShowNav
            if (isShowNav) {
                show(WindowInsetsCompat.Type.navigationBars())
                activity.window.navigationBarColor = Color.BLACK
            } else {
                hide(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                activity.window.navigationBarColor = Color.TRANSPARENT
            }
        }

        // Handle window insets to adjust content layout
        activity.window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            val compatInsets = WindowInsetsCompat.toWindowInsetsCompat(insets, view)
            val statusBarInsets = compatInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBarInsets =
                compatInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Get the content view (root of activity layout)
            val contentView = activity.findViewById<ViewGroup>(R.id.content).getChildAt(0)
            val layoutParams = contentView.layoutParams as ViewGroup.MarginLayoutParams

            // Set margins to avoid status bar overlap
            layoutParams.setMargins(
                0,
                if (transparentStatusBar) statusBarInsets.top else 0,
                0,
                if (isShowNav) navigationBarInsets.bottom else 0
            )
            contentView.layoutParams = layoutParams
            contentView.fitsSystemWindows = false

            // Convert back to WindowInsets
            compatInsets.toWindowInsets() ?: insets
        }

        // Apply custom background to content view
        val contentView = activity.findViewById<ViewGroup>(R.id.content).getChildAt(0)
        val statusBarDrawable: Drawable? = ResourcesCompat.getDrawable(
            activity.resources,
            R.color.white,
            activity.theme
        )
        contentView.background = statusBarDrawable

    }
}
