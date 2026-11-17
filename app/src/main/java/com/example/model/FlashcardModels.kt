package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class FlashcardChapterFile(
    val chapterId: String = "",
    val title: String = "",
    val count: Int = 0,
    val cards: List<FlashcardItem> = emptyList()
)

@Serializable
data class FlashcardFront(
    val question: String,
    val code: String? = null,
    val sourceTables: List<SourceTable> = emptyList()
)

@Serializable
data class FlashcardBack(
    val answer: String,
    val explanation: String = "",
    val code: String? = null,
    val sqliteCode: String? = null,
    val notes: String = ""
)

@Serializable
data class FlashcardItem(
    val id: String,
    val chapterId: String,
    val knowledgePointIds: List<String> = emptyList(),
    val primaryKnowledgePointIds: List<String> = emptyList(),
    val relatedQuestionIds: List<String> = emptyList(),
    val type: String = "概念记忆",
    val difficulty: String = "入门",
    val front: FlashcardFront,
    val back: FlashcardBack,
    val tags: List<String> = emptyList(),
    val supportStatus: String = "仅理论",
    val databaseRef: String? = null,
    val fixtureId: String? = null,
    val contentVersion: String = "1.0.0"
)

@Serializable
data class FlashcardFixtureFile(
    val id: String = "",
    val cardId: String = "",
    val tables: List<SourceTable> = emptyList(),
    val sqliteSql: String? = null,
    val mysqlSql: String? = null
)
