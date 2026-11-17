package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettings
import com.example.data.local.FavoriteEntity
import com.example.data.local.FlashcardEventEntity
import com.example.data.local.FlashcardSessionEntity
import com.example.data.local.FlashcardStateEntity
import com.example.data.local.KpProgressEntity
import com.example.data.local.NoteEntity
import com.example.data.local.PersonalDatasetEntity
import com.example.data.local.QueryHistoryEntity
import com.example.data.local.QuestionAttemptEntity
import com.example.data.local.SettingsDataStore
import com.example.data.local.SqlDraftEntity
import com.example.data.local.StudyDailyLogEntity
import com.example.data.local.UserFlashcardEntity
import com.example.data.local.WrongRecordEntity
import com.example.engine.BackupInspectSummary
import com.example.engine.BackupManager
import com.example.engine.BackupPayload
import com.example.engine.FsrsEngine
import com.example.model.FlashcardBack
import com.example.model.FlashcardFront
import com.example.model.FlashcardItem
import com.example.model.FlashcardRating
import com.example.model.FlashcardReviewMode
import com.example.model.FsrsCardStateData
import com.example.model.JudgeResult
import com.example.model.KnowledgeMasteryInfo
import com.example.model.KnowledgePoint
import com.example.model.QuestionItem
import com.example.model.StatsOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class LearningRepository(
    private val db: AppDatabase,
    val settingsStore: SettingsDataStore
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val shortDayFmt = SimpleDateFormat("MM/dd", Locale.US)

    private val kpDao = db.kpProgressDao()
    private val attemptDao = db.questionAttemptDao()
    private val wrongDao = db.wrongRecordDao()
    private val fcDao = db.flashcardDao()
    private val assetDao = db.userAssetsDao()

    val settingsFlow: Flow<AppSettings> = settingsStore.settingsFlow
    val kpProgressFlow: Flow<List<KpProgressEntity>> = kpDao.observeAll()
    val completedKpIdsFlow: Flow<Set<String>> = kpProgressFlow.map { list ->
        list.filter { it.completed }.map { it.kpId }.toSet()
    }
    val questionAttemptsFlow: Flow<List<QuestionAttemptEntity>> = attemptDao.observeAll()
    val wrongRecordsFlow: Flow<List<WrongRecordEntity>> = wrongDao.observeAll()
    val flashcardStatesFlow: Flow<List<FlashcardStateEntity>> = fcDao.observeStates()
    val flashcardEventsFlow: Flow<List<FlashcardEventEntity>> = fcDao.observeEvents()
    val userFlashcardsFlow: Flow<List<UserFlashcardEntity>> = fcDao.observeUserFlashcards()
    val favoritesFlow: Flow<List<FavoriteEntity>> = assetDao.observeFavorites()
    val notesFlow: Flow<List<NoteEntity>> = assetDao.observeNotes()
    val queryHistoryFlow: Flow<List<QueryHistoryEntity>> = assetDao.observeQueryHistory()
    val personalDatasetsFlow: Flow<List<PersonalDatasetEntity>> = assetDao.observePersonalDatasets()
    val studyLogsFlow: Flow<List<StudyDailyLogEntity>> = assetDao.observeStudyLogs()

    val statsOverviewFlow: Flow<StatsOverview> = combine(
        completedKpIdsFlow,
        questionAttemptsFlow,
        flashcardStatesFlow,
        flashcardEventsFlow,
        userFlashcardsFlow,
        studyLogsFlow
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val completedKps = arr[0] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val attempts = arr[1] as List<QuestionAttemptEntity>
        @Suppress("UNCHECKED_CAST")
        val fcStates = arr[2] as List<FlashcardStateEntity>
        @Suppress("UNCHECKED_CAST")
        val fcEvents = arr[3] as List<FlashcardEventEntity>
        @Suppress("UNCHECKED_CAST")
        val customCards = arr[4] as List<UserFlashcardEntity>
        @Suppress("UNCHECKED_CAST")
        val studyLogs = arr[5] as List<StudyDailyLogEntity>

        buildStatsOverview(completedKps, attempts, fcStates, fcEvents, customCards, studyLogs)
    }

    private fun buildStatsOverview(
        completedKps: Set<String>,
        attempts: List<QuestionAttemptEntity>,
        fcStates: List<FlashcardStateEntity>,
        fcEvents: List<FlashcardEventEntity>,
        customCards: List<UserFlashcardEntity>,
        studyLogs: List<StudyDailyLogEntity>
    ): StatsOverview {
        val groupedByQuestion = attempts.groupBy { it.questionId }
        val completedQuestionIds = attempts.filter { it.isCorrect }.map { it.questionId }.toSet()

        val firstAttempts = groupedByQuestion.values.mapNotNull { it.firstOrNull() }
        val firstPassCnt = firstAttempts.count { it.isCorrect }
        val firstTotalCnt = firstAttempts.size
        val firstRate = if (firstTotalCnt > 0) firstPassCnt.toDouble() / firstTotalCnt else null

        val independentPassed = completedQuestionIds.count { qId ->
            attempts.any { it.questionId == qId && it.isCorrect && it.isIndependent }
        }
        val indRate = if (completedQuestionIds.isNotEmpty()) {
            independentPassed.toDouble() / completedQuestionIds.size
        } else null

        val builtinStates = fcStates.filter { !it.isCustom && it.reps > 0 }
        val builtinMastered = builtinStates.count { it.stability >= 14.0 && it.state == 2 }
        val customMastered = fcStates.count { it.isCustom && it.stability >= 14.0 && it.state == 2 }

        val activeLogs = studyLogs.filter { it.studySeconds > 0 }
        val totalSec = studyLogs.sumOf { it.studySeconds }
        val streak = computeStreakDays(studyLogs)
        val series = build7DaySeries(studyLogs)

        return StatsOverview(
            studyDays = activeLogs.size,
            streakDays = streak,
            totalStudySeconds = totalSec,
            completedKps = completedKps.size,
            totalKps = 152,
            completedQuestions = completedQuestionIds.size,
            totalQuestions = 300,
            firstAttemptAccuracy = firstRate,
            firstAttemptPassedCount = firstPassCnt,
            firstAttemptTotalCount = firstTotalCnt,
            independentCompletionRate = indRate,
            independentPassedCount = independentPassed,
            builtinCardsReviewedUnique = builtinStates.size,
            totalBuiltinCards = 200,
            builtinCardsMastered = builtinMastered,
            customCardsCount = customCards.size,
            customCardsMastered = customMastered,
            totalReviewEvents = fcEvents.size,
            dailyMinutesSeries = series
        )
    }

    private fun computeStreakDays(logs: List<StudyDailyLogEntity>): Int {
        val activeDates = logs.filter { it.studySeconds > 0 }.map { it.dateStr }.toSet()
        if (activeDates.isEmpty()) return 0
        val cal = Calendar.getInstance()
        val today = dayFmt.format(cal.time)
        if (today !in activeDates) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        var streak = 0
        while (true) {
            val d = dayFmt.format(cal.time)
            if (d in activeDates) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        return streak
    }

    private fun build7DaySeries(logs: List<StudyDailyLogEntity>): List<Pair<String, Int>> {
        val map = logs.associateBy { it.dateStr }
        val result = mutableListOf<Pair<String, Int>>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        repeat(7) {
            val key = dayFmt.format(cal.time)
            val label = shortDayFmt.format(cal.time)
            val mins = ((map[key]?.studySeconds ?: 0L) / 60L).toInt()
            result.add(label to mins)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    suspend fun recordStudySeconds(seconds: Long) {
        if (seconds <= 0) return
        val today = dayFmt.format(Date())
        val existing = assetDao.getStudyLog(today)
        val next = (existing?.studySeconds ?: 0L) + seconds
        assetDao.upsertStudyLog(StudyDailyLogEntity(dateStr = today, studySeconds = next))
    }

    suspend fun toggleKpCompleted(kpId: String, chapterId: String) {
        val existing = kpDao.getById(kpId)
        val now = System.currentTimeMillis()
        val nextDone = !(existing?.completed ?: false)
        kpDao.upsert(
            KpProgressEntity(
                kpId = kpId,
                chapterId = chapterId,
                visited = true,
                completed = nextDone,
                completedAtMs = if (nextDone) now else existing?.completedAtMs,
                updatedAtMs = now
            )
        )
        settingsStore.update { it.copy(lastKpId = kpId) }
    }

    suspend fun markKpVisited(kpId: String) {
        val chId = if (kpId.startsWith("kp") && kpId.length >= 4) "ch${kpId.substring(2, 4)}" else "ch01"
        val existing = kpDao.getById(kpId)
        if (existing == null) {
            kpDao.upsert(KpProgressEntity(kpId = kpId, chapterId = chId, visited = true, completed = false))
        } else if (!existing.visited) {
            kpDao.upsert(existing.copy(visited = true, updatedAtMs = System.currentTimeMillis()))
        }
        settingsStore.update { it.copy(lastKpId = kpId) }
    }

    suspend fun toggleFavorite(
        targetType: String,
        targetId: String,
        title: String,
        subtitle: String = "",
        payload: String = ""
    ) {
        val id = "$targetType:$targetId"
        val existing = assetDao.getFavorite(id)
        if (existing != null) {
            assetDao.deleteFavorite(id)
        } else {
            val cat = when (targetType) {
                "kp" -> "知识点"
                "question" -> "练习题"
                "flashcard" -> "闪卡"
                else -> "SQL 片段"
            }
            assetDao.upsertFavorite(
                FavoriteEntity(
                    id = id,
                    targetType = targetType,
                    targetId = targetId,
                    category = cat,
                    title = title,
                    subtitle = subtitle,
                    payload = payload
                )
            )
        }
    }

    suspend fun saveNote(targetType: String, targetId: String, content: String) {
        val id = "$targetType:$targetId"
        if (content.isBlank()) {
            assetDao.deleteNote(id)
        } else {
            assetDao.upsertNote(
                NoteEntity(
                    id = id,
                    targetType = targetType,
                    targetId = targetId,
                    content = content.trim()
                )
            )
        }
    }

    suspend fun saveDraft(scopeKey: String, code: String, cursorPos: Int, datasetId: String = "retail") {
        assetDao.upsertDraft(SqlDraftEntity(scopeKey = scopeKey, code = code, cursorPos = cursorPos, datasetId = datasetId))
    }

    suspend fun loadDraft(scopeKey: String): SqlDraftEntity? = assetDao.getDraft(scopeKey)

    suspend fun recordQuestionSubmission(
        question: QuestionItem,
        userInput: String,
        result: JudgeResult,
        hintsUsed: Int,
        viewedAnswer: Boolean
    ) {
        val isIndependent = result.passed && hintsUsed == 0 && !viewedAnswer
        attemptDao.insert(
            QuestionAttemptEntity(
                questionId = question.id,
                isCorrect = result.passed,
                submittedInput = userInput,
                isIndependent = isIndependent,
                hintsUsed = hintsUsed,
                viewedAnswer = viewedAnswer,
                elapsedMs = result.executionTimeMs,
                errorMessage = if (result.passed) "" else result.chineseFeedback
            )
        )

        val existingWrong = wrongDao.getByQuestionId(question.id)
        if (!result.passed) {
            wrongDao.upsert(
                WrongRecordEntity(
                    questionId = question.id,
                    errorCount = (existingWrong?.errorCount ?: 0) + 1,
                    consecutiveCorrect = 0,
                    mastered = false,
                    lastErrorMessage = result.chineseFeedback,
                    lastWrongSql = userInput
                )
            )
        } else if (existingWrong != null) {
            val nextConsecutive = if (isIndependent) existingWrong.consecutiveCorrect + 1 else existingWrong.consecutiveCorrect
            wrongDao.upsert(
                existingWrong.copy(
                    consecutiveCorrect = nextConsecutive,
                    mastered = existingWrong.mastered || nextConsecutive >= 2,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun insertQueryHistory(
        datasetId: String,
        sql: String,
        rowCount: Int,
        elapsedMs: Long,
        success: Boolean,
        errorMessage: String?
    ) {
        assetDao.insertQueryHistory(
            QueryHistoryEntity(
                datasetId = datasetId,
                sql = sql,
                rowCount = rowCount,
                elapsedMs = elapsedMs,
                success = success,
                errorMessage = errorMessage
            )
        )
    }

    fun computeKnowledgeMastery(
        kp: KnowledgePoint,
        completedKps: Set<String>,
        attempts: List<QuestionAttemptEntity>,
        states: Map<String, FlashcardStateEntity>,
        wrongMap: Map<String, WrongRecordEntity>
    ): KnowledgeMasteryInfo {
        val qIds = (kp.relatedQuestionIds + kp.primaryQuestionIds).distinct()
        val fcIds = (kp.relatedFlashcardIds + kp.primaryFlashcardIds).distinct()
        val qAttempts = attempts.filter { it.questionId in qIds }
        val hasActivity = (kp.id in completedKps) || qAttempts.isNotEmpty() || fcIds.any { (states[it]?.reps ?: 0) > 0 }
        if (!hasActivity) {
            return KnowledgeMasteryInfo(kp.id, null, "尚未评估")
        }

        var weightSum = 0.0
        var weightedScore = 0.0

        // 1. Course completion weight 0.30
        weightSum += 0.30
        if (kp.id in completedKps) weightedScore += 30.0

        // 2. Question performance weight 0.50 (if linked questions exist)
        if (qIds.isNotEmpty() && qAttempts.isNotEmpty()) {
            weightSum += 0.50
            val passedCount = qIds.count { qId -> qAttempts.any { it.questionId == qId && it.isCorrect } }
            val avg = (passedCount.toDouble() / qIds.size) * 100.0
            weightedScore += avg * 0.50
        }

        // 3. Flashcard stability weight 0.20 (if reviewed)
        val reviewedCards = fcIds.mapNotNull { states[it] }.filter { it.reps > 0 }
        if (reviewedCards.isNotEmpty()) {
            weightSum += 0.20
            val avgCardScore = reviewedCards.map { st ->
                (st.stability / 14.0).coerceIn(0.25, 1.0) * 100.0
            }.average()
            weightedScore += avgCardScore * 0.20
        }

        var finalScore = if (weightSum > 0.0) (weightedScore / weightSum).toInt().coerceIn(0, 100) else 0
        val hasActiveWrong = qIds.any { wrongMap[it]?.mastered == false }
        if (hasActiveWrong && finalScore > 75) finalScore = 75

        val label = when {
            finalScore >= 85 -> "已掌握"
            finalScore >= 65 -> "基本掌握"
            finalScore >= 40 -> "需加强"
            else -> "重点巩固"
        }
        return KnowledgeMasteryInfo(kp.id, finalScore, label)
    }

    suspend fun resetProgress(scope: String) {
        db.withTransaction {
            when (scope) {
                "courses" -> kpDao.clearAll()
                "questions" -> {
                    attemptDao.clearAll()
                    wrongDao.clearAll()
                }
                "flashcards" -> {
                    fcDao.clearAllStates()
                    fcDao.clearAllEvents()
                    fcDao.clearAllSessions()
                }
                "lab" -> {
                    assetDao.clearQueryHistory()
                    assetDao.clearDrafts()
                }
                "all" -> {
                    kpDao.clearAll()
                    attemptDao.clearAll()
                    wrongDao.clearAll()
                    fcDao.clearAllStates()
                    fcDao.clearAllEvents()
                    fcDao.clearAllSessions()
                    assetDao.clearQueryHistory()
                    assetDao.clearDrafts()
                    assetDao.clearStudyLogs()
                }
            }
        }
    }

    suspend fun registerPersonalDataset(entity: PersonalDatasetEntity) {
        assetDao.upsertPersonalDataset(entity)
    }

    suspend fun deletePersonalDataset(ds: PersonalDatasetEntity) {
        assetDao.deletePersonalDataset(ds.id)
        runCatching { File(ds.dbFilePath).delete() }
    }

    suspend fun exportBackupJson(): String {
        val payload = BackupPayload(
            exportedAtMs = System.currentTimeMillis(),
            kpProgress = kpDao.getAll(),
            questionAttempts = attemptDao.getAll(),
            wrongRecords = wrongDao.getAll(),
            flashcardStates = fcDao.getAllStates(),
            flashcardEvents = fcDao.getAllEvents(),
            userFlashcards = fcDao.getAllUserFlashcards(),
            favorites = assetDao.getAllFavorites(),
            notes = assetDao.getAllNotes(),
            drafts = assetDao.getAllDrafts(),
            queryHistory = assetDao.getAllQueryHistory(),
            studyLogs = assetDao.getAllStudyLogs(),
            settings = settingsStore.current()
        )
        return BackupManager.serialize(payload)
    }

    fun inspectBackup(rawJson: String): Result<BackupInspectSummary> = BackupManager.parseAndValidate(rawJson)

    suspend fun restoreBackup(payload: BackupPayload, overwrite: Boolean): Result<Unit> = runCatching {
        db.withTransaction {
            if (overwrite) {
                kpDao.clearAll()
                attemptDao.clearAll()
                wrongDao.clearAll()
                fcDao.clearAllStates()
                fcDao.clearAllEvents()
                fcDao.clearAllUserFlashcards()
                assetDao.clearFavorites()
                assetDao.clearNotes()
                assetDao.clearDrafts()
                assetDao.clearQueryHistory()
                assetDao.clearStudyLogs()
            }
            kpDao.upsertAll(payload.kpProgress)
            if (overwrite) {
                attemptDao.insertAll(payload.questionAttempts)
            } else {
                val existingIds = attemptDao.getAll().map { it.id }.toSet()
                attemptDao.insertAll(payload.questionAttempts.filter { it.id !in existingIds })
            }
            wrongDao.upsertAll(payload.wrongRecords)
            fcDao.upsertAllStates(payload.flashcardStates)
            fcDao.insertAllEvents(payload.flashcardEvents)
            fcDao.upsertAllUserFlashcards(payload.userFlashcards)
            assetDao.upsertAllFavorites(payload.favorites)
            assetDao.upsertAllNotes(payload.notes)
            assetDao.upsertAllDrafts(payload.drafts)
            if (overwrite) {
                assetDao.insertAllQueryHistory(payload.queryHistory)
            } else {
                val existingHist = assetDao.getAllQueryHistory().map { it.id }.toSet()
                assetDao.insertAllQueryHistory(payload.queryHistory.filter { it.id !in existingHist })
            }
            assetDao.upsertAllStudyLogs(payload.studyLogs)
        }
        payload.settings?.let { restored ->
            settingsStore.update { restored }
        }
    }

    suspend fun saveCustomFlashcard(card: UserFlashcardEntity) {
        fcDao.upsertUserFlashcard(card)
    }

    suspend fun deleteCustomFlashcard(id: String) {
        fcDao.deleteUserFlashcard(id)
        fcDao.deleteState(id)
    }

    suspend fun duplicateBuiltinToCustom(card: FlashcardItem): UserFlashcardEntity {
        val now = System.currentTimeMillis()
        val copy = UserFlashcardEntity(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            chapterId = card.chapterId,
            type = card.type,
            difficulty = card.difficulty,
            frontQuestion = card.front.question,
            frontCode = card.front.code,
            backAnswer = card.back.answer,
            backExplanation = card.back.explanation,
            backCode = card.back.code ?: card.back.sqliteCode,
            note = "改写自内置卡 ${card.id}",
            createdAtMs = now,
            updatedAtMs = now
        )
        fcDao.upsertUserFlashcard(copy)
        return copy
    }

    fun buildReviewQueue(
        mode: FlashcardReviewMode,
        filterChapterId: String?,
        allBuiltin: List<FlashcardItem>,
        customCards: List<UserFlashcardEntity>,
        statesMap: Map<String, FlashcardStateEntity>,
        favoriteCardIds: Set<String>,
        settings: AppSettings
    ): List<FlashcardItem> {
        val customAsItems = customCards.map { c ->
            FlashcardItem(
                id = c.id,
                chapterId = c.chapterId,
                type = c.type,
                difficulty = c.difficulty,
                front = FlashcardFront(question = c.frontQuestion, code = c.frontCode),
                back = FlashcardBack(answer = c.backAnswer, explanation = c.backExplanation, code = c.backCode, notes = c.note),
                supportStatus = if (c.backCode != null) "直接兼容" else "仅理论"
            )
        }
        val combined = allBuiltin + customAsItems
        val now = System.currentTimeMillis()

        return when (mode) {
            FlashcardReviewMode.DUE -> {
                val due = combined.filter { card ->
                    val st = statesMap[card.id]
                    st != null && st.dueAtMs <= now
                }
                val newCards = combined.filter { it.id !in statesMap }.take(settings.dailyNewCardLimit)
                (due + newCards).take(30)
            }
            FlashcardReviewMode.NEW -> {
                combined.filter { it.id !in statesMap }.take(settings.dailyNewCardLimit)
            }
            FlashcardReviewMode.WEAK -> {
                combined.filter { card ->
                    val st = statesMap[card.id]
                    st != null && (st.lapses > 0 || st.difficulty >= 6.5 || st.state == 3)
                }.sortedByDescending { statesMap[it.id]?.lapses ?: 0 }
            }
            FlashcardReviewMode.CHAPTER -> {
                combined.filter { filterChapterId.isNullOrBlank() || it.chapterId == filterChapterId }
            }
            FlashcardReviewMode.CUSTOM -> customAsItems
            FlashcardReviewMode.FAVORITE -> combined.filter { it.id in favoriteCardIds }
            FlashcardReviewMode.RANDOM -> combined.shuffled().take(20)
        }
    }

    suspend fun saveFlashcardSession(session: FlashcardSessionEntity) {
        fcDao.upsertSession(session)
    }

    suspend fun reviewFlashcard(
        cardId: String,
        isCustom: Boolean,
        contentVersion: String,
        rating: FlashcardRating,
        sessionId: String?,
        countTowardsSchedule: Boolean,
        desiredRetention: Double
    ): Pair<FsrsCardStateData, FlashcardEventEntity?> {
        val now = System.currentTimeMillis()
        val existingEntity = fcDao.getState(cardId)
        val prevState = existingEntity?.let {
            FsrsCardStateData(
                cardId = it.cardId,
                dueAtMs = it.dueAtMs,
                stability = it.stability,
                difficulty = it.difficulty,
                lastReviewAtMs = it.lastReviewAtMs,
                reps = it.reps,
                lapses = it.lapses,
                state = it.state
            )
        } ?: FsrsEngine.newCardState(cardId, now)

        if (!countTowardsSchedule) {
            return prevState to null
        }

        val nextState = FsrsEngine.schedule(prevState, rating, desiredRetention, now)
        val nextEntity = FlashcardStateEntity(
            cardId = cardId,
            isCustom = isCustom,
            contentVersion = contentVersion,
            dueAtMs = nextState.dueAtMs,
            stability = nextState.stability,
            difficulty = nextState.difficulty,
            lastReviewAtMs = nextState.lastReviewAtMs,
            reps = nextState.reps,
            lapses = nextState.lapses,
            state = nextState.state,
            updatedAtMs = now
        )
        val event = FlashcardEventEntity(
            eventId = UUID.randomUUID().toString(),
            cardId = cardId,
            sessionId = sessionId,
            rating = rating.value,
            reviewTimestampMs = now,
            countTowardsSchedule = true,
            prevStateJson = existingEntity?.let { json.encodeToString(it) },
            newStateJson = json.encodeToString(nextEntity)
        )
        db.withTransaction {
            fcDao.upsertState(nextEntity)
            fcDao.insertEvent(event)
        }
        return nextState to event
    }

    suspend fun undoLastFlashcardEvent(eventId: String): Boolean {
        val evt = fcDao.getEvent(eventId) ?: return false
        db.withTransaction {
            val prevJson = evt.prevStateJson
            if (prevJson == null) {
                fcDao.deleteState(evt.cardId)
            } else {
                val restored = json.decodeFromString<FlashcardStateEntity>(prevJson)
                fcDao.upsertState(restored)
            }
            fcDao.deleteEvent(eventId)
        }
        return true
    }
}
