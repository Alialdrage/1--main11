package com.example

import com.example.data.model.DifficultyLevel
import com.example.data.model.QuranDataProvider
import com.example.domain.RecitationEvaluator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class QuranLearningUnitTest {

    @Test
    fun testShortSurahsLibraryContainsEssentialSurahs() {
        val surahs = QuranDataProvider.surahs
        assertTrue(surahs.isNotEmpty())

        val fatihah = surahs.find { it.number == 1 }
        assertNotNull(fatihah)
        assertEquals(7, fatihah?.versesCount)
        assertEquals(DifficultyLevel.BEGINNER, fatihah?.difficultyLevel)

        val ikhlas = surahs.find { it.number == 112 }
        assertNotNull(ikhlas)
        assertEquals(4, ikhlas?.versesCount)

        val nas = surahs.find { it.number == 114 }
        assertNotNull(nas)
        assertEquals(6, nas?.versesCount)
    }

    @Test
    fun testPartitionedAyahsAndTafseer() {
        val fatihahAyahs = QuranDataProvider.getAyahsForSurah(1)
        assertEquals(7, fatihahAyahs.size)

        fatihahAyahs.forEach { ayah ->
            assertTrue("Ayah text should not be empty", ayah.textWithTashkeel.isNotBlank())
            assertTrue("Audio URL should be formatted", ayah.audioUrl.contains("everyayah.com"))
            assertTrue("Tafseer should be provided", ayah.tafseerBrief.isNotBlank())
        }
    }

    @Test
    fun testCleanArabicNormalization() {
        val raw = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        val cleaned = QuranDataProvider.cleanArabicText(raw)
        assertEquals("بسم الله الرحمن الرحيم", cleaned)
    }

    @Test
    fun testRecitationEvaluationAccurateMatch() = runBlocking {
        val ayahs = QuranDataProvider.getAyahsForSurah(112)
        val firstAyah = ayahs.first() // قل هو الله أحد

        val result = RecitationEvaluator.evaluate(firstAyah, "قل هو الله احد")
        assertTrue("Accuracy should be high for matching text", result.score >= 90)
        assertTrue(result.wordResults.all { it.isCorrect })
    }

    @Test
    fun testDifficultyLevelsCategorization() {
        val beginnerSurahs = QuranDataProvider.surahs.filter { it.difficultyLevel == DifficultyLevel.BEGINNER }
        val intermediateSurahs = QuranDataProvider.surahs.filter { it.difficultyLevel == DifficultyLevel.INTERMEDIATE }
        val advancedSurahs = QuranDataProvider.surahs.filter { it.difficultyLevel == DifficultyLevel.ADVANCED }

        assertTrue(beginnerSurahs.isNotEmpty())
        assertTrue(intermediateSurahs.isNotEmpty())
        assertTrue(advancedSurahs.isNotEmpty())
    }

    @Test
    fun testBadgesList() {
        val badges = QuranDataProvider.defaultBadges
        assertTrue(badges.any { it.id == "fatihah_master" })
        assertTrue(badges.any { it.id == "repeat_champion" })
        assertTrue(badges.any { it.id == "golden_reciter" })
    }
}
