package com.example.data.model

enum class DifficultyLevel(
    val id: String,
    val titleArabic: String,
    val subtitleArabic: String,
    val description: String,
    val minPointsToUnlock: Int
) {
    BEGINNER(
        id = "beginner",
        titleArabic = "مستوى المبتدئ",
        subtitleArabic = "قصار السور ومخارج الحروف الأساسية",
        description = "يركز على السور القصيرة (الفاتحة، المعوذات، الإخلاص)، ضبط الحروف، والقلقلة الأساسية مع سرعة تلاوة متأنية.",
        minPointsToUnlock = 0
    ),
    INTERMEDIATE(
        id = "intermediate",
        titleArabic = "مستوى المتوسط",
        subtitleArabic = "أحكام النون والميم والمدود",
        description = "يتضمن سور الجزء الثلاثين المتوسطة (الكوثر، النصر، المسد، قريش، الماعون) وأحكام الإدغام والإخفاء والغنة.",
        minPointsToUnlock = 250
    ),
    ADVANCED(
        id = "advanced",
        titleArabic = "مستوى المتقدم",
        subtitleArabic = "الإتقان الكامل والتسميع الغيبي",
        description = "يشمل السور الأطول نسبياً (العصر، الفيل، التكاثر، القارعة) واختبارات التلاوة المتقدمة بدون أخطاء تجويدية.",
        minPointsToUnlock = 600
    )
}

data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val versesCount: Int,
    val revelationType: String, // مكية / مدنية
    val description: String,
    val difficultyLevel: DifficultyLevel = DifficultyLevel.BEGINNER
)

data class Ayah(
    val surahNumber: Int,
    val ayahNumber: Int,
    val textWithTashkeel: String,
    val textClean: String, // Without harakat for search and speech comparison
    val transliteration: String,
    val translationArabicBrief: String,
    val tafseerBrief: String, // تفسير مبسط ومختصر لمعنى الآية
    val tajweedNotes: List<TajweedNote> = emptyList(),
    val audioUrl: String
)

data class TajweedNote(
    val ruleName: String,
    val part: String,
    val explanation: String,
    val ruleType: RuleType
)

enum class RuleType {
    MADD,       // المد
    GHUNNAH,    // الغنة والإخفاء
    QALQALAH,   // القلقلة
    IDGHAM,     // الإدغام
    IQLAB,      // الإقلاب
    MAKHRAJ     // المخرج والصفة
}

data class Reciter(
    val id: String,
    val nameArabic: String,
    val subName: String,
    val baseUrl: String
)

data class TajweedLesson(
    val id: String,
    val title: String,
    val category: String,
    val difficultyLevel: DifficultyLevel,
    val ruleType: RuleType,
    val explanation: String,
    val letters: String,
    val examples: List<TajweedExample>,
    val beginnerTip: String
)

data class TajweedExample(
    val ayahText: String,
    val highlightedPart: String,
    val surahRef: String,
    val ruleApplied: String
)

data class RecitationEvaluation(
    val score: Int, // 0 - 100
    val targetAyahText: String,
    val recognizedText: String,
    val wordResults: List<WordComparisonResult>,
    val feedbackTitle: String,
    val generalFeedback: String,
    val tajweedTips: List<String>,
    val encouragement: String
)

data class WordComparisonResult(
    val originalWord: String,
    val spokenWord: String,
    val isCorrect: Boolean,
    val statusNote: String = ""
)

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val pointsRequired: Int,
    val isUnlocked: Boolean = false,
    val category: String = "إنجاز تلاوة"
)

data class Certificate(
    val id: String,
    val title: String,
    val recipientName: String,
    val levelTitle: String,
    val dateIssued: String,
    val score: Int,
    val verificationCode: String
)

data class LevelProgress(
    val level: DifficultyLevel,
    val totalLessons: Int,
    val completedLessons: Int,
    val progressPercent: Int,
    val isUnlocked: Boolean
)
