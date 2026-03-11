package ca.srid.appreciate

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Wraps Android's TextToSpeech engine with anti-habituation:
 * randomized speech rate, pitch, and voice on every utterance.
 * Audio routes to STREAM_MUSIC, so it automatically
 * plays through Bluetooth headphones when connected.
 */
class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TtsManager"

        // Anti-habituation ranges
        private const val MIN_RATE = 0.8f
        private const val MAX_RATE = 1.15f
        private const val MIN_PITCH = 0.8f
        private const val MAX_PITCH = 1.3f
    }

    private val tts = TextToSpeech(context, this)
    private var ready = false
    private var availableVoices: List<Voice> = emptyList()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            ready = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ready) {
                Log.w(TAG, "TTS language not supported, falling back to default")
                tts.setLanguage(Locale.getDefault())
                ready = true
            }

            // Collect available voices for this locale
            availableVoices = tts.voices
                ?.filter { !it.isNetworkConnectionRequired && it.locale.language == Locale.US.language }
                ?.toList()
                ?: emptyList()

            Log.d(TAG, "TTS initialized: ${availableVoices.size} voices available")
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
        }
    }

    /** Speaks the given text with randomized rate, pitch, and voice. */
    fun speak(text: String) {
        if (!ready) {
            Log.w(TAG, "TTS not ready, skipping: $text")
            return
        }

        // Anti-habituation: randomize voice parameters each time
        val rate = MIN_RATE + (Math.random() * (MAX_RATE - MIN_RATE)).toFloat()
        val pitch = MIN_PITCH + (Math.random() * (MAX_PITCH - MIN_PITCH)).toFloat()
        tts.setSpeechRate(rate)
        tts.setPitch(pitch)

        // Rotate between available voices
        if (availableVoices.isNotEmpty()) {
            val voice = availableVoices.random()
            tts.voice = voice
            Log.d(TAG, "Speaking: \"$text\" [rate=%.2f, pitch=%.2f, voice=${voice.name}]".format(rate, pitch))
        } else {
            Log.d(TAG, "Speaking: \"$text\" [rate=%.2f, pitch=%.2f, default voice]".format(rate, pitch))
        }

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "appreciate_reminder")
    }

    /** Releases TTS resources. Call in Service.onDestroy(). */
    fun shutdown() {
        tts.stop()
        tts.shutdown()
        ready = false
        Log.d(TAG, "TTS shut down")
    }
}
