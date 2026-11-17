package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "kp_progress")
data class KpProgressEntity(
    @PrimaryKey val kpId: String,
    val chapterId: String,
    val visited: Boolean = true,
    val completed: Boolean = false,
    val completedAtMs: Long? = null,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "question_attempts")
data class QuestionAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    val isCorrect: Boolean,
    val submittedInput: String,
    val isIndependent: Boolean,
    val hintsUsed: Int,
    val viewedAnswer: Boolean,
    val elapsedMs: Long,
    val errorMessage: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "wrong_records")
data class WrongRecordEntity(
    @PrimaryKey val questionId: String,
    val errorCount: Int = 1,
    val consecutiveCorrect: Int = 0,
    val mastered: Boolean = false,
    val lastErrorMessage: String = "",
    val lastWrongSql: String = "",
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "flashcard_states")
data class FlashcardStateEntity(
    @PrimaryKey val cardId: String,
    val isCustom: Boolean = false,
    val contentVersion: String = "1.0.0",
    val dueAtMs: Long,
    val stability: Double,
    val difficulty: Double,
    val lastReviewAtMs: Long? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
    val state: Int = 0,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "flashcard_events")
data class FlashcardEventEntity(
    @PrimaryKey val eventId: String,
    val cardId: String,
    val sessionId: String? = null,
    val rating: Int,
    val reviewTimestampMs: Long,
    val countTowardsSchedule: Boolean,
    val prevStateJson: String? = null,
    val newStateJson: String = ""
)

@Serializable
@Entity(tableName = "flashcard_sessions")
data class FlashcardSessionEntity(
    @PrimaryKey val sessionId: String,
    val mode: String,
    val filterId: String = "",
    val cardIdsJson: String,
    val currentPosition: Int = 0,
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    val countTowardsSchedule: Boolean = true
)

@Serializable
@Entity(tableName = "user_flashcards")
data class UserFlashcardEntity(
    @PrimaryKey val id: String,
    val chapterId: String = "ch01",
    val type: String = "概念辨析",
    val difficulty: String = "中等",
    val frontQuestion: String,
    val frontCode: String? = null,
    val backAnswer: String,
    val backExplanation: String = "",
    val backCode: String? = null,
    val note: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val targetType: String,
    val targetId: String,
    val category: String,
    val title: String,
    val subtitle: String = "",
    val payload: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val targetType: String,
    val targetId: String,
    val content: String,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "sql_drafts")
data class SqlDraftEntity(
    @PrimaryKey val scopeKey: String,
    val code: String,
    val cursorPos: Int = 0,
    val datasetId: String = "retail",
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "query_history")
data class QueryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val datasetId: String,
    val sql: String,
    val rowCount: Int,
    val elapsedMs: Long,
    val success: Boolean,
    val errorMessage: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "personal_datasets")
data class PersonalDatasetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourceType: String,
    val tableCount: Int,
    val rowCount: Int,
    val dbFilePath: String,
    val schemaJson: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "study_daily_logs")
data class StudyDailyLogEntity(
    @PrimaryKey val dateStr: String,
    val studySeconds: Long = 0L,
    val updatedAtMs: Long = System.currentTimeMillis()
)
