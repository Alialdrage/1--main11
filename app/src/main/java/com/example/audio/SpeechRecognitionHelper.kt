package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SpeechState {
    object Idle : SpeechState()
    data class Listening(val partialText: String = "", val rmsDb: Float = 0f) : SpeechState()
    object Processing : SpeechState()
    data class Success(val recognizedText: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class SpeechRecognitionHelper(private val context: Context) {

    private val TAG = "SpeechHelper"
    private var speechRecognizer: SpeechRecognizer? = null

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    init {
        createRecognizer()
    }

    private fun createRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _speechState.value = SpeechState.Listening(partialText = "استمع إليك الآن... اقرأ الآية")
                        }

                        override fun onBeginningOfSpeech() {
                            _speechState.value = SpeechState.Listening(partialText = "جاري الاستماع...")
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            val current = _speechState.value
                            if (current is SpeechState.Listening) {
                                _speechState.value = current.copy(rmsDb = rmsdB.coerceAtLeast(0f))
                            }
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _speechState.value = SpeechState.Processing
                        }

                        override fun onError(error: Int) {
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرف على الكلمات بوضوح، يرجى إعادة المحاولة برفع الصوت قليلاً."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "انتهى الوقت دون التقاط صوت، اضغط مجدداً وتلُ الآية."
                                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "خطأ في الاتصال بخدمة التعرف الصوتي، يرجى التأكد من الإنترنت."
                                SpeechRecognizer.ERROR_AUDIO -> "تعذر الوصول للميكروفون بشكل سليم."
                                else -> "حدث خطأ أثناء الاستماع، حاول مرة أخرى."
                            }
                            Log.w(TAG, "SpeechRecognizer error: $error -> $msg")
                            _speechState.value = SpeechState.Error(msg)
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognized = matches?.firstOrNull()?.trim() ?: ""
                            if (recognized.isNotBlank()) {
                                _speechState.value = SpeechState.Success(recognized)
                            } else {
                                _speechState.value = SpeechState.Error("لم يتم التقاط تلاوة، يرجى المحاولة مجدداً.")
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull() ?: ""
                            if (partial.isNotBlank()) {
                                _speechState.value = SpeechState.Listening(partialText = partial)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating SpeechRecognizer", e)
            }
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            createRecognizer()
        }

        if (speechRecognizer == null) {
            _speechState.value = SpeechState.Error("خدمة التعرف الصوتي غير متوفرة في هذا الجهاز. يمكنك تجربة كتابة التلاوة أو اختيار وضع المحاكاة.")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ar", "ar-EG"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
        }

        try {
            _speechState.value = SpeechState.Listening(partialText = "استعد للتلاوة...")
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech listening", e)
            _speechState.value = SpeechState.Error("تعذر بدء التسجيل: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _speechState.value = SpeechState.Processing
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech listening", e)
        }
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
            _speechState.value = SpeechState.Idle
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling speech recognition", e)
        }
    }

    fun simulateRecitationInput(text: String) {
        _speechState.value = SpeechState.Success(text)
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer", e)
        }
        speechRecognizer = null
    }
}
