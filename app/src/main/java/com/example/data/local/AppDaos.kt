package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KpProgressDao {
    @Query("SELECT * FROM kp_progress")
    fun observeAll(): Flow<List<KpProgressEntity>>

    @Query("SELECT * FROM kp_progress")
    suspend fun getAll(): List<KpProgressEntity>

    @Query("SELECT * FROM kp_progress WHERE kpId = :kpId LIMIT 1")
    suspend fun getById(kpId: String): KpProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: KpProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<KpProgressEntity>)

    @Query("DELETE FROM kp_progress")
    suspend fun clearAll()
}

@Dao
interface QuestionAttemptDao {
    @Query("SELECT * FROM question_attempts ORDER BY createdAtMs ASC")
    fun observeAll(): Flow<List<QuestionAttemptEntity>>

    @Query("SELECT * FROM question_attempts ORDER BY createdAtMs ASC")
    suspend fun getAll(): List<QuestionAttemptEntity>

    @Insert
    suspend fun insert(entity: QuestionAttemptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<QuestionAttemptEntity>)

    @Query("DELETE FROM question_attempts")
    suspend fun clearAll()
}

@Dao
interface WrongRecordDao {
    @Query("SELECT * FROM wrong_records ORDER BY updatedAtMs DESC")
    fun observeAll(): Flow<List<WrongRecordEntity>>

    @Query("SELECT * FROM wrong_records")
    suspend fun getAll(): List<WrongRecordEntity>

    @Query("SELECT * FROM wrong_records WHERE questionId = :qId LIMIT 1")
    suspend fun getByQuestionId(qId: String): WrongRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WrongRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<WrongRecordEntity>)

    @Query("DELETE FROM wrong_records")
    suspend fun clearAll()
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcard_states")
    fun observeStates(): Flow<List<FlashcardStateEntity>>

    @Query("SELECT * FROM flashcard_states")
    suspend fun getAllStates(): List<FlashcardStateEntity>

    @Query("SELECT * FROM flashcard_states WHERE cardId = :cardId LIMIT 1")
    suspend fun getState(cardId: String): FlashcardStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(entity: FlashcardStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllStates(entities: List<FlashcardStateEntity>)

    @Query("DELETE FROM flashcard_states WHERE cardId = :cardId")
    suspend fun deleteState(cardId: String)

    @Query("DELETE FROM flashcard_states")
    suspend fun clearAllStates()

    @Query("SELECT * FROM flashcard_events ORDER BY reviewTimestampMs DESC")
    fun observeEvents(): Flow<List<FlashcardEventEntity>>

    @Query("SELECT * FROM flashcard_events ORDER BY reviewTimestampMs DESC")
    suspend fun getAllEvents(): List<FlashcardEventEntity>

    @Query("SELECT * FROM flashcard_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getEvent(eventId: String): FlashcardEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(entity: FlashcardEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEvents(entities: List<FlashcardEventEntity>)

    @Query("DELETE FROM flashcard_events WHERE eventId = :eventId")
    suspend fun deleteEvent(eventId: String)

    @Query("DELETE FROM flashcard_events")
    suspend fun clearAllEvents()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(entity: FlashcardSessionEntity)

    @Query("DELETE FROM flashcard_sessions")
    suspend fun clearAllSessions()

    @Query("SELECT * FROM user_flashcards ORDER BY updatedAtMs DESC")
    fun observeUserFlashcards(): Flow<List<UserFlashcardEntity>>

    @Query("SELECT * FROM user_flashcards ORDER BY updatedAtMs DESC")
    suspend fun getAllUserFlashcards(): List<UserFlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserFlashcard(entity: UserFlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllUserFlashcards(entities: List<UserFlashcardEntity>)

    @Query("DELETE FROM user_flashcards WHERE id = :id")
    suspend fun deleteUserFlashcard(id: String)

    @Query("DELETE FROM user_flashcards")
    suspend fun clearAllUserFlashcards()
}

@Dao
interface UserAssetsDao {
    @Query("SELECT * FROM favorites ORDER BY createdAtMs DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavorites(): List<FavoriteEntity>

    @Query("SELECT * FROM favorites WHERE id = :id LIMIT 1")
    suspend fun getFavorite(id: String): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavorite(entity: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllFavorites(entities: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavorite(id: String)

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()

    @Query("SELECT * FROM notes ORDER BY updatedAtMs DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(entity: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllNotes(entities: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query("SELECT * FROM sql_drafts WHERE scopeKey = :scopeKey LIMIT 1")
    suspend fun getDraft(scopeKey: String): SqlDraftEntity?

    @Query("SELECT * FROM sql_drafts")
    suspend fun getAllDrafts(): List<SqlDraftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(entity: SqlDraftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllDrafts(entities: List<SqlDraftEntity>)

    @Query("DELETE FROM sql_drafts")
    suspend fun clearDrafts()

    @Query("SELECT * FROM query_history ORDER BY createdAtMs DESC LIMIT 200")
    fun observeQueryHistory(): Flow<List<QueryHistoryEntity>>

    @Query("SELECT * FROM query_history ORDER BY createdAtMs DESC")
    suspend fun getAllQueryHistory(): List<QueryHistoryEntity>

    @Insert
    suspend fun insertQueryHistory(entity: QueryHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQueryHistory(entities: List<QueryHistoryEntity>)

    @Query("DELETE FROM query_history")
    suspend fun clearQueryHistory()

    @Query("SELECT * FROM personal_datasets ORDER BY createdAtMs DESC")
    fun observePersonalDatasets(): Flow<List<PersonalDatasetEntity>>

    @Query("SELECT * FROM personal_datasets")
    suspend fun getAllPersonalDatasets(): List<PersonalDatasetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPersonalDataset(entity: PersonalDatasetEntity)

    @Query("DELETE FROM personal_datasets WHERE id = :id")
    suspend fun deletePersonalDataset(id: String)

    @Query("SELECT * FROM study_daily_logs ORDER BY dateStr ASC")
    fun observeStudyLogs(): Flow<List<StudyDailyLogEntity>>

    @Query("SELECT * FROM study_daily_logs ORDER BY dateStr ASC")
    suspend fun getAllStudyLogs(): List<StudyDailyLogEntity>

    @Query("SELECT * FROM study_daily_logs WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getStudyLog(dateStr: String): StudyDailyLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudyLog(entity: StudyDailyLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllStudyLogs(entities: List<StudyDailyLogEntity>)

    @Query("DELETE FROM study_daily_logs")
    suspend fun clearStudyLogs()
}
