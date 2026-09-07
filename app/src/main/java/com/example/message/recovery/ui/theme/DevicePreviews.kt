package com.example.message.recovery.ui.theme

import androidx.compose.ui.tooling.preview.Preview

/**
 * The set of configurations every screen has to survive. Small landscape (844x390) and the 2x font
 * scale are the two that actually catch bugs — they are where stacked content clips and where
 * fixed-height rows overlap.
 */
@Preview(name = "1 Small phone", device = "spec:width=360dp,height=640dp,dpi=160")
@Preview(name = "2 Phone", device = "spec:width=390dp,height=844dp,dpi=160")
@Preview(name = "3 Phone landscape", device = "spec:width=844dp,height=390dp,dpi=160")
@Preview(name = "4 Tablet", device = "spec:width=1280dp,height=800dp,dpi=240")
@Preview(name = "5 Large font", device = "spec:width=390dp,height=844dp,dpi=160", fontScale = 2f)
@Preview(name = "6 RTL", device = "spec:width=390dp,height=844dp,dpi=160", locale = "ar")
annotation class DevicePreviews

/** Same set, for screens drawn on a dark/photographic background where the theme is irrelevant. */
@Preview(name = "Phone", device = "spec:width=390dp,height=844dp,dpi=160")
@Preview(name = "Phone landscape", device = "spec:width=844dp,height=390dp,dpi=160")
@Preview(name = "Tablet", device = "spec:width=1280dp,height=800dp,dpi=240")
annotation class OrientationPreviews

