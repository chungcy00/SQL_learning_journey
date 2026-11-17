package com.example.engine

import com.example.data.local.AppSettings
import com.example.data.local.FavoriteEntity
import com.example.data.local.FlashcardEventEntity
import com.example.data.local.FlashcardStateEntity
import com.example.data.local.KpProgressEntity
import com.example.data.local.NoteEntity
import com.example.data.local.QueryHistoryEntity
import com.example.data.local.QuestionAttemptEntity
import com.example.data.local.SqlDraftEntity
import com.example.data.local.StudyDailyLogEntity
import com.example.data.local.UserFlashcardEntity
import com.example.data.local.WrongRecordEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupPayload(
    val formatVersion: Int = 1,
    val exportedAtMs: Long = System.currentTimeMillis(),
    val kpProgress: List<KpProgressEntity> = emptyList(),
    val questionAttempts: List<QuestionAttemptEntity> = emptyList(),
    val wrongRecords: List<WrongRecordEntity> = emptyList(),
    val flashcardStates: List<FlashcardStateEntity> = emptyList(),
    val flashcardEvents: List<FlashcardEventEntity> = emptyList(),
    val userFlashcards: List<UserFlashcardEntity> = emptyList(),
    val favorites: List<FavoriteEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val drafts: List<SqlDraftEntity> = emptyList(),
    val queryHistory: List<QueryHistoryEntity> = emptyList(),
    val studyLogs: List<StudyDailyLogEntity> = emptyList(),
    val settings: AppSettings? = null
)

data class BackupInspectSummary(
    val recordCount: Int,
    val exportedAtMs: Long,
    val payload: BackupPayload
)

object BackupManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun serialize(payload: BackupPayload): String = json.encodeToString(payload)

    fun parseAndValidate(rawJson: String): Result<BackupInspectSummary> = runCatching {
        val payload = json.decodeFromString<BackupPayload>(rawJson)
        require(payload.formatVersion >= 1) { "不支持的备份文件版本" }
        val total = payload.kpProgress.size +
            payload.questionAttempts.size +
            payload.wrongRecords.size +
            payload.flashcardStates.size +
            payload.flashcardEvents.size +
            payload.userFlashcards.size +
            payload.favorites.size +
            payload.notes.size +
            payload.drafts.size +
            payload.queryHistory.size +
            payload.studyLogs.size
        BackupInspectSummary(
            recordCount = total,
            exportedAtMs = payload.exportedAtMs,
            payload = payload
        )
    }
}
