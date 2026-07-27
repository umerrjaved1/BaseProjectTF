package com.professor.baseproject.utils

import android.app.Activity
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.professor.baseproject.ui.base.FullscreenScreen

/**
 * Single entry point for edge-to-edge window handling. Called for every Activity from
 * MyApp's ActivityLifecycleCallbacks.
 *
 * What changed and why:
 *
 *  - `window.statusBarColor` / `window.navigationBarColor` are gone. Both are deprecated
 *    and **ignored from API 35 onward** with edge-to-edge enforced, so at targetSdk 37
 *    every colour this file (and six Activities) set was silently discarded. Bars are now
 *    transparent and the *content root's* background shows through, which means a root
 *    using `@color/bg_color` gets the right bar colour in light AND dark automatically,
 *    because bg_color has a values-night variant.
 *
 *  - Icon appearance is no longer hardcoded. `themeBuilder = true` forced
 *    isAppearanceLightStatusBars = true unconditionally, i.e. dark icons — invisible
 *    against a dark background in night mode. `enableEdgeToEdge()` defaults to
 *    SystemBarStyle.auto, which flips icon appearance with the active theme.
 *
 *  - Fullscreen behaviour is driven by the [FullscreenScreen] marker instead of matching
 *    Activity class-name strings.
 *
 *  - Removed as dead code: `setupEdgeToEdge` (private, uncalled), `hideSystemBars`
 *    (no callers; it also cast the content child's LayoutParams to MarginLayoutParams,
 *    a ClassCastException for any root whose parent supplies no margin params),
 *    `applyStatusBarBackground` and `createStatusBarView` (unreachable — their only call
 *    site was commented out), and the `statusBarDrawable` / `statusBarColor` parameters,
 *    which were accepted and then ignored.
 */
object StatusBarUtils {

    private val INSET_TYPES = WindowInsetsCompat.Type.systemBars() or
        WindowInsetsCompat.Type.displayCutout() or
        WindowInsetsCompat.Type.ime()

    @JvmStatic
    fun applyEdgeToEdge(activity: Activity) {
        val window = activity.window ?: return
        val contentView = activity.findViewById<View>(android.R.id.content) ?: return

        // enableEdgeToEdge() also handles decorFitsSystemWindows and, crucially, sets
        // SystemBarStyle.auto so bar icons follow the light/dark theme.
        if (activity is ComponentActivity) {
            activity.enableEdgeToEdge()
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        val immersive = activity is FullscreenScreen
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (immersive) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }

        ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, windowInsets ->
            val bars = windowInsets.getInsets(INSET_TYPES)
            view.updatePadding(
                left = bars.left,
                // Immersive screens draw under the status bar deliberately.
                top = if (immersive) 0 else bars.top,
                right = bars.right,
                bottom = bars.bottom
            )
            // Returned un-consumed so nested listeners still see the insets.
            windowInsets
        }
    }
}
