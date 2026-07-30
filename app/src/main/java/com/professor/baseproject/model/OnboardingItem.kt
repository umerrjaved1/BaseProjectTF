package com.professor.baseproject.model

import androidx.annotation.Keep

@Keep
data class OnboardingItem(
    val title: String,
    val description: String,
    val imageRes: Int,
    /**
     * Whether this slide carries a native ad, from `ad_rules.showOb1Native` /
     * `showOb2Native` / `showOb3Native`.
     *
     * Per slide rather than per screen, so slides can be measured independently — which is the
     * whole point of there being three flags instead of one.
     */
    val adEnabled: Boolean = true
)
