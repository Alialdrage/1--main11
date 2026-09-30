package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recitation_attempts")
data class RecitationAttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val score: Int,
    val recognizedText: String,
    val targetText: String,
    val feedbackTitle: String,
    val generalFeedback: String,
    val tajweedNotesJson: String,
    val isStarred: Boolean = false
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String,
    val ayahSnippet: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "learner_stats")
data class LearnerStatEntity(
    @PrimaryKey
    val id: Int = 1,
    val totalRepetitionsCompleted: Int = 0,
    val totalRecitationsChecked: Int = 0,
    val averageScore: Int = 0,
    val streakDays: Int = 1,
    val lastActiveDate: Long = System.currentTimeMillis(),
    val totalPoints: Int = 125,
    val currentLevelId: String = "beginner",
    val completedLessonsIds: String = "1,112", // Default completed for beginner
    val unlockedBadgesIds: String = "first_ayah",
    val userName: String = "قارئ رتّل"
)

@Entity(tableName = "certificates")
data class CertificateEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val recipientName: String,
    val levelTitle: String,
    val dateIssued: String,
    val score: Int,
    val verificationCode: String
)
