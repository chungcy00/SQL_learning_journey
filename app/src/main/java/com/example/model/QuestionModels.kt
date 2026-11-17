package com.example.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class QuestionChapterFile(
    val chapterId: String = "",
    val questions: List<QuestionItem> = emptyList()
)

@Serializable
data class QuestionOption(
    val id: String,
    val text: String,
    val explanation: String = ""
)

@Serializable
data class ReferenceSolution(
    val mysqlSql: String = "",
    val sqliteSql: String = "",
    val explanation: String = ""
)

@Serializable
data class NumericTolerance(
    val absolute: Double = 1e-6,
    val decimalAbsolute: Double = 0.015,
    val integerExact: Boolean = true,
    val textExact: Boolean = true,
    val nullDistinct: Boolean = true
)

@Serializable
data class QuestionItem(
    val id: String,
    val chapterId: String,
    val title: String,
    val type: String,
    val difficulty: String,
    val context: String = "",
    val requirements: String = "",
    val initialCode: String = "",
    val options: List<QuestionOption> = emptyList(),
    val correctOptionId: String? = null,
    val predictionOptions: List<String> = emptyList(),
    val predictionAnswerText: String? = null,
    val blankAnswerHint: String = "",
    val acceptedBlanks: List<String> = emptyList(),
    val hints: List<String> = emptyList(),
    val explanation: String = "",
    val referenceSolutions: List<ReferenceSolution> = emptyList(),
    val requiredAstFeatures: List<String> = emptyList(),
    val supportStatus: String = "直接兼容",
    val datasetId: String? = null,
    val setupSql: String? = null,
    val fixtureIds: List<String> = emptyList(),
    val expectedColumns: List<String> = emptyList(),
    val expectedRows: List<List<JsonElement?>> = emptyList(),
    val orderPolicy: String = "ordered",
    val columnPolicy: String = "exact_names_in_order",
    val numericTolerance: NumericTolerance = NumericTolerance(),
    val taskId: String? = null,
    val crossGroup: String? = null,
    val contentVersion: String = "3.0.0"
)

@Serializable
data class FixtureExpected(
    val columns: List<String> = emptyList(),
    val rows: List<List<JsonElement?>> = emptyList(),
    val correctOptionId: String? = null
)

@Serializable
data class QuestionFixture(
    val id: String = "",
    val questionId: String = "",
    val datasetId: String? = null,
    val visibility: String = "public",
    val variant: String = "public",
    val kind: String = "query",
    val tables: List<SourceTable> = emptyList(),
    val setupSql: String? = null,
    val verificationQuery: String? = null,
    val expected: FixtureExpected? = null,
    val orderPolicy: String = "ordered",
    val columnPolicy: String = "exact_names_in_order",
    val numericTolerance: NumericTolerance = NumericTolerance()
)
