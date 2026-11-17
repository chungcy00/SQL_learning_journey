package com.example.model

import kotlinx.serialization.Serializable

enum class FlashcardRating(val value: Int, val label: String) {
    AGAIN(1, "重来"),
    HARD(2, "困难"),
    GOOD(3, "良好"),
    EASY(4, "简单")
}

enum class FlashcardReviewMode(val id: String, val label: String) {
    DUE("due", "今日待复习"),
    NEW("new", "新卡"),
    WEAK("weak", "困难/薄弱"),
    CHAPTER("chapter", "按章节"),
    CUSTOM("custom", "自定义卡"),
    FAVORITE("favorite", "收藏卡"),
    RANDOM("random", "随机自由浏览")
}

enum class KeyboardMode(val id: String, val label: String) {
    SYNTAX("syntax", "语法"),
    FUNCTION("function", "函数"),
    SYMBOL("symbol", "符号"),
    FIELD("field", "表与字段")
}

data class FsrsCardStateData(
    val cardId: String,
    val dueAtMs: Long,
    val stability: Double,
    val difficulty: Double,
    val lastReviewAtMs: Long?,
    val reps: Int,
    val lapses: Int,
    val state: Int
)

data class FsrsPreviewOption(
    val rating: FlashcardRating,
    val intervalDays: Double,
    val intervalLabel: String
)

data class StatsOverview(
    val studyDays: Int = 0,
    val streakDays: Int = 0,
    val totalStudySeconds: Long = 0L,
    val completedKps: Int = 0,
    val totalKps: Int = 152,
    val completedQuestions: Int = 0,
    val totalQuestions: Int = 300,
    val firstAttemptAccuracy: Double? = null,
    val firstAttemptPassedCount: Int = 0,
    val firstAttemptTotalCount: Int = 0,
    val independentCompletionRate: Double? = null,
    val independentPassedCount: Int = 0,
    val builtinCardsReviewedUnique: Int = 0,
    val totalBuiltinCards: Int = 200,
    val builtinCardsMastered: Int = 0,
    val customCardsCount: Int = 0,
    val customCardsMastered: Int = 0,
    val totalReviewEvents: Int = 0,
    val dailyMinutesSeries: List<Pair<String, Int>> = emptyList()
)

data class KnowledgeMasteryInfo(
    val kpId: String,
    val score: Int?,
    val label: String
)

@Serializable
data class BundleFileEntry(
    val path: String,
    val sha256: String,
    val bytes: Long
)

@Serializable
data class BundleManifest(
    val version: String = "1.0.0",
    val files: List<BundleFileEntry> = emptyList()
)

data class OfflineCheckReport(
    val checkedAtMs: Long,
    val isReady: Boolean,
    val totalFilesChecked: Int,
    val missingOrCorruptFiles: List<String> = emptyList(),
    val sqliteSmokePassed: Boolean = false,
    val sqliteSmokeDetail: String = "",
    val totalAssetBytes: Long = 0L,
    val extractedDbBytes: Long = 0L,
    val personalDbBytes: Long = 0L
)
