package com.example.ptmanageremployee

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/** Applies system-bar insets as padding so content sits below the status bar
 *  and above the navigation bar, matching the wireframe's safe-area layout. */
fun View.applySystemBarInsets(top: Boolean = true, bottom: Boolean = true) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(
            top = if (top) bars.top else v.paddingTop,
            bottom = if (bottom) bars.bottom else v.paddingBottom
        )
        insets
    }
}
