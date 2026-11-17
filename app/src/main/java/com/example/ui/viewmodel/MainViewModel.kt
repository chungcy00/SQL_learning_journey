package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.os.CancellationSignal
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SqlLearningApp
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
import com.example.data.local.UserFlashcardEntity
import com.example.data.local.WrongRecordEntity
import com.example.engine.BackupInspectSummary
import com.example.engine.BackupPayload
import com.example.engine.FsrsEngine
import com.example.engine.ImportExportEngine
import com.example.engine.SqlExecutionEngine
import com.example.model.ImportColumnSpec
import com.example.model.BusinessProjectInfo
import com.example.model.ChapterDetail
import com.example.model.DatabaseSchemaInfo
import com.example.model.ExecutionResult
import com.example.model.FlashcardItem
import com.example.model.FlashcardRating
import com.example.model.FlashcardReviewMode
import com.example.model.FsrsCardStateData
import com.example.model.ImportPreviewState
import com.example.model.JudgeResult
import com.example.model.KnowledgeMasteryInfo
import com.example.model.KnowledgePoint
import com.example.model.OfflineCheckReport
import com.example.model.QuestionFixture
import com.example.model.QuestionItem
import com.example.model.SourceTable
import com.example.model.StatsOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as SqlLearningApp).container
    val contentRepo = container.contentRepository
    val learningRepo = container.learningRepository

    private val _contentReady = MutableStateFlow(false)
    val contentReady: StateFlow<Boolean> = _contentReady.asStateFlow()

    private val _offlineReport = MutableStateFlow<OfflineCheckReport?>(null)
    val offlineReport: StateFlow<OfflineCheckReport?> = _offlineReport.asStateFlow()

    private val _isCheckingOffline = MutableStateFlow(false)
    val isCheckingOffline: StateFlow<Boolean> = _isCheckingOffline.asStateFlow()

    val settingsFlow: StateFlow<AppSettings> = learningRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val kpProgressList: StateFlow<List<KpProgressEntity>> = learningRepo.kpProgressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedKpIds: StateFlow<Set<String>> = learningRepo.completedKpIdsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val questionAttempts: StateFlow<List<QuestionAttemptEntity>> = learningRepo.questionAttemptsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wrongRecords: StateFlow<List<WrongRecordEntity>> = learningRepo.wrongRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcardStates: StateFlow<List<FlashcardStateEntity>> = learningRepo.flashcardStatesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcardEvents: StateFlow<List<FlashcardEventEntity>> = learningRepo.flashcardEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userFlashcards: StateFlow<List<UserFlashcardEntity>> = learningRepo.userFlashcardsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteEntity>> = learningRepo.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = learningRepo.notesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queryHistory: StateFlow<List<QueryHistoryEntity>> = learningRepo.queryHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalDatasets: StateFlow<List<PersonalDatasetEntity>> = learningRepo.personalDatasetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statsOverview: StateFlow<StatsOverview> = learningRepo.statsOverviewFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsOverview())

    init {
        viewModelScope.launch {
            contentRepo.ensureLoaded()
            _contentReady.value = true
            runOfflineSelfCheck()
        }
        // Record active study seconds every 15s while app ViewModel is alive
        viewModelScope.launch {
            while (isActive) {
                delay(15_000L)
                if (_contentReady.value) {
                    learningRepo.recordStudySeconds(15L)
                }
            }
        }
    }

    fun chapters(): List<ChapterDetail> = contentRepo.chapters
    fun knowledgePoints(): List<KnowledgePoint> = contentRepo.allKnowledgePoints
    fun questions(): List<QuestionItem> = contentRepo.allQuestions
    fun flashcards(): List<FlashcardItem> = contentRepo.allFlashcards
    fun businessProjects(): List<BusinessProjectInfo> = contentRepo.businessProjects
    fun schemas(): Map<String, DatabaseSchemaInfo> = contentRepo.schemas

    fun runOfflineSelfCheck() {
        if (_isCheckingOffline.value) return
        viewModelScope.launch {
            _isCheckingOffline.value = true
            val report = contentRepo.runRealOfflineSelfCheck()
            _offlineReport.value = report
            learningRepo.settingsStore.update {
                it.copy(
                    lastOfflineCheckAtMs = report.checkedAtMs,
                    lastOfflineCheckPassed = report.isReady,
                    lastOfflineFileCount = report.totalFilesChecked
                )
            }
            _isCheckingOffline.value = false
        }
    }

    fun toggleKpCompleted(kp: KnowledgePoint) {
        viewModelScope.launch {
            learningRepo.toggleKpCompleted(kp.id, kp.chapterId)
        }
    }

    fun markKpVisited(kpId: String) {
        viewModelScope.launch {
            learningRepo.markKpVisited(kpId)
        }
    }

    fun toggleFavorite(targetType: String, targetId: String, title: String, subtitle: String = "", payload: String = "") {
        viewModelScope.launch {
            learningRepo.toggleFavorite(targetType, targetId, title, subtitle, payload)
        }
    }

    fun saveNote(targetType: String, targetId: String, content: String) {
        viewModelScope.launch {
            learningRepo.saveNote(targetType, targetId, content)
        }
    }

    fun saveDraft(scopeKey: String, code: String, cursorPos: Int, datasetId: String = "retail") {
        viewModelScope.launch {
            learningRepo.saveDraft(scopeKey, code, cursorPos, datasetId)
        }
    }

    suspend fun loadDraft(scopeKey: String): String? = learningRepo.loadDraft(scopeKey)?.code

    suspend fun runKpExample(kp: KnowledgePoint, sql: String): ExecutionResult {
        val tables = if (kp.sourceTables.isNotEmpty()) {
            kp.sourceTables
        } else if (kp.fixtureIds.isNotEmpty()) {
            contentRepo.loadLessonFixtureTables(kp.fixtureIds.first())
        } else emptyList()

        return if (tables.isNotEmpty()) {
            SqlExecutionEngine.runOnTables(
                tables = tables,
                setupSql = kp.setupSql,
                rawSql = sql,
                allowWrite = kp.chapterId == "ch17"
            )
        } else if (!kp.datasetId.isNullOrBlank()) {
            val file = contentRepo.ensureDatabaseExtracted(kp.datasetId)
            if (file != null) {
                SqlExecutionEngine.runOnDatabaseFile(file, sql, readOnly = true)
            } else {
                ExecutionResult(success = false, errorMessage = "数据库尚未解压就绪")
            }
        } else {
            ExecutionResult(success = false, errorMessage = "该知识点为概念或理论讲解，暂无可执行的本地数据表。")
        }
    }

    suspend fun loadFixturesForQuestion(question: QuestionItem): List<QuestionFixture> =
        contentRepo.loadQuestionFixtures(question)

    suspend fun trialRunQuestion(
        question: QuestionItem,
        fixtures: List<QuestionFixture>,
        userSql: String,
        signal: CancellationSignal? = null
    ): ExecutionResult {
        val fullSql = if (question.type == "代码填空" && question.initialCode.contains("____")) {
            val parts = userSql.split("||").map { it.trim() }
            var assembled = question.initialCode
            var idx = 0
            while (assembled.contains("____")) {
                assembled = assembled.replaceFirst("____", parts.getOrElse(idx) { userSql.trim() })
                idx++
            }
            assembled
        } else userSql

        val ds = question.datasetId
        if (!ds.isNullOrBlank() && fixtures.none { it.tables.isNotEmpty() }) {
            val dbFile = contentRepo.ensureDatabaseExtracted(ds)
                ?: return ExecutionResult(success = false, errorMessage = "企业数据库 ${ds.removeSuffix("-v2")} 尚未准备就绪")
            return SqlExecutionEngine.runOnDatabaseFile(dbFile, fullSql, readOnly = true, cancellationSignal = signal)
        }

        val pub = fixtures.firstOrNull { it.variant == "public" } ?: fixtures.firstOrNull()
        val tables = pub?.tables ?: emptyList()
        val allowWrite = question.chapterId == "ch17" || pub?.kind == "ddl_dml"
        return SqlExecutionEngine.runOnTables(
            tables = tables,
            setupSql = pub?.setupSql ?: question.setupSql,
            rawSql = fullSql,
            allowWrite = allowWrite,
            cancellationSignal = signal
        )
    }

    suspend fun submitQuestion(
        question: QuestionItem,
        fixtures: List<QuestionFixture>,
        userInput: String,
        hintsUsed: Int,
        viewedAnswer: Boolean
    ): JudgeResult {
        val result = SqlExecutionEngine.judgeQuestion(
            question = question,
            fixtures = fixtures,
            userInput = userInput,
            databaseFileProvider = { dbId -> contentRepo.ensureDatabaseExtracted(dbId) }
        )
        learningRepo.recordQuestionSubmission(question, userInput, result, hintsUsed, viewedAnswer)
        return result
    }

    suspend fun runLabQuery(
        datasetId: String,
        sql: String,
        signal: CancellationSignal? = null
    ): ExecutionResult {
        val cleanId = datasetId.removeSuffix("-v2")
        val personal = personalDatasets.value.firstOrNull { it.id == cleanId }
        val res = if (personal != null) {
            val file = File(personal.dbFilePath)
            SqlExecutionEngine.runOnDatabaseFile(file, sql, readOnly = false, cancellationSignal = signal)
        } else {
            val wsFile = contentRepo.getOrCreateLabWorkspaceCopy(cleanId)
                ?: return ExecutionResult(success = false, errorMessage = "无法打开 $cleanId 工作副本")
            SqlExecutionEngine.runOnDatabaseFile(wsFile, sql, readOnly = false, cancellationSignal = signal)
        }
        learningRepo.insertQueryHistory(
            datasetId = cleanId,
            sql = sql,
            rowCount = res.totalRows,
            elapsedMs = res.elapsedMs,
            success = res.success,
            errorMessage = res.errorMessage
        )
        learningRepo.settingsStore.update { it.copy(lastDatasetId = cleanId) }
        return res
    }

    suspend fun inspectLabTables(datasetId: String): List<Pair<String, List<String>>> = withContext(Dispatchers.IO) {
        val cleanId = datasetId.removeSuffix("-v2")
        val personal = personalDatasets.value.firstOrNull { it.id == cleanId }
        val file = if (personal != null) File(personal.dbFilePath) else contentRepo.getOrCreateLabWorkspaceCopy(cleanId)
        if (file == null || !file.exists()) return@withContext emptyList()
        val schema = ImportExportEngine.inspectDatabaseSchema(cleanId, file)
        schema.tables.map { (tblName, tblInfo) ->
            tblName to tblInfo.columns.map { it.name }
        }
    }

    suspend fun resetLabWorkspace(datasetId: String): Boolean {
        val f = contentRepo.resetLabWorkspace(datasetId)
        return f != null && f.exists()
    }

    suspend fun runFlashcardExample(card: FlashcardItem): ExecutionResult {
        val sql = card.back.sqliteCode ?: card.back.code ?: card.front.code ?: return ExecutionResult(false, errorMessage = "该闪卡为概念卡，无示例查询")
        val tables = if (card.front.sourceTables.isNotEmpty()) {
            card.front.sourceTables
        } else {
            contentRepo.loadFlashcardFixtureTables(card)
        }
        return if (tables.isNotEmpty()) {
            SqlExecutionEngine.runOnTables(tables = tables, rawSql = sql, allowWrite = card.chapterId == "ch17")
        } else if (!card.databaseRef.isNullOrBlank()) {
            val file = contentRepo.ensureDatabaseExtracted(card.databaseRef)
                ?: return ExecutionResult(false, errorMessage = "未找到业务数据库 ${card.databaseRef}")
            SqlExecutionEngine.runOnDatabaseFile(file, sql, readOnly = true)
        } else {
            ExecutionResult(false, errorMessage = "当前卡片仅包含理论知识对比")
        }
    }

    fun computeMastery(kp: KnowledgePoint): KnowledgeMasteryInfo {
        val stateMap = flashcardStates.value.associateBy { it.cardId }
        val wrongMap = wrongRecords.value.associateBy { it.questionId }
        return learningRepo.computeKnowledgeMastery(
            kp = kp,
            completedKps = completedKpIds.value,
            attempts = questionAttempts.value,
            states = stateMap,
            wrongMap = wrongMap
        )
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            learningRepo.settingsStore.update(transform)
        }
    }

    fun resetProgress(scope: String) {
        viewModelScope.launch {
            learningRepo.resetProgress(scope)
        }
    }

    // --- Personal Datasets & Import ---

    suspend fun parseUriForImport(uri: Uri, displayName: String): Result<ImportPreviewState> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("无法读取所选文件")
            ImportExportEngine.parseFileForPreview(displayName, bytes).getOrThrow()
        }
    }

    suspend fun commitImportPersonalDataset(
        preview: ImportPreviewState,
        datasetName: String,
        tableName: String,
        columns: List<ImportColumnSpec>
    ): Result<PersonalDatasetEntity> = withContext(Dispatchers.IO) {
        ImportExportEngine.importIntoPersonalDatabase(
            context = getApplication(),
            preview = preview,
            datasetName = datasetName,
            targetTableName = tableName,
            columns = columns
        ).mapCatching { (dbFile, schema, totalRows) ->
            val id = dbFile.nameWithoutExtension
            val entity = PersonalDatasetEntity(
                id = id,
                name = datasetName.ifBlank { preview.fileName.substringBeforeLast('.') },
                sourceType = preview.sourceType,
                tableCount = schema.tables.size,
                rowCount = totalRows,
                dbFilePath = dbFile.absolutePath,
                schemaJson = ""
            )
            learningRepo.registerPersonalDataset(entity)
            entity
        }
    }

    suspend fun copyBuiltinToPersonal(datasetId: String): Result<PersonalDatasetEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val src = contentRepo.ensureDatabaseExtracted(datasetId) ?: error("内置库提取失败")
            val dir = contentRepo.getPersonalDatabaseDir()
            val id = "personal_${datasetId}_${UUID.randomUUID().toString().take(6)}"
            val dst = File(dir, "$id.sqlite")
            src.copyTo(dst, overwrite = true)
            val schema = ImportExportEngine.inspectDatabaseSchema(id, dst)
            val entity = PersonalDatasetEntity(
                id = id,
                name = "${datasetId}_个人副本",
                sourceType = "copied_template",
                tableCount = schema.tables.size,
                rowCount = schema.tables.values.sumOf { it.rowCount },
                dbFilePath = dst.absolutePath,
                schemaJson = ""
            )
            learningRepo.registerPersonalDataset(entity)
            entity
        }
    }

    fun deletePersonalDataset(ds: PersonalDatasetEntity) {
        viewModelScope.launch {
            learningRepo.deletePersonalDataset(ds)
        }
    }

    // --- Backup / Restore ---

    suspend fun generateBackupJson(): String = learningRepo.exportBackupJson()

    fun inspectBackupJson(rawJson: String): Result<BackupInspectSummary> = learningRepo.inspectBackup(rawJson)

    suspend fun restoreBackup(payload: BackupPayload, overwrite: Boolean): Result<Unit> =
        learningRepo.restoreBackup(payload, overwrite)

    // --- Custom Flashcard CRUD ---

    fun saveCustomFlashcard(card: UserFlashcardEntity) {
        viewModelScope.launch {
            learningRepo.saveCustomFlashcard(card)
        }
    }

    fun deleteCustomFlashcard(id: String) {
        viewModelScope.launch {
            learningRepo.deleteCustomFlashcard(id)
        }
    }

    suspend fun duplicateBuiltinCard(card: FlashcardItem): UserFlashcardEntity =
        learningRepo.duplicateBuiltinToCustom(card)

    // --- Flashcard Session Helper ---

    suspend fun startOrResumeFlashcardSession(
        mode: FlashcardReviewMode,
        chapterId: String?,
        countTowardsSchedule: Boolean
    ): Pair<FlashcardSessionEntity?, List<FlashcardItem>> {
        val allBuiltin = contentRepo.allFlashcards
        val custom = userFlashcards.value
        val statesMap = flashcardStates.value.associateBy { it.cardId }
        val favIds = favorites.value.filter { it.targetType == "flashcard" }.map { it.targetId }.toSet()
        val settings = settingsFlow.value

        val queue = learningRepo.buildReviewQueue(
            mode = mode,
            filterChapterId = chapterId,
            allBuiltin = allBuiltin,
            customCards = custom,
            statesMap = statesMap,
            favoriteCardIds = favIds,
            settings = settings
        )
        if (queue.isEmpty()) return null to emptyList()
        val session = FlashcardSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            mode = mode.id,
            filterId = chapterId.orEmpty(),
            cardIdsJson = queue.joinToString(",") { it.id },
            currentPosition = 0,
            startedAtMs = System.currentTimeMillis(),
            endedAtMs = null,
            countTowardsSchedule = countTowardsSchedule
        )
        learningRepo.saveFlashcardSession(session)
        return session to queue
    }

    suspend fun rateCardInSession(
        card: FlashcardItem,
        rating: FlashcardRating,
        sessionId: String?,
        countTowardsSchedule: Boolean
    ): Pair<FsrsCardStateData, FlashcardEventEntity?> {
        return learningRepo.reviewFlashcard(
            cardId = card.id,
            isCustom = card.id.startsWith("custom_"),
            contentVersion = card.contentVersion,
            rating = rating,
            sessionId = sessionId,
            countTowardsSchedule = countTowardsSchedule,
            desiredRetention = settingsFlow.value.desiredRetention
        )
    }

    suspend fun undoFlashcardEvent(eventId: String): Boolean = learningRepo.undoLastFlashcardEvent(eventId)
}
