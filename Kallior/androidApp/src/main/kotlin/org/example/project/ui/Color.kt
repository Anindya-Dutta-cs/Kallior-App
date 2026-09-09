package org.example.project.ui

import androidx.compose.ui.graphics.Color

/**
 * Central palette for the Android app, mirroring the iOS [KalliorColors].
 */
object KalliorColors {
    /** Secondary layer (page background). #000000 */
    val SecondaryBackground = Color(0xFF000000)
    /** Near-black canvas used by the redesigned home dashboard. */
    val CanvasBackground = Color(0xFF050505)
    /** Dark Brown for gradient. #1B0F05 */
    val DarkBrown = Color(0xFF300C00)
    /** Primary layer (scaffold / sheet behind the foreground cards). #161616 */
    val PrimaryLayer = Color(0xFF161616)
    /** Quiet charcoal surface for dashboard rows. */
    val SurfaceCharcoal = Color(0xFF141414)
    /** Slightly lifted dark surface. */
    val SurfaceElevated = Color(0xFF1C1C1C)
    /** Foreground card background. #2C2C2C */
    val ForegroundCard = Color(0xFF2C2C2C)
    /** Radar chart grid lines. #424242 */
    val RadarLine = Color(0xFF424242)
    /** Accent orange. #FFB370 */
    val AccentOrange = Color(0xFFFB5607)
    /** Destructive / delete red (matches the Exercise category red). #B80E0C */
    val DangerRed = Color(0xFFB80E0C)
    /** Muted text. #ACACAC */
    val MutedText = Color(0xFFACACAC)
    /** Inactive navigation / metadata. */
    val InactiveNav = Color(0xFF6E6E6E)
    /** Normal text. #FFFFFF */
    val NormalText = Color(0xFFFFFFFF)
    /** Hairline stroke used sparingly on dark surfaces. */
    val Hairline = Color(0x14FFFFFF)
}
