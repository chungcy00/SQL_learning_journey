package com.example.engine

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.CancellationSignal
import com.example.model.ExecutionResult
import com.example.model.FixtureCheckDetail
import com.example.model.JudgeResult
import com.example.model.NumericTolerance
import com.example.model.QuestionFixture
import com.example.model.QuestionItem
import com.example.model.SourceTable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import kotlin.math.abs

object SqlExecutionEngine {

    private const val MAX_PREVIEW_ROWS = 500

    fun runOnTables(
        tables: List<SourceTable>,
        setupSql: String? = null,
        rawSql: String,
        allowWrite: Boolean = false,
        cancellationSignal: CancellationSignal? = null
    ): ExecutionResult {
        val compat = SqlCompatibilityEngine.translateForSqlite(rawSql, allowWriteStatements = allowWrite)
        if (!compat.allowed) {
            return ExecutionResult(
                success = false,
                errorMessage = compat.blockReason ?: "语句被安全策略拦截",
                compatibilityNotes = compat.notes
            )
        }
        val start = System.currentTimeMillis()
        var db: SQLiteDatabase? = null
        return try {
            db = SQLiteDatabase.create(null)
            loadTablesIntoDb(db, tables)
            if (!setupSql.isNullOrBlank()) {
                val setupTranslated = SqlCompatibilityEngine.translateForSqlite(setupSql, allowWriteStatements = true)
                for (stmt in SqlCompatibilityEngine.splitTopLevelStatements(setupTranslated.sqliteSql)) {
                    if (stmt.isNotBlank()) db.execSQL(stmt)
                }
            }
            val exec = executeStatementsOnDb(db, compat.sqliteSql, cancellationSignal)
            exec.copy(
                elapsedMs = (System.currentTimeMillis() - start).coerceAtLeast(1L),
                compatibilityNotes = compat.notes
            )
        } catch (t: Throwable) {
            ExecutionResult(
                success = false,
                elapsedMs = (System.currentTimeMillis() - start).coerceAtLeast(1L),
                errorMessage = friendlySqliteError(t),
                compatibilityNotes = compat.notes
            )
        } finally {
            runCatching { db?.close() }
        }
    }

