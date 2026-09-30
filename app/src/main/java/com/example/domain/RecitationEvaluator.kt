package com.example.domain

import com.example.data.model.Ayah
import com.example.data.model.QuranDataProvider
import com.example.data.model.RecitationEvaluation
import com.example.data.model.WordComparisonResult
import com.example.data.remote.GeminiRecitationService
import kotlin.math.max

object RecitationEvaluator {

    suspend fun evaluate(ayah: Ayah, spokenText: String): RecitationEvaluation {
        val targetClean = QuranDataProvider.cleanArabicText(ayah.textClean)
        val spokenClean = QuranDataProvider.cleanArabicText(spokenText)

        val targetWords = targetClean.split(" ").filter { it.isNotBlank() }
        val spokenWords = spokenClean.split(" ").filter { it.isNotBlank() }

        val wordResults = mutableListOf<WordComparisonResult>()
        val missingOrWrong = mutableListOf<String>()
        var matchCount = 0

        // Alignment and similarity matching
        for (i in targetWords.indices) {
            val originalWord = targetWords[i]
            // Look for best match in spoken words within a sliding window
            val windowStart = max(0, i - 2)
            val windowEnd = kotlin.math.min(spokenWords.size, i + 3)
            var bestSimilarity = 0.0
            var matchedSpokenWord = ""

            for (j in windowStart until windowEnd) {
                val candidate = spokenWords[j]
                val sim = calculateSimilarity(originalWord, candidate)
                if (sim > bestSimilarity) {
                    bestSimilarity = sim
                    matchedSpokenWord = candidate
                }
            }

            if (bestSimilarity >= 0.75) {
                matchCount++
                wordResults.add(
                    WordComparisonResult(
                        originalWord = originalWord,
                        spokenWord = matchedSpokenWord,
                        isCorrect = true,
                        statusNote = "نطق سليم ومطابق"
                    )
                )
            } else {
                missingOrWrong.add(originalWord)
                wordResults.add(
                    WordComparisonResult(
                        originalWord = originalWord,
                        spokenWord = matchedSpokenWord.ifBlank { "لم تُنطق" },
                        isCorrect = false,
                        statusNote = if (matchedSpokenWord.isNotBlank()) "تأكد من مخارج الحروف" else "كلمة ناقصة"
                    )
                )
            }
        }

        val accuracyScore = if (targetWords.isNotEmpty()) {
            ((matchCount.toDouble() / targetWords.size) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val (feedbackTitle, encouragement) = when {
            accuracyScore >= 90 -> "ما شاء الله! تلاوة ممتازة ومتقنة" to "أداء رائع جداً! حروفك واضحة ومخارجك منضبطة. استمر في التكرار لتثبيت التلاوة."
            accuracyScore >= 70 -> "أحسنت! قراءة جيدة جداً" to "تلاوة طيبة ومبشرة! ركّز أكثر على الكلمات المحددة باللون البرتقالي وحاول الاستماع للتكرار مرة أخرى."
            accuracyScore >= 50 -> "بداية موفقة ومحاولة طيبة" to "أنت في الطريق الصحيح! استمع للشيخ المرتل عدة مرات مع خاصية التكرار ثم أعد القراءة بهدوء وتأنٍّ."
            else -> "واصل المحاولة والتدريب" to "لا تقلق، فالتلاوة تحتاج إلى تكرار وتمرين. اضغط على زر الاستماع واستمع لتلاوة الآية 3 مرات متتالية ثم جرّب مرة أخرى."
        }

        // Generate specific Tajweed tips for this Ayah
        val tajweedTips = mutableListOf<String>()
        ayah.tajweedNotes.forEach { note ->
            tajweedTips.add("${note.ruleName} في «${note.part}»: ${note.explanation}")
        }
        if (tajweedTips.isEmpty()) {
            tajweedTips.add("احرص على إعطاء كل حرف حقه ومستحقه من المد والغنة.")
        }

        // Try getting AI feedback via Gemini
        val aiFeedback = GeminiRecitationService.getRecitationTeacherFeedback(
            ayahWithTashkeel = ayah.textWithTashkeel,
            spokenText = spokenText,
            accuracyScore = accuracyScore,
            missingOrWrongWords = missingOrWrong
        )

        val finalGeneralFeedback = aiFeedback ?: encouragement

        return RecitationEvaluation(
            score = accuracyScore,
            targetAyahText = ayah.textWithTashkeel,
            recognizedText = spokenText,
            wordResults = wordResults,
            feedbackTitle = feedbackTitle,
            generalFeedback = finalGeneralFeedback,
            tajweedTips = tajweedTips,
            encouragement = encouragement
        )
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val longer = if (s1.length >= s2.length) s1 else s2
        val shorter = if (s1.length < s2.length) s1 else s2
        if (longer.isEmpty()) return 1.0

        val editDistance = levenshteinDistance(s1, s2)
        return (longer.length - editDistance).toDouble() / longer.length.toDouble()
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        var cost = Array(lhs.length + 1) { it }
        var newCost = Array(lhs.length + 1) { 0 }

        for (i in 1..rhs.length) {
            newCost[0] = i
            for (j in 1..lhs.length) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhs.length]
    }
}
