package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.PlaybackState
import com.example.audio.QuranAudioPlayer
import com.example.audio.SpeechRecognitionHelper
import com.example.audio.SpeechState
import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.CertificateEntity
import com.example.data.local.LearnerStatEntity
import com.example.data.local.RecitationAttemptEntity
import com.example.data.model.*
import com.example.data.repository.QuranRepository
import com.example.domain.RecitationEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuranUiState(
    val selectedSurah: Surah = QuranDataProvider.surahs.first(),
    val ayahs: List<Ayah> = emptyList(),
    val selectedAyahForCorrection: Ayah? = null,
    val selectedAyahForTafseer: Ayah? = null,
    val selectedReciter: Reciter = QuranDataProvider.reciters.first(),
    val currentDifficultyLevel: DifficultyLevel = DifficultyLevel.BEGINNER,
    val fontSizeSp: Int = 24,
    val showTajweedColors: Boolean = true,
    val showTranslation: Boolean = true,
    val isEvaluating: Boolean = false,
    val lastEvaluation: RecitationEvaluation? = null,
    val selectedTajweedLesson: TajweedLesson? = null,
    val newlyUnlockedBadge: Badge? = null,
    val activeCertificate: CertificateEntity? = null
)

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuranRepository(AppDatabase.getDatabase(application))
    private val audioPlayer = QuranAudioPlayer(application)
    private val speechHelper = SpeechRecognitionHelper(application)

    private val _uiState = MutableStateFlow(QuranUiState())
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = audioPlayer.playbackState
    val speechState: StateFlow<SpeechState> = speechHelper.speechState

    val attemptsHistory: StateFlow<List<RecitationAttemptEntity>> = repository.getAllAttempts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.getBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val learnerStats: StateFlow<LearnerStatEntity?> = repository.getLearnerStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val certificates: StateFlow<List<CertificateEntity>> = repository.getAllCertificates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Badges mapped to unlocked state
    val badges: StateFlow<List<Badge>> = learnerStats.combine(_uiState) { stats, _ ->
        val unlockedIds = stats?.unlockedBadgesIds?.split(",")?.map { it.trim() }?.toSet() ?: setOf("first_ayah")
        QuranDataProvider.defaultBadges.map { badge ->
            badge.copy(isUnlocked = unlockedIds.contains(badge.id) || (stats?.totalPoints ?: 0) >= badge.pointsRequired)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuranDataProvider.defaultBadges)

    // Difficulty level progression calculations
    val levelProgressList: StateFlow<List<LevelProgress>> = learnerStats.combine(_uiState) { stats, _ ->
        val completedKeys = stats?.completedLessonsIds?.split(",")?.map { it.trim() }?.toSet() ?: emptySet()
        val totalPoints = stats?.totalPoints ?: 0

        DifficultyLevel.values().map { level ->
            val levelSurahs = repository.getSurahsByLevel(level)
            val levelLessons = repository.getTajweedLessonsByLevel(level)
            val totalItems = levelSurahs.size + levelLessons.size

            var completedCount = 0
            levelSurahs.forEach { s ->
                if (completedKeys.contains("surah_${s.number}") || completedKeys.contains("${s.number}")) {
                    completedCount++
                }
            }
            levelLessons.forEach { l ->
                if (completedKeys.contains(l.id)) {
                    completedCount++
                }
            }

            // Points also count toward progress
            val pointsProgress = ((totalPoints.toDouble() / level.minPointsToUnlock.coerceAtLeast(100)) * 100).toInt().coerceIn(0, 100)
            val lessonProgress = if (totalItems > 0) ((completedCount.toDouble() / totalItems) * 100).toInt() else 0
            val combinedPercent = if (level == DifficultyLevel.BEGINNER) {
                ((completedCount.toDouble() / totalItems.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
            } else {
                ((lessonProgress * 0.6) + (pointsProgress * 0.4)).toInt().coerceIn(0, 100)
            }

            val isUnlocked = totalPoints >= level.minPointsToUnlock

            LevelProgress(
                level = level,
                totalLessons = totalItems,
                completedLessons = completedCount,
                progressPercent = combinedPercent,
                isUnlocked = isUnlocked
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSurah(QuranDataProvider.surahs.first())

        audioPlayer.onAyahCompleted = { _ ->
            viewModelScope.launch {
                repository.recordRepetitionCompleted()
            }
        }

        audioPlayer.onAdvanceToNextAyah = {
            advanceToNextAyah()
        }

        // Listen for speech recognition completion
        viewModelScope.launch {
            speechHelper.speechState.collect { state ->
                if (state is SpeechState.Success) {
                    evaluateRecognizedText(state.recognizedText)
                }
            }
        }
    }

    fun setDifficultyLevel(level: DifficultyLevel) {
        _uiState.value = _uiState.value.copy(currentDifficultyLevel = level)
        // Select first surah of that level
        val firstSurahOfLevel = repository.getSurahsByLevel(level).firstOrNull() ?: QuranDataProvider.surahs.first()
        loadSurah(firstSurahOfLevel)
    }

    fun loadSurah(surah: Surah) {
        val ayahs = repository.getAyahs(surah.number, _uiState.value.selectedReciter.id)
        _uiState.value = _uiState.value.copy(
            selectedSurah = surah,
            ayahs = ayahs,
            selectedAyahForCorrection = ayahs.firstOrNull()
        )
    }

    fun selectReciter(reciter: Reciter) {
        _uiState.value = _uiState.value.copy(selectedReciter = reciter)
        val currentSurah = _uiState.value.selectedSurah
        val ayahs = repository.getAyahs(currentSurah.number, reciter.id)
        _uiState.value = _uiState.value.copy(ayahs = ayahs)
    }

    fun playAyah(ayah: Ayah) {
        audioPlayer.playAyah(ayah, repeatFromStart = true)
    }

    // Explicit individual Ayah repeat for Short Surahs Library
    fun repeatAyahIndividually(ayah: Ayah, repeatCount: Int = 3, delaySec: Int = 2) {
        audioPlayer.setAutoAdvance(false) // Keep repeating this specific ayah only!
        audioPlayer.setRepeatCount(repeatCount)
        audioPlayer.setRepeatDelaySeconds(delaySec)
        audioPlayer.playAyah(ayah, repeatFromStart = true)
    }

    fun pauseAudio() {
        audioPlayer.pause()
    }

    fun resumeAudio() {
        audioPlayer.resume()
    }

    fun stopAudio() {
        audioPlayer.stop()
    }

    fun setRepeatCount(count: Int) {
        audioPlayer.setRepeatCount(count)
    }

    fun setRepeatDelaySeconds(seconds: Int) {
        audioPlayer.setRepeatDelaySeconds(seconds)
    }

    fun setPlaybackSpeed(speed: Float) {
        audioPlayer.setPlaybackSpeed(speed)
    }

    fun setAutoAdvance(enabled: Boolean) {
        audioPlayer.setAutoAdvance(enabled)
    }

    fun advanceToNextAyah() {
        val currentAyah = playbackState.value.currentAyah ?: return
        val currentList = _uiState.value.ayahs
        val currentIndex = currentList.indexOfFirst { it.ayahNumber == currentAyah.ayahNumber }
        if (currentIndex != -1 && currentIndex < currentList.size - 1) {
            val nextAyah = currentList[currentIndex + 1]
            playAyah(nextAyah)
        } else {
            audioPlayer.stop()
        }
    }

    fun playPreviousAyah() {
        val currentAyah = playbackState.value.currentAyah ?: return
        val currentList = _uiState.value.ayahs
        val currentIndex = currentList.indexOfFirst { it.ayahNumber == currentAyah.ayahNumber }
        if (currentIndex > 0) {
            val prevAyah = currentList[currentIndex - 1]
            playAyah(prevAyah)
        }
    }

    fun selectAyahForCorrection(ayah: Ayah) {
        _uiState.value = _uiState.value.copy(
            selectedAyahForCorrection = ayah,
            lastEvaluation = null
        )
    }

    fun showAyahTafseer(ayah: Ayah?) {
        _uiState.value = _uiState.value.copy(selectedAyahForTafseer = ayah)
    }

    fun startRecording() {
        speechHelper.startListening()
    }

    fun stopRecording() {
        speechHelper.stopListening()
    }

    fun cancelRecording() {
        speechHelper.cancel()
    }

    fun evaluateRecognizedText(spokenText: String) {
        val ayah = _uiState.value.selectedAyahForCorrection ?: return
        _uiState.value = _uiState.value.copy(isEvaluating = true)

        viewModelScope.launch {
            val evaluation = RecitationEvaluator.evaluate(ayah, spokenText)
            _uiState.value = _uiState.value.copy(
                isEvaluating = false,
                lastEvaluation = evaluation
            )
            repository.saveRecitationAttempt(ayah.surahNumber, ayah.ayahNumber, evaluation)
            if (evaluation.score >= 80) {
                repository.markSurahMastered(ayah.surahNumber)
            }
        }
    }

    fun simulateRecitation(sampleText: String) {
        speechHelper.simulateRecitationInput(sampleText)
    }

    fun toggleBookmark(ayah: Ayah) {
        viewModelScope.launch {
            repository.toggleBookmark(_uiState.value.selectedSurah, ayah)
        }
    }

    fun toggleTajweedColors() {
        _uiState.value = _uiState.value.copy(showTajweedColors = !_uiState.value.showTajweedColors)
    }

    fun toggleTranslation() {
        _uiState.value = _uiState.value.copy(showTranslation = !_uiState.value.showTranslation)
    }

    fun setFontSize(sizeSp: Int) {
        _uiState.value = _uiState.value.copy(fontSizeSp = sizeSp.coerceIn(18, 36))
    }

    fun selectTajweedLesson(lesson: TajweedLesson?) {
        _uiState.value = _uiState.value.copy(selectedTajweedLesson = lesson)
        if (lesson != null) {
            viewModelScope.launch {
                repository.markLessonCompleted(lesson.id)
            }
        }
    }

    fun generateCertificate(recipientName: String, level: DifficultyLevel) {
        viewModelScope.launch {
            val avgScore = attemptsHistory.value.map { it.score }.average().toInt().coerceIn(70, 98)
            val cert = repository.generateCertificate(recipientName, level, avgScore)
            _uiState.value = _uiState.value.copy(activeCertificate = cert)
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            repository.updateUserName(name)
        }
    }

    fun dismissCertificate() {
        _uiState.value = _uiState.value.copy(activeCertificate = null)
    }

    fun deleteAttempt(id: Long) {
        viewModelScope.launch {
            repository.deleteAttempt(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        speechHelper.destroy()
    }
}