    fun runOnDatabaseFile(
        dbFile: File,
        rawSql: String,
        readOnly: Boolean = true,
        cancellationSignal: CancellationSignal? = null
    ): ExecutionResult {
        if (!dbFile.exists()) {
            return ExecutionResult(success = false, errorMessage = "数据库文件不存在：${dbFile.name}")
        }
        val compat = SqlCompatibilityEngine.translateForSqlite(rawSql, allowWriteStatements = !readOnly)
        if (!compat.allowed) {
            return ExecutionResult(
                success = false,
                errorMessage = compat.blockReason ?: "当前数据库为只读模式",
                compatibilityNotes = compat.notes
            )
        }
        val start = System.currentTimeMillis()
        var db: SQLiteDatabase? = null
        return try {
            val flags = if (readOnly) SQLiteDatabase.OPEN_READONLY else SQLiteDatabase.OPEN_READWRITE
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, flags)
            val exec = executeStatementsOnDb(db, compat.sqliteSql, cancellationSignal)
            exec.copy(
                elapsedMs = (System.currentTimeMillis() - start).coerceAtLeast(1L),
                compatibilityNotes = compat.notes
            )
        } catch (t: Throwable) {
            ExecutionResult(
                success = false,
                elapsedMs = (System.currentTimeMillis() - start).coerceAtLeast(1L),
                errorMessage = friendlySqliteError(t),
                compatibilityNotes = compat.notes
            )
        } finally {
            runCatching { db?.close() }
        }
    }

    private fun loadTablesIntoDb(db: SQLiteDatabase, tables: List<SourceTable>) {
        for (tbl in tables) {
            val cols = tbl.columns
            if (cols.isEmpty()) continue
            val colDefs = cols.joinToString(", ") { col ->
                val rawType = (col.sqliteType.ifBlank { col.type }.ifBlank { col.mysqlType }).uppercase()
                val mappedType = when {
                    rawType.contains("INT") -> "INTEGER"
                    rawType.contains("DEC") || rawType.contains("NUM") || rawType.contains("FLOAT") || rawType.contains("DOUBLE") || rawType.contains("REAL") -> "REAL"
                    else -> "TEXT"
                }
                "\"${col.name.replace("\"", "\"\"")}\" $mappedType"
            }
            val safeTable = tbl.name.replace("\"", "\"\"")
            db.execSQL("DROP TABLE IF EXISTS \"$safeTable\";")
            db.execSQL("CREATE TABLE \"$safeTable\" ($colDefs);")
            if (tbl.rows.isNotEmpty()) {
                val placeholders = cols.joinToString(", ") { "?" }
                val stmt = db.compileStatement("INSERT INTO \"$safeTable\" VALUES ($placeholders);")
                db.beginTransaction()
                try {
                    for (row in tbl.rows) {
                        stmt.clearBindings()
                        for (cIdx in cols.indices) {
                            val elem = row.getOrNull(cIdx)
                            bindJsonElement(stmt, cIdx + 1, elem)
                        }
                        stmt.executeInsert()
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                    stmt.close()
                }
            }
        }
    }

    private fun bindJsonElement(stmt: android.database.sqlite.SQLiteStatement, index: Int, elem: JsonElement?) {
        if (elem == null || elem is JsonNull) {
            stmt.bindNull(index)
            return
        }
        val prim = elem as? JsonPrimitive
        if (prim == null) {
            stmt.bindString(index, elem.toString())
            return
        }
        if (prim.isString) {
            stmt.bindString(index, prim.content)
            return
        }
        val content = prim.content
        when {
            content.equals("true", ignoreCase = true) -> stmt.bindLong(index, 1L)
            content.equals("false", ignoreCase = true) -> stmt.bindLong(index, 0L)
            content.toLongOrNull() != null -> stmt.bindLong(index, content.toLong())
            content.toDoubleOrNull() != null -> stmt.bindDouble(index, content.toDouble())
            else -> stmt.bindString(index, content)
        }
    }

    private fun executeStatementsOnDb(
        db: SQLiteDatabase,
        sql: String,
        signal: CancellationSignal?
    ): ExecutionResult {
        val stmts = SqlCompatibilityEngine.splitTopLevelStatements(sql)
        if (stmts.isEmpty()) {
            return ExecutionResult(success = false, errorMessage = "未检测到可执行的 SQL 语句")
        }
        var lastResult = ExecutionResult(success = true, columns = listOf("status"), rows = listOf(listOf("OK")), totalRows = 1)
        for (stmt in stmts) {
            signal?.throwIfCanceled()
            val maskedUpper = SqlCompatibilityEngine.maskLiteralsAndComments(stmt).trim().uppercase()
            val returnsRows = maskedUpper.startsWith("SELECT") ||
                maskedUpper.startsWith("WITH") ||
                maskedUpper.startsWith("EXPLAIN")
            if (returnsRows) {
                db.rawQuery(stmt, null, signal).use { cursor ->
                    val cols = cursor.columnNames.toList()
                    val rows = mutableListOf<List<String?>>()
                    var total = 0
                    var truncated = false
                    while (cursor.moveToNext()) {
                        signal?.throwIfCanceled()
                        total++
                        if (rows.size < MAX_PREVIEW_ROWS) {
                            val row = ArrayList<String?>(cols.size)
                            for (c in cols.indices) {
                                row.add(readCursorCell(cursor, c))
                            }
                            rows.add(row)
                        } else {
                            truncated = true
                        }
                    }
                    lastResult = ExecutionResult(
                        success = true,
                        columns = cols,
                        rows = rows,
                        totalRows = total,
                        isTruncated = truncated
                    )
                }
            } else {
                db.execSQL(stmt)
                lastResult = ExecutionResult(
                    success = true,
                    columns = listOf("status"),
                    rows = listOf(listOf("执行成功")),
                    totalRows = 1
                )
            }
        }
        return lastResult
    }

    private fun readCursorCell(cursor: Cursor, colIdx: Int): String? {
        return when (cursor.getType(colIdx)) {
            Cursor.FIELD_TYPE_NULL -> null
            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(colIdx).toString()
            Cursor.FIELD_TYPE_FLOAT -> {
                val d = cursor.getDouble(colIdx)
                if (d == d.toLong().toDouble() && abs(d) < 1e14) {
                    d.toLong().toString()
                } else {
                    "%.6f".format(d).trimEnd('0').trimEnd('.')
                }
            }
            Cursor.FIELD_TYPE_BLOB -> "<BLOB ${cursor.getBlob(colIdx).size}B>"
            else -> cursor.getString(colIdx)
        }
    }

    fun compareResults(
        actualCols: List<String>,
        actualRows: List<List<String?>>,
        expectedCols: List<String>,
        expectedRows: List<List<String?>>,
        orderPolicy: String,
        columnPolicy: String,
        tolerance: NumericTolerance
    ): Pair<Boolean, String> {
        if (actualRows.size != expectedRows.size) {
            return false to "结果行数不一致：预期 ${expectedRows.size} 行，实际 ${actualRows.size} 行。"
        }

        val colMapping: List<Int>
        when (columnPolicy.lowercase()) {
            "exact_names_any_order" -> {
                if (actualCols.size != expectedCols.size) {
                    return false to "输出列数不一致：预期 ${expectedCols.size} 列 (${expectedCols.joinToString()}), 实际 ${actualCols.size} 列。"
                }
                val actualLower = actualCols.map { it.trim().lowercase() }
                val mapping = mutableListOf<Int>()
                val used = BooleanArray(actualCols.size)
                for (expCol in expectedCols) {
                    val idx = actualLower.indexOfFirst { it == expCol.trim().lowercase() }
                    if (idx < 0 || used[idx]) {
                        return false to "缺少预期列「$expCol」，实际返回列为：${actualCols.joinToString()}。"
                    }
                    used[idx] = true
                    mapping.add(idx)
                }
                colMapping = mapping
            }
            "any_columns" -> {
                if (actualCols.size != expectedCols.size) {
                    return false to "输出列数不一致：预期 ${expectedCols.size} 列，实际 ${actualCols.size} 列。"
                }
                colMapping = actualCols.indices.toList()
            }
            else -> { // exact_names_in_order
                if (actualCols.size != expectedCols.size) {
                    return false to "输出列数不一致：预期 ${expectedCols.size} 列 (${expectedCols.joinToString()}), 实际 ${actualCols.size} 列。"
                }
                for (i in expectedCols.indices) {
                    if (!actualCols[i].trim().equals(expectedCols[i].trim(), ignoreCase = true)) {
                        return false to "第 ${i + 1} 列列名不匹配：预期「${expectedCols[i]}」，实际「${actualCols[i]}」。"
                    }
                }
                colMapping = actualCols.indices.toList()
            }
        }

        val reorderedActual = actualRows.map { row ->
            colMapping.map { idx -> row.getOrNull(idx) }
        }

        if (orderPolicy.equals("unordered", ignoreCase = true)) {
            val matchedExpected = BooleanArray(expectedRows.size)
            for (aRow in reorderedActual) {
                var foundMatch = false
                for (eIdx in expectedRows.indices) {
                    if (!matchedExpected[eIdx] && rowsEqual(aRow, expectedRows[eIdx], tolerance)) {
                        matchedExpected[eIdx] = true
                        foundMatch = true
                        break
                    }
                }
                if (!foundMatch) {
                    return false to "存在与预期结果集不匹配的数据行（含重复行或 NULL 值差异）。"
                }
            }
            return true to "结果集完全一致。"
        } else {
            for (rIdx in expectedRows.indices) {
                if (!rowsEqual(reorderedActual[rIdx], expectedRows[rIdx], tolerance)) {
                    return false to "第 ${rIdx + 1} 行数据与预期不一致：实际 ${reorderedActual[rIdx]}，预期 ${expectedRows[rIdx]}。"
                }
            }
            return true to "结果集及排序完全一致。"
        }
    }

    private fun rowsEqual(a: List<String?>, b: List<String?>, tolerance: NumericTolerance): Boolean {
        if (a.size != b.size) return false
        for (i in a.indices) {
            if (!cellsEqual(a[i], b[i], tolerance)) return false
        }
        return true
    }

    private fun cellsEqual(rawA: String?, rawB: String?, tolerance: NumericTolerance): Boolean {
        if (rawA == null && rawB == null) return true
        if (rawA == null || rawB == null) return false

        val a = rawA.trim()
        val b = rawB.trim()
        val numA = a.toDoubleOrNull()
        val numB = b.toDoubleOrNull()
        if (numA != null && numB != null) {
            val tol = if (!a.contains('.') && !b.contains('.') && tolerance.integerExact) 0.0
            else maxOf(tolerance.absolute, tolerance.decimalAbsolute)
            return abs(numA - numB) <= tol
        }
        return if (tolerance.textExact) a == b else a.equals(b, ignoreCase = true)
    }

    suspend fun judgeQuestion(
        question: QuestionItem,
        fixtures: List<QuestionFixture>,
        userInput: String,
        databaseFileProvider: suspend (String) -> File?
    ): JudgeResult {
        val start = System.currentTimeMillis()
        val trimmedInput = userInput.trim()
        if (trimmedInput.isEmpty()) {
            return JudgeResult(
                passed = false,
                summary = "尚未作答",
                chineseFeedback = "请先输入或选择你的答案后再提交判题。"
            )
        }

        // 1. Choice questions
        if (question.type == "选择") {
            val passed = trimmedInput.equals(question.correctOptionId?.trim(), ignoreCase = true)
            val chosen = question.options.firstOrNull { it.id.equals(trimmedInput, ignoreCase = true) }
            val feedback = if (passed) {
                chosen?.explanation?.ifBlank { question.explanation }?.ifBlank { "回答正确！" } ?: "回答正确！"
            } else {
                val hint = chosen?.explanation?.takeIf { it.isNotBlank() }?.let { "所选解析：$it\n" }.orEmpty()
                "${hint}请再对照题目考点与 SQL 执行逻辑思考。"
            }
            return JudgeResult(
                passed = passed,
                summary = if (passed) "选择题回答正确" else "选择题回答有误",
                chineseFeedback = feedback,
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        // 2. Result prediction questions (NO SQL execution allowed for user answer)
        if (question.type == "结果预测") {
            val expected = (question.predictionAnswerText ?: question.correctOptionId ?: "").trim()
            val passed = normalizePrediction(trimmedInput) == normalizePrediction(expected)
            return JudgeResult(
                passed = passed,
                summary = if (passed) "推演预测完全正确" else "预测结果有偏差",
                chineseFeedback = if (passed) {
                    question.explanation.ifBlank { "你已准确推演出该查询的运行结果！" }
                } else {
                    "注意逐行检查过滤条件、NULL 传播与分组/窗口计算顺序。"
                },
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        // 3. Blank questions assemble code if template contains ____
        val fullSql = if (question.type == "代码填空" && question.initialCode.contains("____")) {
            val parts = trimmedInput.split("||").map { it.trim() }
            var assembled = question.initialCode
            var idx = 0
            while (assembled.contains("____")) {
                assembled = assembled.replaceFirst("____", parts.getOrElse(idx) { trimmedInput })
                idx++
            }
            assembled
        } else trimmedInput

        // Blank acceptance shortcut if no runnable fixture/dataset and matches acceptedBlanks
        if (question.type == "代码填空" && question.acceptedBlanks.isNotEmpty()) {
            val normUser = normalizeSqlWhitespace(trimmedInput)
            if (question.acceptedBlanks.any { normalizeSqlWhitespace(it) == normUser }) {
                return JudgeResult(
                    passed = true,
                    summary = "代码填空正确通过",
                    chineseFeedback = question.explanation.ifBlank { "语法关键字与位置完全准确！" },
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
        }

        // 4. Required AST structural checks
        val astError = SqlCompatibilityEngine.checkRequiredAstFeatures(fullSql, question.requiredAstFeatures)
        if (astError != null) {
            return JudgeResult(
                passed = false,
                summary = "专项语法结构未达标",
                chineseFeedback = astError,
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        val allowWrite = question.chapterId == "ch17" || fixtures.any { it.kind == "ddl_dml" }

        // 5. Evaluate against database if datasetId is configured and no custom fixture tables override
        val ds = question.datasetId?.removeSuffix("-v2")
        if (!ds.isNullOrBlank() && fixtures.none { it.tables.isNotEmpty() }) {
            val dbFile = databaseFileProvider(ds)
                ?: return JudgeResult(false, "无法加载业务库", "未找到业务数据库 $ds")
            val actualRes = runOnDatabaseFile(dbFile, fullSql, readOnly = true)
            if (!actualRes.success) {
                return JudgeResult(
                    passed = false,
                    summary = "SQL 执行报错",
                    chineseFeedback = actualRes.errorMessage ?: "语法或字段名有误",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
            var expectedCols = question.expectedColumns
            var expectedRows = jsonRowsToStrings(question.expectedRows)
            if (expectedCols.isEmpty()) {
                val refSql = question.referenceSolutions.firstOrNull()?.sqliteSql?.ifBlank { null }
                    ?: question.referenceSolutions.firstOrNull()?.mysqlSql
                if (!refSql.isNullOrBlank()) {
                    val refRes = runOnDatabaseFile(dbFile, refSql, readOnly = true)
                    if (refRes.success) {
                        expectedCols = refRes.columns
                        expectedRows = refRes.rows
                    }
                }
            }
            if (expectedCols.isEmpty()) {
                return JudgeResult(
                    passed = true,
                    summary = "查询执行成功",
                    chineseFeedback = "SQL 在 $ds 数据库中执行成功，共返回 ${actualRes.totalRows} 行。",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
            val (ok, msg) = compareResults(
                actualCols = actualRes.columns,
                actualRows = actualRes.rows,
                expectedCols = expectedCols,
                expectedRows = expectedRows,
                orderPolicy = question.orderPolicy,
                columnPolicy = question.columnPolicy,
                tolerance = question.numericTolerance
            )
            return JudgeResult(
                passed = ok,
                summary = if (ok) "业务实战验证通过" else "结果集与预期口径不一致",
                chineseFeedback = if (ok) "恭喜！$msg" else msg,
                fixtureResults = listOf(
                    FixtureCheckDetail(
                        fixtureName = "企业数据库 ($ds)",
                        visibility = "public",
                        passed = ok,
                        message = msg,
                        actualColumns = actualRes.columns,
                        actualRowsPreview = actualRes.rows.take(12),
                        expectedColumns = expectedCols,
                        expectedRowsPreview = expectedRows.take(12)
                    )
                ),
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        // 6. Theoretical / syntax-only fallback when no tables exist
        if (fixtures.isEmpty() || fixtures.all { it.tables.isEmpty() && it.datasetId.isNullOrBlank() }) {
            val refSql = question.referenceSolutions.firstOrNull()?.mysqlSql.orEmpty()
            val matched = refSql.isNotBlank() && normalizeSqlWhitespace(fullSql) == normalizeSqlWhitespace(refSql)
            return JudgeResult(
                passed = matched,
                summary = if (matched) "语法匹配通过" else "参考语义对比未匹配",
                chineseFeedback = if (matched) "作答与标准语句一致。" else "请核对 SQL 关键字拼写与语法结构（可结合提示或参考答案检查）。",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        // 7. Isolated fixture-by-fixture judging (public + hidden fixtures)
        val details = mutableListOf<FixtureCheckDetail>()
        var allPassed = true
        var lastCompatStatus = "直接兼容"

        for ((idx, fx) in fixtures.withIndex()) {
            val fxName = if (fx.visibility == "hidden") "隐藏验证集 #${idx + 1}" else "公开验证集 #${idx + 1}"
            val exec = runIsolatedFixture(fx, question.setupSql, fullSql, allowWrite, fx.verificationQuery)
            val compat = SqlCompatibilityEngine.translateForSqlite(fullSql, allowWrite)
            lastCompatStatus = compat.compatibilityStatus

            if (!exec.success) {
                allPassed = false
                details.add(
                    FixtureCheckDetail(
                        fixtureName = fxName,
                        visibility = fx.visibility,
                        passed = false,
                        message = exec.errorMessage ?: "执行失败"
                    )
                )
                continue
            }

            var expCols = fx.expected?.columns.orEmpty()
            var expRows = jsonRowsToStrings(fx.expected?.rows.orEmpty())
            if (expCols.isEmpty() && question.expectedColumns.isNotEmpty()) {
                expCols = question.expectedColumns
                expRows = jsonRowsToStrings(question.expectedRows)
            }
            if (expCols.isEmpty()) {
                val refSql = question.referenceSolutions.firstOrNull()?.sqliteSql?.ifBlank { null }
                    ?: question.referenceSolutions.firstOrNull()?.mysqlSql
                if (!refSql.isNullOrBlank()) {
                    val refExec = runIsolatedFixture(fx, question.setupSql, refSql, allowWrite, fx.verificationQuery)
                    if (refExec.success) {
                        expCols = refExec.columns
                        expRows = refExec.rows
                    }
                }
            }

            val (ok, msg) = if (expCols.isEmpty()) {
                true to "语句成功执行。"
            } else {
                compareResults(
                    actualCols = exec.columns,
                    actualRows = exec.rows,
                    expectedCols = expCols,
                    expectedRows = expRows,
                    orderPolicy = fx.orderPolicy.ifBlank { question.orderPolicy },
                    columnPolicy = fx.columnPolicy.ifBlank { question.columnPolicy },
                    tolerance = fx.numericTolerance
                )
            }

            if (!ok) allPassed = false
            details.add(
                FixtureCheckDetail(
                    fixtureName = fxName,
                    visibility = fx.visibility,
                    passed = ok,
                    message = if (fx.visibility == "hidden" && !ok) "未通过隐藏边界数据验证，请检查空值、去重或边界排序逻辑。" else msg,
                    actualColumns = exec.columns,
                    actualRowsPreview = exec.rows.take(12),
                    expectedColumns = if (fx.visibility == "public") expCols else emptyList(),
                    expectedRowsPreview = if (fx.visibility == "public") expRows.take(12) else emptyList()
                )
            )
        }

        val passedCount = details.count { it.passed }
        return JudgeResult(
            passed = allPassed,
            summary = if (allPassed) "全部 ${details.size} 组验证集通过" else "通过 $passedCount/${details.size} 组验证集",
            chineseFeedback = if (allPassed) {
                "太棒了！公开与边界隐藏用例已全部通过。"
            } else {
                details.firstOrNull { !it.passed }?.message ?: "结果与预期不一致，请检查查询逻辑。"
            },
            fixtureResults = details,
            executionTimeMs = System.currentTimeMillis() - start,
            compatibilityStatus = lastCompatStatus
        )
    }

    private fun runIsolatedFixture(
        fx: QuestionFixture,
        questionSetupSql: String?,
        userSql: String,
        allowWrite: Boolean,
        verificationQuery: String?
    ): ExecutionResult {
        val combined = if (!verificationQuery.isNullOrBlank()) {
            "${userSql.trimEnd(';', ' ', '\n')};\n$verificationQuery"
        } else userSql
        return runOnTables(
            tables = fx.tables,
            setupSql = fx.setupSql ?: questionSetupSql,
            rawSql = combined,
            allowWrite = allowWrite
        )
    }

    fun jsonRowsToStrings(rows: List<List<JsonElement?>>): List<List<String?>> {
        return rows.map { r ->
            r.map { elem ->
                when (elem) {
                    null, is JsonNull -> null
                    is JsonPrimitive -> elem.content
                    else -> elem.toString()
                }
            }
        }
    }

    private fun normalizePrediction(s: String): String =
        s.replace(Regex("\\s+"), " ").trim().lowercase()

    private fun normalizeSqlWhitespace(s: String): String =
        s.trim().trimEnd(';').replace(Regex("\\s+"), " ").lowercase()

    private fun friendlySqliteError(t: Throwable): String {
        val raw = t.message ?: t::class.simpleName ?: "执行异常"
        return when {
            raw.contains("OperationCanceledException", true) || raw.contains("canceled", true) ->
                "查询已由用户手动中断。"
            raw.contains("no such table", true) ->
                "数据表不存在：$raw"
            raw.contains("no such column", true) ->
                "字段名不存在或作用域有误：$raw"
            raw.contains("syntax error", true) ->
                "SQL 语法错误：$raw"
            raw.contains("ambiguous column name", true) ->
                "多表连接时字段名存在歧义，请加上表别名限定：$raw"
            else -> raw
        }
    }
}
