package ca.srid.appreciate

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
import android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Wraps Android's TextToSpeech engine with anti-habituation:
 * randomized speech rate, pitch, and voice on every utterance.
 * Audio routes to STREAM_MUSIC, so it automatically
 * plays through Bluetooth headphones when connected.
 * 
 * Uses AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK to duck music while speaking.
 */
class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TtsManager"
        private const val UTTERANCE_ID = "appreciate_reminder"

        // Anti-habituation ranges
        private const val MIN_RATE = 0.8f
        private const val MAX_RATE = 1.15f
        private const val MIN_PITCH = 0.8f
        private const val MAX_PITCH = 1.3f
    }

    private val tts = TextToSpeech(context, this)
    private var ready = false
    private var availableVoices: List<Voice> = emptyList()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

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

            availableVoices = tts.voices
                ?.filter { !it.isNetworkConnectionRequired && it.locale.language == Locale.US.language }
                ?.toList()
                ?: emptyList()

            audioFocusRequest = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { /* no-op — we don't duck ourselves */ }
                .build()

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "Speech started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "Speech done: $utteranceId")
                    abandonAudioFocus()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.w(TAG, "Speech error: $utteranceId")
                    abandonAudioFocus()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.w(TAG, "Speech error $errorCode: $utteranceId")
                    abandonAudioFocus()
                }
            })

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

        val rate = MIN_RATE + (Math.random() * (MAX_RATE - MIN_RATE)).toFloat()
        val pitch = MIN_PITCH + (Math.random() * (MAX_PITCH - MIN_PITCH)).toFloat()
        tts.setSpeechRate(rate)
        tts.setPitch(pitch)

        if (availableVoices.isNotEmpty()) {
            val voice = availableVoices.random()
            tts.voice = voice
            Log.d(TAG, "Speaking: \"$text\" [rate=%.2f, pitch=%.2f, voice=${voice.name}]".format(rate, pitch))
        } else {
            Log.d(TAG, "Speaking: \"$text\" [rate=%.2f, pitch=%.2f, default voice]".format(rate, pitch))
        }

        requestAudioFocus()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private fun requestAudioFocus() {
        audioFocusRequest?.let { req ->
            val result = audioManager.requestAudioFocus(req)
            if (result == AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d(TAG, "Audio focus granted (ducking enabled)")
            } else {
                Log.w(TAG, "Audio focus request failed: $result")
            }
        }
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { req ->
            audioManager.abandonAudioFocusRequest(req)
            Log.d(TAG, "Audio focus abandoned (ducking disabled)")
        }
    }

    /** Releases TTS resources. Call in Service.onDestroy(). */
    fun shutdown() {
        tts.stop()
        tts.shutdown()
        abandonAudioFocus()
        ready = false
        Log.d(TAG, "TTS shut down")
    }
}
