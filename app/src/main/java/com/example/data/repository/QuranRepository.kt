package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.CertificateEntity
import com.example.data.local.LearnerStatEntity
import com.example.data.local.RecitationAttemptEntity
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class QuranRepository(private val db: AppDatabase) {

    private val attemptDao = db.recitationAttemptDao()
    private val bookmarkDao = db.bookmarkDao()
    private val statDao = db.learnerStatDao()
    private val certificateDao = db.certificateDao()

    fun getSurahs(): List<Surah> = QuranDataProvider.surahs

    fun getSurahsByLevel(level: DifficultyLevel): List<Surah> =
        QuranDataProvider.surahs.filter { it.difficultyLevel == level }

    fun getAyahs(surahNumber: Int, reciterId: String): List<Ayah> =
        QuranDataProvider.getAyahsForSurah(surahNumber, reciterId)

    fun getTajweedLessons(): List<TajweedLesson> = QuranDataProvider.tajweedLessons

    fun getTajweedLessonsByLevel(level: DifficultyLevel): List<TajweedLesson> =
        QuranDataProvider.tajweedLessons.filter { it.difficultyLevel == level }

    // Persistence
    fun getAllAttempts(): Flow<List<RecitationAttemptEntity>> = attemptDao.getAllAttempts()

    fun getBookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    fun getLearnerStats(): Flow<LearnerStatEntity?> = statDao.getStats()

    fun getAllCertificates(): Flow<List<CertificateEntity>> = certificateDao.getAllCertificates()

    suspend fun saveRecitationAttempt(
        surahNumber: Int,
        ayahNumber: Int,
        evaluation: RecitationEvaluation
    ): Long {
        val notesJson = JSONArray(evaluation.tajweedTips).toString()
        val entity = RecitationAttemptEntity(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            score = evaluation.score,
            recognizedText = evaluation.recognizedText,
            targetText = evaluation.targetAyahText,
            feedbackTitle = evaluation.feedbackTitle,
            generalFeedback = evaluation.generalFeedback,
            tajweedNotesJson = notesJson
        )
        val id = attemptDao.insertAttempt(entity)

        // Award points based on recitation accuracy
        val earnedPoints = when {
            evaluation.score >= 90 -> 100
            evaluation.score >= 75 -> 60
            evaluation.score >= 50 -> 30
            else -> 10
        }
        addPointsAndCheckBadges(earnedPoints, surahNumber, evaluation.score)

        updateStatsAfterAttempt(evaluation.score)
        return id
    }

    private suspend fun updateStatsAfterAttempt(newScore: Int) {
        val currentStats = statDao.getStats().firstOrNull() ?: LearnerStatEntity()
        val count = attemptDao.getCount()
        val avg = attemptDao.getAverageScore()?.toInt() ?: newScore

        statDao.saveStats(
            currentStats.copy(
                totalRecitationsChecked = count,
                averageScore = avg,
                lastActiveDate = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordRepetitionCompleted() {
        val currentStats = statDao.getStats().firstOrNull() ?: LearnerStatEntity()
        val newRepCount = currentStats.totalRepetitionsCompleted + 1
        statDao.saveStats(
            currentStats.copy(
                totalRepetitionsCompleted = newRepCount,
                totalPoints = currentStats.totalPoints + 15, // +15 points per completed verse repetition loop
                lastActiveDate = System.currentTimeMillis()
            )
        )
        checkBadgeUnlocks()
    }

    suspend fun markLessonCompleted(lessonId: String) {
        val currentStats = statDao.getStats().firstOrNull() ?: LearnerStatEntity()
        val completedList = currentStats.completedLessonsIds.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (!completedList.contains(lessonId)) {
            completedList.add(lessonId)
            statDao.saveStats(
                currentStats.copy(
                    completedLessonsIds = completedList.joinToString(","),
                    totalPoints = currentStats.totalPoints + 50 // +50 points per lesson
                )
            )
            checkBadgeUnlocks()
        }
    }

    suspend fun markSurahMastered(surahNumber: Int) {
        val currentStats = statDao.getStats().firstOrNull() ?: LearnerStatEntity()
        val completedList = currentStats.completedLessonsIds.split(",").filter { it.isNotBlank() }.toMutableSet()
        val surahKey = "surah_$surahNumber"
        if (!completedList.contains(surahKey)) {
            completedList.add(surahKey)
            statDao.saveStats(
                currentStats.copy(
                    completedLessonsIds = completedList.joinToString(","),
                    totalPoints = currentStats.totalPoints + 75
                )
            )
            checkBadgeUnlocks()
        }
    }

    private suspend fun addPointsAndCheckBadges(points: Int, surahNumber: Int, score: Int) {
        val currentStats = statDao.getStats().firstOrNull() ?: LearnerStatEntity()
        val newPoints = currentStats.totalPoints + points
        val unlockedList = currentStats.unlockedBadgesIds.split(",").filter { it.isNotBlank() }.toMutableSet()

        if (surahNumber == 1 && score >= 85) unlockedList.add("fatihah_master")
        if (surahNumber == 112 && score >= 85) unlockedList.add("ikhlas_master")
        if (score >= 95) unlockedList.add("golden_reciter")
        if (currentStats.totalRepetitionsCompleted >= 15) unlockedList.add("repeat_champion")

        // Level-based badge unlock
        if (newPoints >= 500) unlockedList.add("beginner_grad")
        if (newPoints >= 800) unlockedList.add("intermediate_grad")

        // Automatic unlock of badges whose pointsRequired <= newPoints
        QuranDataProvider.defaultBadges.forEach { badge ->
            if (newPoints >= badge.pointsRequired) {
                unlockedList.add(badge.id)
            }
        }

        // Determine current highest unlocked level
        val newLevelId = when {
            newPoints >= DifficultyLevel.ADVANCED.minPointsToUnlock -> DifficultyLevel.ADVANCED.id
            newPoints >= DifficultyLevel.INTERMEDIATE.minPointsToUnlock -> DifficultyLevel.INTERMEDIATE.id
            else -> DifficultyLevel.BEGINNER.id
        }

        statDao.saveStats(
            currentStats.copy(
                totalPoints = newPoints,
                unlockedBadgesIds = unlockedList.joinToString(","),
                currentLevelId = newLevelId
            )
        )
    }

    private suspend fun checkBadgeUnlocks() {
        val currentStats = statDao.getStats().firstOrNull() ?: return
        val unlockedList = currentStats.unlockedBadgesIds.split(",").filter { it.isNotBlank() }.toMutableSet()
        val points = currentStats.totalPoints

        QuranDataProvider.defaultBadges.forEach { badge ->
            if (points >= badge.pointsRequired) {
                unlockedList.add(badge.id)
            }
        }
        if (currentStats.totalRepetitionsCompleted >= 15) {
            unlockedList.add("repeat_champion")
        }

        val newLevelId = when {
            points >= DifficultyLevel.ADVANCED.minPointsToUnlock -> DifficultyLevel.ADVANCED.id
            points >= DifficultyLevel.INTERMEDIATE.minPointsToUnlock -> DifficultyLevel.INTERMEDIATE.id
            else -> DifficultyLevel.BEGINNER.id
        }

        statDao.saveStats(
            currentStats.copy(
                unlockedBadgesIds = unlockedList.joinToString(","),
                currentLevelId = newLevelId
            )
        )
    }

    suspend fun generateCertificate(
        recipientName: String,
        level: DifficultyLevel,
        score: Int
    ): CertificateEntity {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
        val dateString = dateFormat.format(Date())
        val code = "QRN-" + UUID.randomUUID().toString().take(6).uppercase()

        val cert = CertificateEntity(
            id = UUID.randomUUID().toString(),
            title = "شهادة إتمام وتفوق في ${level.titleArabic}",
            recipientName = recipientName.ifBlank { "طالب القرآن الكريم" },
            levelTitle = level.titleArabic,
            dateIssued = dateString,
            score = score,
            verificationCode = code
        )
        certificateDao.insertCertificate(cert)
        return cert
    }

    suspend fun updateUserName(name: String) {
        val currentStats = statDao.getStats().firstOrNull() ?: LearnerStatEntity()
        statDao.saveStats(currentStats.copy(userName = name))
    }

    suspend fun toggleBookmark(surah: Surah, ayah: Ayah) {
        val existing = bookmarkDao.getBookmark(ayah.surahNumber, ayah.ayahNumber)
        if (existing != null) {
            bookmarkDao.deleteBookmark(ayah.surahNumber, ayah.ayahNumber)
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    surahNumber = ayah.surahNumber,
                    ayahNumber = ayah.ayahNumber,
                    surahName = surah.nameArabic,
                    ayahSnippet = ayah.textWithTashkeel
                )
            )
        }
    }

    suspend fun deleteAttempt(id: Long) {
        attemptDao.deleteAttempt(id)
    }
}
