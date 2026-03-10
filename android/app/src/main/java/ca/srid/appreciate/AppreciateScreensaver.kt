package ca.srid.appreciate

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Android Screensaver (DreamService) that shows rotating reminder text
 * with randomized styling. Users can set this as their screen saver in
 * Settings → Display → Screen saver to display reminders on the
 * Always On Display (AOD) or when the device is idle/charging.
 */
class AppreciateScreensaver : DreamService() {

    companion object {
        private const val TAG = "AppreciateScreensaver"
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var container: FrameLayout
    private lateinit var settings: SettingsStore
    private var cycleRunnable: Runnable? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "Screensaver attached")

        isInteractive = false
        isFullscreen = true
        isScreenBright = true // full brightness so text is clearly visible

        settings = SettingsStore(this)

        container = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        setContentView(container)

        showNextReminder()
    }

    private fun showNextReminder() {
        container.removeAllViews()

        val text = settings.randomLine
        val style = randomScreensaverStyle()

        Log.d(TAG, "Showing: \"$text\"")

        val textView = TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSize)
            setTextColor(style.textColor)
            typeface = if (style.isBold)
                Typeface.create(style.typeface, Typeface.BOLD)
            else
                style.typeface
            gravity = Gravity.CENTER
            setShadowLayer(16f, 2f, 2f, Color.argb(150, 0, 0, 0))

            if (style.showPill) {
                val bg = GradientDrawable().apply {
                    cornerRadius = 40f
                    setColor(style.pillColor)
                }
                background = bg
                val hPad = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 32f, resources.displayMetrics
                ).toInt()
                val vPad = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics
                ).toInt()
                setPadding(hPad, vPad, hPad, vPad)
            }
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        container.addView(textView, params)

        // Apply position offset after layout
        container.post {
            val maxTextWidth = (container.width * 0.8f).toInt()
            textView.maxWidth = maxTextWidth

            val maxXOffset = container.width * style.xOffsetFraction
            val maxYOffset = container.height * style.yOffsetFraction

            val halfW = textView.width / 2f
            val halfH = textView.height / 2f
            val maxClampX = (container.width / 2f - halfW).coerceAtLeast(0f)
            val maxClampY = (container.height / 2f - halfH).coerceAtLeast(0f)
            textView.translationX = maxXOffset.coerceIn(-maxClampX, maxClampX)
            textView.translationY = maxYOffset.coerceIn(-maxClampY, maxClampY)

            // Fade in
            textView.alpha = 0f
            textView.animate().alpha(1f).setDuration(1000).start()
        }

        // Schedule next cycle: display duration + 2s transition gap
        val cycleMs = ((settings.displayDurationSeconds + 2f) * 1000).toLong()
        cycleRunnable = Runnable {
            // Fade out then show next
            textView.animate()
                .alpha(0f)
                .setDuration(1000)
                .withEndAction { showNextReminder() }
                .start()
        }
        handler.postDelayed(cycleRunnable!!, cycleMs)
    }

    override fun onDetachedFromWindow() {
        cycleRunnable?.let { handler.removeCallbacks(it) }
        super.onDetachedFromWindow()
        Log.d(TAG, "Screensaver detached")
    }

    // SYNC: Color palette must match OverlayViewFactory.vibrantColors
    private val vibrantColors = listOf(
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

    private val fontFamilies = listOf(
        Typeface.DEFAULT,
        Typeface.SERIF,
        Typeface.SANS_SERIF,
        Typeface.MONOSPACE
    )

    private data class ScreensaverStyle(
        val fontSize: Float,
        val textColor: Int,
        val typeface: Typeface,
        val isBold: Boolean,
        val showPill: Boolean,
        val pillColor: Int,
        val xOffsetFraction: Float,
        val yOffsetFraction: Float
    )

    private fun randomScreensaverStyle(): ScreensaverStyle {
        val hsb = vibrantColors.random()
        val textColor = Color.HSVToColor(floatArrayOf(hsb[0] * 360f, hsb[1], hsb[2]))

        val showPill = Math.random() < 0.5
        val pillColor = if (Math.random() < 0.5)
            Color.argb(100, 0, 0, 0)
        else
            Color.argb(40, 255, 255, 255)

        return ScreensaverStyle(
            fontSize = (30f + Math.random().toFloat() * 24f), // slightly smaller for AOD
            textColor = textColor,
            typeface = fontFamilies.random(),
            isBold = Math.random() < 0.5,
            showPill = showPill,
            pillColor = pillColor,
            xOffsetFraction = (-0.15f + Math.random().toFloat() * 0.3f),
            yOffsetFraction = (-0.15f + Math.random().toFloat() * 0.3f)
        )
    }
}
