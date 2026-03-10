package ca.srid.appreciate

import android.graphics.Color
import android.graphics.Typeface

/**
 * Shared overlay styling used by both OverlayViewFactory (regular overlays)
 * and AppreciateScreensaver (AOD/daydream). Centralizes the anti-habituation
 * randomization so both code paths behave identically.
 */
object OverlayStyle {

    // SYNC: Color palette must match macos/Sources/OverlayManager.swift vibrantColors
    val vibrantColors = listOf(
        floatArrayOf(0.05f, 0.85f, 0.95f),
        floatArrayOf(0.08f, 0.90f, 1.0f),
        floatArrayOf(0.12f, 0.80f, 1.0f),
        floatArrayOf(0.95f, 0.70f, 1.0f),
        floatArrayOf(0.55f, 0.75f, 0.95f),
        floatArrayOf(0.50f, 0.60f, 1.0f),
        floatArrayOf(0.45f, 0.70f, 0.90f),
        floatArrayOf(0.75f, 0.60f, 0.95f),
        floatArrayOf(0.30f, 0.80f, 0.90f),
        floatArrayOf(0.85f, 0.65f, 1.0f),
        floatArrayOf(0.65f, 0.70f, 1.0f),
        floatArrayOf(0.15f, 0.90f, 1.0f),
        floatArrayOf(0.35f, 0.95f, 1.0f),
        floatArrayOf(0.80f, 0.80f, 1.0f),
        floatArrayOf(0.00f, 0.80f, 1.0f),
        floatArrayOf(0.60f, 0.35f, 1.0f),
        floatArrayOf(0.85f, 0.30f, 1.0f),
        floatArrayOf(0.40f, 0.35f, 0.95f),
    )

    val fontFamilies = listOf(
        Typeface.DEFAULT,
        Typeface.SERIF,
        Typeface.SANS_SERIF,
        Typeface.MONOSPACE
    )

    // SYNC: Animation kinds must match macos/Sources/OverlayManager.swift AnimationKind
    enum class AnimationKind {
        FADE, SLIDE_TOP, SLIDE_BOTTOM, SLIDE_LEFT, SLIDE_RIGHT, SCALE_UP, BLUR_FADE
    }

    data class Style(
        val fontSize: Float,
        val textColor: Int,
        val typeface: Typeface,
        val isBold: Boolean,
        val rotation: Float,
        val animationKind: AnimationKind,
        val showPill: Boolean,
        val pillColor: Int,
        val scaleBreathing: Boolean,
        val targetScale: Float,
        val xOffsetFraction: Float,
        val yOffsetFraction: Float
    )

    fun randomStyle(): Style {
        val hsb = vibrantColors.random()
        val textColor = Color.HSVToColor(floatArrayOf(hsb[0] * 360f, hsb[1], hsb[2]))

        val showPill = Math.random() < 0.5
        val pillColor = if (Math.random() < 0.5)
            Color.argb(100, 0, 0, 0)
        else
            Color.argb(40, 255, 255, 255)

        return Style(
            fontSize = (36f + Math.random().toFloat() * 36f),
            textColor = textColor,
            typeface = fontFamilies.random(),
            isBold = Math.random() < 0.5,
            rotation = (-3f + Math.random().toFloat() * 6f),
            animationKind = AnimationKind.entries.toTypedArray().random(),
            showPill = showPill,
            pillColor = pillColor,
            scaleBreathing = Math.random() < 0.5,
            targetScale = (1.0f + Math.random().toFloat() * 0.08f),
            xOffsetFraction = (-0.15f + Math.random().toFloat() * 0.3f),
            yOffsetFraction = (-0.15f + Math.random().toFloat() * 0.3f)
        )
    }
}
