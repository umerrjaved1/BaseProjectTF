package com.example.message.recovery.ui.theme

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The window size class is computed once in [com.example.message.recovery.ui.screens.MainActivity]
 * (it needs an Activity) and read from here by any screen that adapts to available space.
 *
 * Screens branch on *width* rather than raw orientation: a landscape phone and a portrait tablet
 * are different layout problems even though `Configuration.ORIENTATION` calls them the same thing.
 * [isShortScreen] covers the other half — landscape phones are vertically cramped regardless of
 * how wide they are, and that is where stacked content overflows.
 *
 * Previews and tests that don't have an Activity fall back to the compact default below.
 */
val LocalWindowSize = compositionLocalOf { CompactWindowSize }

/** Fallback used by previews and by any composable rendered outside the Activity content. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
val CompactWindowSize: WindowSizeClass = WindowSizeClass.calculateFromSize(DpSize(390.dp, 844.dp))

/** True on phones in portrait — the single-column case. */
val WindowSizeClass.isCompactWidth: Boolean
    get() = widthSizeClass == WindowWidthSizeClass.Compact

/** True on tablets and large foldables, where a single stretched column reads badly. */
val WindowSizeClass.isExpandedWidth: Boolean
    get() = widthSizeClass == WindowWidthSizeClass.Expanded

/**
 * True when there is little vertical room — landscape phones, split-screen. Headers, hero images
 * and generous spacing have to shrink here or the primary action falls off the bottom.
 */
val WindowSizeClass.isShortScreen: Boolean
    get() = heightSizeClass == WindowHeightSizeClass.Compact

/**
 * True when content should be laid out side-by-side instead of stacked: either the screen is wide
 * enough to hold two columns, or it is too short to stack them.
 */
val WindowSizeClass.prefersSideBySide: Boolean
    get() = !isCompactWidth || isShortScreen

/** Navigation moves from a bottom bar to a side rail once there is room for it. */
val WindowSizeClass.prefersNavigationRail: Boolean
    get() = !isCompactWidth

@Composable
@ReadOnlyComposable
fun windowSize(): WindowSizeClass = LocalWindowSize.current

/**
 * Horizontal page padding. Wide screens get more breathing room; the content itself is additionally
 * capped by [contentMaxWidth] so lines never stretch to an unreadable length.
 */
val WindowSizeClass.screenHorizontalPadding
    get() = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 20.dp
        WindowWidthSizeClass.Medium -> 32.dp
        else -> 48.dp
    }

/** Maximum width a single column of content should occupy. */
val WindowSizeClass.contentMaxWidth
    get() = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 600.dp
        WindowWidthSizeClass.Medium -> 720.dp
        else -> 900.dp
    }

/** Minimum tile width for adaptive lists (languages, survey apps) rendered as a grid. */
val listItemMinWidth = 320.dp
