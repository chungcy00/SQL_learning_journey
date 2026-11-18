package com.example.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.model.BundleManifest
import com.example.model.BusinessProjectInfo
import com.example.model.ChapterDetail
import com.example.model.DatabaseSchemaInfo
import com.example.model.FlashcardChapterFile
import com.example.model.FlashcardFixtureFile
import com.example.model.FlashcardItem
import com.example.model.KnowledgePoint
import com.example.model.LessonFixture
import com.example.model.OfflineCheckReport
import com.example.model.QuestionChapterFile
import com.example.model.QuestionFixture
import com.example.model.QuestionItem
import com.example.model.SourceTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

class ContentRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val loadMutex = Mutex()
    private val extractMutex = Mutex()
    private var loaded = false

    var chapters: List<ChapterDetail> = emptyList()
        private set
    var allKnowledgePoints: List<KnowledgePoint> = emptyList()
        private set
    var allQuestions: List<QuestionItem> = emptyList()
        private set
    var allFlashcards: List<FlashcardItem> = emptyList()
        private set
    var businessProjects: List<BusinessProjectInfo> = emptyList()
        private set
    var schemas: Map<String, DatabaseSchemaInfo> = emptyMap()
        private set

    private var kpMap: Map<String, KnowledgePoint> = emptyMap()
    private var questionMap: Map<String, QuestionItem> = emptyMap()

    suspend fun ensureLoaded() {
        if (loaded) return
        loadMutex.withLock {
            if (loaded) return
            withContext(Dispatchers.IO) {
                val loadedChapters = mutableListOf<ChapterDetail>()
                val loadedQuestions = mutableListOf<QuestionItem>()
                val loadedFlashcards = mutableListOf<FlashcardItem>()

                for (i in 1..20) {
                    val chCode = "ch" + i.toString().padStart(2, '0')
                    readAssetText("content/courses/$chCode.json")?.let { txt ->
                        loadedChapters.add(json.decodeFromString(ChapterDetail.serializer(), txt))
                    }
                    readAssetText("content/questions/$chCode.json")?.let { txt ->
                        val qFile = json.decodeFromString(QuestionChapterFile.serializer(), txt)
                        loadedQuestions.addAll(qFile.questions)
                    }
                    readAssetText("content/flashcards/$chCode.json")?.let { txt ->
                        val fcFile = json.decodeFromString(FlashcardChapterFile.serializer(), txt)
                        loadedFlashcards.addAll(fcFile.cards)
                    }
                }

                val projList = readAssetText("content/projects/business_projects.json")?.let { txt ->
                    json.decodeFromString<List<BusinessProjectInfo>>(txt)
                }.orEmpty()

                val schemaMap = linkedMapOf<String, DatabaseSchemaInfo>()
                val schemaFiles = runCatching { context.assets.list("content/schemas")?.toList().orEmpty() }.getOrDefault(emptyList())
                for (fName in schemaFiles.sorted()) {
                    if (fName.endsWith(".json")) {
                        readAssetText("content/schemas/$fName")?.let { txt ->
                            val s = json.decodeFromString(DatabaseSchemaInfo.serializer(), txt)
                            val id = s.databaseId.ifBlank { fName.removeSuffix(".json") }.removeSuffix("-v2")
                            schemaMap[id] = s
                        }
                    }
                }

                chapters = loadedChapters.sortedBy { it.number }
                allKnowledgePoints = chapters.flatMap { it.knowledgePoints }
                allQuestions = loadedQuestions
                allFlashcards = loadedFlashcards
                businessProjects = projList
                schemas = schemaMap
                kpMap = allKnowledgePoints.associateBy { it.id }
                questionMap = allQuestions.associateBy { it.id }
                loaded = true
            }
        }
    }

    fun getKnowledgePoint(id: String): KnowledgePoint? = kpMap[id]
    fun getQuestion(id: String): QuestionItem? = questionMap[id]

    suspend fun loadLessonFixtureTables(fixtureId: String): List<SourceTable> = withContext(Dispatchers.IO) {
        val clean = fixtureId.removeSuffix(".json")
        val txt = readAssetText("content/lesson_fixtures/$clean.json") ?: return@withContext emptyList()
        runCatching {
            val lf = json.decodeFromString(LessonFixture.serializer(), txt)
            if (lf.tables.isNotEmpty()) lf.tables
            else if (lf.table.isNotBlank() && lf.columns.isNotEmpty()) {
                listOf(SourceTable(name = lf.table, columns = lf.columns, rows = lf.rows))
            } else emptyList()
        }.getOrDefault(emptyList())
    }

    suspend fun loadQuestionFixtures(question: QuestionItem): List<QuestionFixture> = withContext(Dispatchers.IO) {
        val results = mutableListOf<QuestionFixture>()
        val candidates = if (question.fixtureIds.isNotEmpty()) {
            question.fixtureIds
        } else {
            listOf("${question.id}-public", "${question.id}-hidden")
        }
        for (fid in candidates) {
            val clean = fid.removeSuffix(".json")
            val txt = readAssetText("content/question_fixtures/$clean.json")
            if (txt != null) {
                runCatching {
                    results.add(json.decodeFromString(QuestionFixture.serializer(), txt))
                }
            }
        }
        results
    }

    suspend fun loadFlashcardFixtureTables(card: FlashcardItem): List<SourceTable> = withContext(Dispatchers.IO) {
        val fid = card.fixtureId?.removeSuffix(".json") ?: return@withContext emptyList()
        val txt = readAssetText("content/flashcard_fixtures/$fid.json") ?: return@withContext emptyList()
        runCatching {
            val fx = json.decodeFromString(FlashcardFixtureFile.serializer(), txt)
            fx.tables
        }.getOrDefault(emptyList())
    }

    suspend fun ensureDatabaseExtracted(datasetId: String): File? = extractMutex.withLock {
        withContext(Dispatchers.IO) {
            val clean = datasetId.removeSuffix("-v2").removeSuffix(".sqlite").removeSuffix(".gz")
            if (clean.isBlank()) return@withContext null
            val outDir = File(context.filesDir, "extracted_dbs").apply { mkdirs() }
            val outFile = File(outDir, "$clean.sqlite")
            if (outFile.exists() && outFile.length() > 0L) {
                return@withContext outFile
            }
            val assetPath = "content/databases/$clean.sqlite.dat"
            runCatching {
                context.assets.open(assetPath).use { rawIn ->
                    GZIPInputStream(rawIn).use { gzIn ->
                        outFile.outputStream().use { out ->
                            gzIn.copyTo(out)
                        }
                    }
                }
                outFile
            }.getOrNull()
        }
    }

    suspend fun getOrCreateLabWorkspaceCopy(datasetId: String): File? = withContext(Dispatchers.IO) {
        val clean = datasetId.removeSuffix("-v2")
        val wsDir = File(context.filesDir, "lab_workspaces").apply { mkdirs() }
        val wsFile = File(wsDir, "${clean}_ws.sqlite")
        if (wsFile.exists() && wsFile.length() > 0L) return@withContext wsFile
        val base = ensureDatabaseExtracted(clean) ?: return@withContext null
        base.copyTo(wsFile, overwrite = true)
        wsFile
    }

    suspend fun resetLabWorkspace(datasetId: String): File? = withContext(Dispatchers.IO) {
        val clean = datasetId.removeSuffix("-v2")
        val wsDir = File(context.filesDir, "lab_workspaces").apply { mkdirs() }
        val wsFile = File(wsDir, "${clean}_ws.sqlite")
        val base = ensureDatabaseExtracted(clean) ?: return@withContext null
        base.copyTo(wsFile, overwrite = true)
        wsFile
    }

    fun getPersonalDatabaseDir(): File = File(context.filesDir, "personal_dbs").apply { mkdirs() }

    suspend fun runRealOfflineSelfCheck(): OfflineCheckReport = withContext(Dispatchers.IO) {
        val checkedAt = System.currentTimeMillis()
        val missingOrCorrupt = mutableListOf<String>()
        var totalAssetBytes = 0L
        var totalChecked = 0

        val manifestTxt = readAssetText("content/bundle_manifest.json")
        if (manifestTxt == null) {
            return@withContext OfflineCheckReport(
                checkedAtMs = checkedAt,
                isReady = false,
                totalFilesChecked = 0,
                missingOrCorruptFiles = listOf("content/bundle_manifest.json 缺失")
            )
        }

        val manifest = runCatching {
            json.decodeFromString(BundleManifest.serializer(), manifestTxt)
        }.getOrElse {
            return@withContext OfflineCheckReport(
                checkedAtMs = checkedAt,
                isReady = false,
                totalFilesChecked = 0,
                missingOrCorruptFiles = listOf("bundle_manifest.json 解析失败: ${it.message}")
            )
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16384)
        for (entry in manifest.files) {
            totalChecked++
            val assetPath = "content/${entry.path}"
            try {
                context.assets.open(assetPath).use { input ->
                    digest.reset()
                    var bytesReadTotal = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        digest.update(buffer, 0, n)
                        bytesReadTotal += n
                    }
                    totalAssetBytes += bytesReadTotal
                    val actualSha = digest.digest().joinToString("") { "%02x".format(it) }
                    if (actualSha != entry.sha256) {
                        missingOrCorrupt.add("${entry.path} (SHA-256 不匹配)")
                    }
                }
            } catch (t: Throwable) {
                missingOrCorrupt.add("${entry.path} (无法打开: ${t.message})")
            }
        }

        // Real SQLite smoke check on in-memory DB + extracted retail DB
        var smokeOk = false
        var smokeDetail = ""
        try {
            val memDb = SQLiteDatabase.create(null)
            memDb.execSQL("CREATE TABLE _smoke(id INTEGER PRIMARY KEY, v TEXT);")
            memDb.execSQL("INSERT INTO _smoke VALUES (1, 'ok');")
            val memRes = memDb.rawQuery("SELECT COUNT(*) FROM _smoke;", null).use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
            memDb.close()
            val retailFile = ensureDatabaseExtracted("retail")
            val retailOk = if (retailFile != null && retailFile.exists()) {
                val rdb = SQLiteDatabase.openDatabase(retailFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                val cnt = rdb.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table';", null).use { c ->
                    if (c.moveToFirst()) c.getInt(0) else 0
                }
                rdb.close()
                cnt > 0
            } else false
            smokeOk = memRes == 1 && retailOk
            smokeDetail = if (smokeOk) "SQLite 运行时与零售企业库连接正常" else "企业库提取或表验证失败"
        } catch (t: Throwable) {
            smokeOk = false
            smokeDetail = "SQLite 引擎异常: ${t.message}"
        }

        val extractedBytes = File(context.filesDir, "extracted_dbs").listFiles()?.sumOf { it.length() } ?: 0L
        val personalBytes = getPersonalDatabaseDir().listFiles()?.sumOf { it.length() } ?: 0L

        OfflineCheckReport(
            checkedAtMs = checkedAt,
            isReady = missingOrCorrupt.isEmpty() && smokeOk && totalChecked > 0,
            totalFilesChecked = totalChecked,
            missingOrCorruptFiles = missingOrCorrupt,
            sqliteSmokePassed = smokeOk,
            sqliteSmokeDetail = smokeDetail,
            totalAssetBytes = totalAssetBytes,
            extractedDbBytes = extractedBytes,
            personalDbBytes = personalBytes
        )
    }

    private fun readAssetText(path: String): String? {
        return runCatching {
            context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }
}
