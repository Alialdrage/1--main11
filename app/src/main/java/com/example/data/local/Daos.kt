package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecitationAttemptDao {
    @Query("SELECT * FROM recitation_attempts ORDER BY timestamp DESC")
    fun getAllAttempts(): Flow<List<RecitationAttemptEntity>>

    @Query("SELECT * FROM recitation_attempts WHERE surahNumber = :surah AND ayahNumber = :ayah ORDER BY timestamp DESC LIMIT 5")
    fun getAttemptsForAyah(surah: Int, ayah: Int): Flow<List<RecitationAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: RecitationAttemptEntity): Long

    @Query("DELETE FROM recitation_attempts WHERE id = :id")
    suspend fun deleteAttempt(id: Long)

    @Query("SELECT COUNT(*) FROM recitation_attempts")
    suspend fun getCount(): Int

    @Query("SELECT AVG(score) FROM recitation_attempts")
    suspend fun getAverageScore(): Double?
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY updatedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE surahNumber = :surah AND ayahNumber = :ayah LIMIT 1")
    suspend fun getBookmark(surah: Int, ayah: Int): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE surahNumber = :surah AND ayahNumber = :ayah")
    suspend fun deleteBookmark(surah: Int, ayah: Int)
}

@Dao
interface LearnerStatDao {
    @Query("SELECT * FROM learner_stats WHERE id = 1")
    fun getStats(): Flow<LearnerStatEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStats(stat: LearnerStatEntity)
}

@Dao
interface CertificateDao {
    @Query("SELECT * FROM certificates ORDER BY dateIssued DESC")
    fun getAllCertificates(): Flow<List<CertificateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCertificate(certificate: CertificateEntity)
}