package com.professor.baseproject.ui.base

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() and window-inset padding are handled centrally by
        // StatusBarUtils.applyEdgeToEdge(), which MyApp invokes for every Activity.
        // Doing it here as well installed a second OnApplyWindowInsetsListener on the
        // same content view, so insets were applied twice.

        // Bound to `this` as LifecycleOwner so the callback is removed with the Activity.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    /**
     * Open, not abstract, and defaults to finishing.
     *
     * This callback is always enabled, so a subclass that overrode the old `abstract`
     * version and forgot to call `finish()` made the Back button do nothing at all.
     * Overriders that want the default behaviour on some paths should call
     * `super.handleBackPress()`.
     */
    protected open fun handleBackPress() {
        finish()
    }
}
