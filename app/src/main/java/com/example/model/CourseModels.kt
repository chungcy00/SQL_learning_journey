package com.example.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ColumnDef(
    val name: String,
    val type: String = "",
    val mysqlType: String = "",
    val sqliteType: String = ""
)

@Serializable
data class SourceTable(
    val name: String,
    val columns: List<ColumnDef> = emptyList(),
    val rows: List<List<JsonElement?>> = emptyList()
)

@Serializable
data class LessonFixture(
    val id: String = "",
    val table: String = "",
    val columns: List<ColumnDef> = emptyList(),
    val rows: List<List<JsonElement?>> = emptyList(),
    val tables: List<SourceTable> = emptyList()
)

@Serializable
data class CommonErrorInfo(
    val example: String = "",
    val correction: String = ""
)

@Serializable
data class CompatibilityInfo(
    val status: String = "直接兼容",
    val detail: String = ""
)

@Serializable
data class KnowledgePoint(
    val id: String,
    val chapterId: String,
    val title: String,
    val learningObjective: String = "",
    val prerequisites: List<String> = emptyList(),
    val concept: String = "",
    val syntax: String = "",
    val parameters: Map<String, String> = emptyMap(),
    val fixtureIds: List<String> = emptyList(),
    val sourceTables: List<SourceTable> = emptyList(),
    val mysqlSql: String = "",
    val sqliteSql: String = "",
    val setupSql: String? = null,
    val datasetId: String? = null,
    val executionSteps: List<String> = emptyList(),
    val useCase: String = "",
    val comparison: String = "",
    val commonError: CommonErrorInfo? = null,
    val notes: String = "",
    val compatibility: CompatibilityInfo? = null,
    val relatedQuestionIds: List<String> = emptyList(),
    val relatedFlashcardIds: List<String> = emptyList(),
    val relatedKnowledgePointIds: List<String> = emptyList(),
    val primaryQuestionIds: List<String> = emptyList(),
    val primaryFlashcardIds: List<String> = emptyList()
)

@Serializable
data class ChapterDetail(
    val id: String,
    val number: Int,
    val title: String,
    val stage: String,
    val objective: String = "",
    val prerequisites: List<String> = emptyList(),
    val knowledgePoints: List<KnowledgePoint> = emptyList()
)

@Serializable
data class BusinessProjectInfo(
    val projectId: String,
    val title: String,
    val subtitle: String = "",
    val datasetId: String,
    val background: String = "",
    val goals: List<String> = emptyList(),
    val taskQuestionIds: List<String> = emptyList()
)
