package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class SchemaColumnInfo(
    val name: String,
    val type: String = "",
    val nullable: Boolean = true,
    val primaryKey: Boolean = false
)

@Serializable
data class ForeignKeyRef(
    val column: String,
    val targetTable: String,
    val targetColumn: String
)

@Serializable
data class TableSchemaInfo(
    val rowCount: Int = 0,
    val columns: List<SchemaColumnInfo> = emptyList(),
    val foreignKeys: List<ForeignKeyRef> = emptyList()
)

@Serializable
data class DatabaseSchemaInfo(
    val databaseId: String = "",
    val version: String = "v2",
    val tables: Map<String, TableSchemaInfo> = emptyMap(),
    val views: Map<String, List<SchemaColumnInfo>> = emptyMap()
)

data class ExecutionResult(
    val success: Boolean,
    val columns: List<String> = emptyList(),
    val rows: List<List<String?>> = emptyList(),
    val totalRows: Int = 0,
    val elapsedMs: Long = 0L,
    val errorMessage: String? = null,
    val compatibilityNotes: List<String> = emptyList(),
    val isTruncated: Boolean = false
)

data class FixtureCheckDetail(
    val fixtureName: String,
    val visibility: String,
    val passed: Boolean,
    val message: String,
    val actualColumns: List<String> = emptyList(),
    val actualRowsPreview: List<List<String?>> = emptyList(),
    val expectedColumns: List<String> = emptyList(),
    val expectedRowsPreview: List<List<String?>> = emptyList()
)

data class JudgeResult(
    val passed: Boolean,
    val summary: String,
    val chineseFeedback: String,
    val fixtureResults: List<FixtureCheckDetail> = emptyList(),
    val executionTimeMs: Long = 0L,
    val compatibilityStatus: String = "直接兼容"
)

data class QueryTabState(
    val id: String,
    val title: String,
    val datasetId: String,
    val sql: String,
    val lastResult: ExecutionResult? = null
)

@Serializable
data class ImportColumnSpec(
    val name: String,
    val sqliteType: String = "TEXT"
)

data class ImportPreviewState(
    val fileName: String,
    val sourceType: String,
    val columns: List<ImportColumnSpec>,
    val sampleRows: List<List<String?>>,
    val totalRowCount: Int,
    val rawBytes: ByteArray,
    val warnings: List<String> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImportPreviewState) return false
        return fileName == other.fileName && sourceType == other.sourceType &&
            columns == other.columns && totalRowCount == other.totalRowCount
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + sourceType.hashCode()
        result = 31 * result + columns.hashCode()
        result = 31 * result + totalRowCount
        return result
    }
}
