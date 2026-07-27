package com.professor.baseproject.ui.base

/**
 * Marker for screens that want immersive, hidden system bars and no top inset padding
 * (splash, paywall, onboarding).
 *
 * Replaces the previous approach of comparing `activity.javaClass.simpleName` against a
 * hardcoded list of strings, which was broken in three ways:
 *  - it still listed "AdActivity", a class that no longer exists here;
 *  - renaming an Activity silently changed its behaviour;
 *  - R8 can rename these classes in release, so the comparison could stop matching in
 *    exactly the build nobody tests.
 */
interface FullscreenScreen
