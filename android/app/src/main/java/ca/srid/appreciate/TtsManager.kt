package ca.srid.appreciate

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Wraps Android's TextToSpeech engine.
 * Audio routes to STREAM_MUSIC, so it automatically
 * plays through Bluetooth headphones when connected.
 */
class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TtsManager"
    }

    private val tts = TextToSpeech(context, this)
    private var ready = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            ready = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            if (ready) {
                // Slightly slower speech rate for clarity while walking
                tts.setSpeechRate(0.9f)
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.w(TAG, "TTS language not supported, falling back to default")
                tts.setLanguage(Locale.getDefault())
                ready = true
            }
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
        }
    }

    /** Speaks the given text. Does nothing if TTS is not ready. */
    fun speak(text: String) {
        if (!ready) {
            Log.w(TAG, "TTS not ready, skipping: $text")
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "appreciate_reminder")
        Log.d(TAG, "Speaking: $text")
    }

    /** Releases TTS resources. Call in Service.onDestroy(). */
    fun shutdown() {
        tts.stop()
        tts.shutdown()
        ready = false
        Log.d(TAG, "TTS shut down")
    }
}
