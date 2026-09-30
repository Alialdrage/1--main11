package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.model.Ayah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class PlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isPauseBetweenRepeats: Boolean = false,
    val pauseSecondsRemaining: Int = 0,
    val currentAyah: Ayah? = null,
    val currentRepeatIndex: Int = 1,
    val targetRepeatCount: Int = 3, // 1, 3, 5, 7, 10, -1 (infinite)
    val repeatDelaySeconds: Int = 2, // Pause after verse so beginner repeats aloud
    val playbackSpeed: Float = 1.0f,
    val autoAdvance: Boolean = true,
    val errorMessage: String? = null
)

class QuranAudioPlayer(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "QuranAudioPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pauseCountdownRunnable: Runnable? = null

    var onAyahCompleted: ((Ayah) -> Unit)? = null
    var onAdvanceToNextAyah: (() -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TTS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val arLocale = Locale("ar")
            val result = tts?.setLanguage(arLocale)
            if (result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                isTtsReady = true
            }
        }
    }

    fun setRepeatCount(count: Int) {
        _playbackState.value = _playbackState.value.copy(targetRepeatCount = count)
    }

    fun setRepeatDelaySeconds(seconds: Int) {
        _playbackState.value = _playbackState.value.copy(repeatDelaySeconds = seconds)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    try {
                        val params = player.playbackParams
                        params.speed = speed
                        player.playbackParams = params
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to set playback speed", e)
                    }
                }
            }
        }
    }

    fun setAutoAdvance(enabled: Boolean) {
        _playbackState.value = _playbackState.value.copy(autoAdvance = enabled)
    }

    fun playAyah(ayah: Ayah, repeatFromStart: Boolean = true) {
        stopPauseCountdown()
        releaseMediaPlayer()

        val repeatIndex = if (repeatFromStart) 1 else _playbackState.value.currentRepeatIndex

        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            isBuffering = true,
            isPauseBetweenRepeats = false,
            currentAyah = ayah,
            currentRepeatIndex = repeatIndex,
            errorMessage = null
        )

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(ayah.audioUrl)
                setOnPreparedListener { mp ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val params = mp.playbackParams
                            params.speed = _playbackState.value.playbackSpeed
                            mp.playbackParams = params
                        } catch (e: Exception) {
                            Log.e(TAG, "Error applying speed", e)
                        }
                    }
                    mp.start()
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = true,
                        isBuffering = false
                    )
                }

                setOnCompletionListener {
                    handleAyahPlaybackFinished(ayah)
                }

                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra. Trying TTS fallback.")
                    fallbackToTts(ayah)
                    true
                }
            }
            mediaPlayer = player
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting MediaPlayer. Trying TTS.", e)
            fallbackToTts(ayah)
        }
    }

    private fun handleAyahPlaybackFinished(ayah: Ayah) {
        val currentState = _playbackState.value
        val targetRepeats = currentState.targetRepeatCount
        val currentRepeat = currentState.currentRepeatIndex

        onAyahCompleted?.invoke(ayah)

        val shouldRepeatAgain = if (targetRepeats == -1) {
            true // Infinite repeat
        } else {
            currentRepeat < targetRepeats
        }

        if (shouldRepeatAgain) {
            val delaySeconds = currentState.repeatDelaySeconds
            if (delaySeconds > 0) {
                startPauseBetweenRepeats(ayah, currentRepeat + 1, delaySeconds)
            } else {
                _playbackState.value = currentState.copy(currentRepeatIndex = currentRepeat + 1)
                playAyah(ayah, repeatFromStart = false)
            }
        } else {
            // Reached target repetitions for this Ayah
            _playbackState.value = currentState.copy(
                isPlaying = false,
                isPauseBetweenRepeats = false,
                currentRepeatIndex = 1
            )
            if (currentState.autoAdvance) {
                onAdvanceToNextAyah?.invoke()
            }
        }
    }

    private fun startPauseBetweenRepeats(ayah: Ayah, nextRepeatIndex: Int, totalSeconds: Int) {
        var secondsLeft = totalSeconds
        _playbackState.value = _playbackState.value.copy(
            isPlaying = true,
            isPauseBetweenRepeats = true,
            pauseSecondsRemaining = secondsLeft
        )

        pauseCountdownRunnable = object : Runnable {
            override fun run() {
                secondsLeft--
                if (secondsLeft > 0) {
                    _playbackState.value = _playbackState.value.copy(pauseSecondsRemaining = secondsLeft)
                    mainHandler.postDelayed(this, 1000)
                } else {
                    _playbackState.value = _playbackState.value.copy(
                        isPauseBetweenRepeats = false,
                        currentRepeatIndex = nextRepeatIndex
                    )
                    playAyah(ayah, repeatFromStart = false)
                }
            }
        }
        mainHandler.postDelayed(pauseCountdownRunnable!!, 1000)
    }

    private fun fallbackToTts(ayah: Ayah) {
        if (isTtsReady && tts != null) {
            _playbackState.value = _playbackState.value.copy(
                isPlaying = true,
                isBuffering = false,
                errorMessage = "تشغيل صوتي محلي بديل"
            )
            val params = android.os.Bundle()
            tts?.speak(ayah.textWithTashkeel, TextToSpeech.QUEUE_FLUSH, params, "quran_tts")
            // Schedule finish based on text length
            val estDurationMs = (ayah.textWithTashkeel.length * 110L).coerceAtLeast(2500L)
            mainHandler.postDelayed({
                handleAyahPlaybackFinished(ayah)
            }, estDurationMs)
        } else {
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                isBuffering = false,
                errorMessage = "تعذر تحميل التسجيل الصوتي. يرجى التحقق من الاتصال بالإنترنت."
            )
        }
    }

    fun pause() {
        stopPauseCountdown()
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
        }
        tts?.stop()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPauseBetweenRepeats = false
        )
    }

    fun resume() {
        val current = _playbackState.value.currentAyah ?: return
        mediaPlayer?.let { player ->
            player.start()
            _playbackState.value = _playbackState.value.copy(isPlaying = true)
        } ?: run {
            playAyah(current, repeatFromStart = false)
        }
    }

    fun stop() {
        stopPauseCountdown()
        releaseMediaPlayer()
        tts?.stop()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isBuffering = false,
            isPauseBetweenRepeats = false,
            currentRepeatIndex = 1
        )
    }

    private fun stopPauseCountdown() {
        pauseCountdownRunnable?.let { mainHandler.removeCallbacks(it) }
        pauseCountdownRunnable = null
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        }
        mediaPlayer = null
    }

    fun release() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
