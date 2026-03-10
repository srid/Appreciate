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
 * with full anti-habituation styling (same as the regular overlay).
 * Users can set this as their screen saver in
 * Settings → Display → Screen saver to display reminders on the
 * Always On Display (AOD) or when the device is idle/charging.
 *
 * Uses a fixed 15-second cycle (independent of the app's interval settings,
 * which are designed for the regular overlay cadence).
 */
class AppreciateScreensaver : DreamService() {

    companion object {
        private const val TAG = "AppreciateScreensaver"
        /** How long each reminder stays on screen before transitioning (seconds). */
        private const val CYCLE_SECONDS = 15f
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
        val style = OverlayStyle.randomStyle()

        Log.d(TAG, "Showing: \"$text\" anim=${style.animationKind}")

        val textView = TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSize)
            setTextColor(style.textColor)
            typeface = if (style.isBold)
                Typeface.create(style.typeface, Typeface.BOLD)
            else
                style.typeface
            this.rotation = style.rotation
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

        // Apply position offset, animate entrance, schedule exit — all after layout
        container.post {
            val maxTextWidth = (container.width * 0.8f).toInt()
            textView.maxWidth = maxTextWidth

            val maxXOffset = container.width * style.xOffsetFraction
            val maxYOffset = container.height * style.yOffsetFraction
            val halfW = textView.width / 2f
            val halfH = textView.height / 2f
            val maxClampX = (container.width / 2f - halfW).coerceAtLeast(0f)
            val maxClampY = (container.height / 2f - halfH).coerceAtLeast(0f)
            val finalTransX = maxXOffset.coerceIn(-maxClampX, maxClampX)
            val finalTransY = maxYOffset.coerceIn(-maxClampY, maxClampY)
            textView.translationX = finalTransX
            textView.translationY = finalTransY

            // --- Entrance animation ---
            textView.alpha = 0f
            when (style.animationKind) {
                OverlayStyle.AnimationKind.SLIDE_TOP -> textView.translationY = finalTransY - 400f
                OverlayStyle.AnimationKind.SLIDE_BOTTOM -> textView.translationY = finalTransY + 400f
                OverlayStyle.AnimationKind.SLIDE_LEFT -> textView.translationX = finalTransX - 600f
                OverlayStyle.AnimationKind.SLIDE_RIGHT -> textView.translationX = finalTransX + 600f
                OverlayStyle.AnimationKind.SCALE_UP -> {
                    textView.scaleX = 0.3f
                    textView.scaleY = 0.3f
                }
                OverlayStyle.AnimationKind.BLUR_FADE, OverlayStyle.AnimationKind.FADE -> { /* just opacity */ }
            }

            val enter = textView.animate().alpha(1f).setDuration(800)
            when (style.animationKind) {
                OverlayStyle.AnimationKind.SLIDE_TOP, OverlayStyle.AnimationKind.SLIDE_BOTTOM -> enter.translationY(finalTransY)
                OverlayStyle.AnimationKind.SLIDE_LEFT, OverlayStyle.AnimationKind.SLIDE_RIGHT -> enter.translationX(finalTransX)
                OverlayStyle.AnimationKind.SCALE_UP -> enter.scaleX(1f).scaleY(1f)
                else -> {}
            }
            enter.start()

            // --- Scale breathing ---
            if (style.scaleBreathing) {
                fun breatheLoop() {
                    textView.animate()
                        .scaleX(style.targetScale)
                        .scaleY(style.targetScale)
                        .setDuration(1500)
                        .withEndAction {
                            textView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(1500)
                                .withEndAction { breatheLoop() }
                                .start()
                        }
                        .start()
                }
                handler.postDelayed({ breatheLoop() }, 800)
            }
        }

        // --- Schedule exit → next cycle ---
        val holdMs = (CYCLE_SECONDS * 1000).toLong()
        val exitDelay = 800 + holdMs // entrance + hold
        cycleRunnable = Runnable {
            val exit = textView.animate()
                .alpha(0f)
                .setDuration(1200)

            if (style.animationKind == OverlayStyle.AnimationKind.SCALE_UP) {
                exit.scaleX(1.5f).scaleY(1.5f)
            }

            exit.withEndAction { showNextReminder() }
            exit.start()
        }
        handler.postDelayed(cycleRunnable!!, exitDelay)
    }

    override fun onDetachedFromWindow() {
        cycleRunnable?.let { handler.removeCallbacks(it) }
        super.onDetachedFromWindow()
        Log.d(TAG, "Screensaver detached")
    }
}
