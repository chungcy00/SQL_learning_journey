package com.example.engine

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.model.DatabaseSchemaInfo
import com.example.model.ImportColumnSpec
import com.example.model.ImportPreviewState
import com.example.model.SchemaColumnInfo
import com.example.model.TableSchemaInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.util.UUID

object ImportExportEngine {

    private val prettyJson = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun inspectDatabaseSchema(databaseId: String, dbFile: File): DatabaseSchemaInfo {
        if (!dbFile.exists()) return DatabaseSchemaInfo(databaseId = databaseId)
        val tables = linkedMapOf<String, TableSchemaInfo>()
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val tableNames = mutableListOf<String>()
            db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type IN ('table','view') AND name NOT LIKE 'sqlite_%' ORDER BY name;",
                null
            ).use { cur ->
                while (cur.moveToNext()) tableNames.add(cur.getString(0))
            }
            for (tName in tableNames) {
                val safeName = tName.replace("\"", "\"\"")
                val cols = mutableListOf<SchemaColumnInfo>()
                db.rawQuery("PRAGMA table_info(\"$safeName\");", null).use { cur ->
                    while (cur.moveToNext()) {
                        cols.add(
                            SchemaColumnInfo(
                                name = cur.getString(1),
                                type = cur.getString(2) ?: "TEXT",
                                nullable = cur.getInt(3) == 0,
                                primaryKey = cur.getInt(5) > 0
                            )
                        )
                    }
                }
                var cnt = 0
                runCatching {
                    db.rawQuery("SELECT COUNT(*) FROM \"$safeName\";", null).use { cur ->
                        if (cur.moveToFirst()) cnt = cur.getInt(0)
                    }
                }
                tables[tName] = TableSchemaInfo(rowCount = cnt, columns = cols)
            }
        } catch (_: Throwable) {
        } finally {
            runCatching { db?.close() }
        }
        return DatabaseSchemaInfo(databaseId = databaseId, tables = tables)
    }

    fun parseFileForPreview(fileName: String, bytes: ByteArray): Result<ImportPreviewState> = runCatching {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        when (ext) {
            "csv", "tsv", "txt" -> parseCsvPreview(fileName, bytes, if (ext == "tsv") '\t' else ',')
            "json" -> parseJsonPreview(fileName, bytes)
            "sql" -> {
                val text = bytes.toString(Charsets.UTF_8)
                val stmts = SqlCompatibilityEngine.splitTopLevelStatements(text)
                ImportPreviewState(
                    fileName = fileName,
                    sourceType = "sql",
                    columns = emptyList(),
                    sampleRows = emptyList(),
                    totalRowCount = stmts.size,
                    rawBytes = bytes,
                    warnings = listOf("将把包含 ${stmts.size} 条语句的 SQL 脚本导入为独立本地 SQLite 数据库。")
                )
            }
            "db", "sqlite", "sqlite3" -> {
                ImportPreviewState(
                    fileName = fileName,
                    sourceType = "sqlite",
                    columns = emptyList(),
                    sampleRows = emptyList(),
                    totalRowCount = 0,
                    rawBytes = bytes,
                    warnings = listOf("导入前会验证 SQLite 文件魔数与表结构。")
                )
            }
            else -> error("不支持的文件扩展名 .$ext，请选择 .csv / .json / .sqlite / .sql")
        }
    }

    fun importIntoPersonalDatabase(
        context: Context,
        preview: ImportPreviewState,
        datasetName: String,
        targetTableName: String,
        columns: List<ImportColumnSpec>
    ): Result<Triple<File, DatabaseSchemaInfo, Int>> = runCatching {
        val dir = File(context.filesDir, "personal_dbs").apply { mkdirs() }
        val id = "personal_${UUID.randomUUID().toString().take(8)}"
        val dst = File(dir, "$id.sqlite")

        when (preview.sourceType) {
            "sqlite" -> {
                val header = preview.rawBytes.take(15).toByteArray().toString(Charsets.US_ASCII)
                require(header.startsWith("SQLite format 3")) { "文件不是有效的 SQLite 3 数据库格式" }
                dst.writeBytes(preview.rawBytes)
            }
            "sql" -> {
                val db = SQLiteDatabase.openOrCreateDatabase(dst, null)
                try {
                    val raw = preview.rawBytes.toString(Charsets.UTF_8)
                    val compat = SqlCompatibilityEngine.translateForSqlite(raw, allowWriteStatements = true)
                    for (stmt in SqlCompatibilityEngine.splitTopLevelStatements(compat.sqliteSql)) {
                        if (stmt.isNotBlank()) db.execSQL(stmt)
                    }
                } finally {
                    db.close()
                }
            }
            "csv", "json" -> {
                val cleanTable = targetTableName.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "imported_table" }
                val rows = if (preview.sourceType == "csv") {
                    parseCsvAllRows(preview.rawBytes.toString(Charsets.UTF_8), if (preview.fileName.endsWith(".tsv", true)) '\t' else ',').drop(1)
                } else {
                    parseJsonAllRows(preview.rawBytes.toString(Charsets.UTF_8), columns.map { it.name })
                }
                val db = SQLiteDatabase.openOrCreateDatabase(dst, null)
                try {
                    val colDefs = columns.joinToString(", ") { "\"${it.name.replace("\"", "\"\"")}\" ${it.sqliteType}" }
                    db.execSQL("CREATE TABLE \"$cleanTable\" ($colDefs);")
                    val ph = columns.joinToString(", ") { "?" }
                    val stmt = db.compileStatement("INSERT INTO \"$cleanTable\" VALUES ($ph);")
                    db.beginTransaction()
                    try {
                        for (row in rows) {
                            stmt.clearBindings()
                            for (i in columns.indices) {
                                val v = row.getOrNull(i)
                                if (v == null) {
                                    stmt.bindNull(i + 1)
                                } else {
                                    when (columns[i].sqliteType) {
                                        "INTEGER" -> v.toLongOrNull()?.let { stmt.bindLong(i + 1, it) } ?: stmt.bindString(i + 1, v)
                                        "REAL" -> v.toDoubleOrNull()?.let { stmt.bindDouble(i + 1, it) } ?: stmt.bindString(i + 1, v)
                                        else -> stmt.bindString(i + 1, v)
                                    }
                                }
                            }
                            stmt.executeInsert()
                        }
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                        stmt.close()
                    }
                } finally {
                    db.close()
                }
            }
        }

        val schema = inspectDatabaseSchema(id, dst)
        val totalRows = schema.tables.values.sumOf { it.rowCount }
        Triple(dst, schema, totalRows)
    }

    fun exportResultToCsv(columns: List<String>, rows: List<List<String?>>): String {
        val sb = StringBuilder()
        sb.append(columns.joinToString(",") { escapeCsv(it) }).append("\n")
        for (row in rows) {
            sb.append(row.joinToString(",") { escapeCsv(it ?: "") }).append("\n")
        }
        return sb.toString()
    }

    fun exportResultToJson(columns: List<String>, rows: List<List<String?>>): String {
        val arr = JsonArray(rows.map { r ->
            val obj = linkedMapOf<String, kotlinx.serialization.json.JsonElement>()
            columns.forEachIndexed { i, col ->
                val v = r.getOrNull(i)
                obj[col] = if (v == null) JsonNull else JsonPrimitive(v)
            }
            JsonObject(obj)
        })
        return prettyJson.encodeToString(JsonArray.serializer(), arr)
    }

    private fun escapeCsv(s: String): String {
        return if (s.contains(',') || s.contains('"') || s.contains('\n')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s
    }

    private fun parseCsvPreview(fileName: String, bytes: ByteArray, delim: Char): ImportPreviewState {
        val raw = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val all = parseCsvAllRows(raw, delim)
        require(all.isNotEmpty()) { "CSV 文件为空" }
        val headers = all.first().mapIndexed { i, h -> h?.trim()?.ifBlank { "col_${i + 1}" } ?: "col_${i + 1}" }
        val dataRows = all.drop(1)
        val specs = headers.mapIndexed { idx, name ->
            val sampleVals = dataRows.take(50).mapNotNull { it.getOrNull(idx)?.trim() }.filter { it.isNotEmpty() }
            val inferred = when {
                sampleVals.isNotEmpty() && sampleVals.all { it.toLongOrNull() != null } -> "INTEGER"
                sampleVals.isNotEmpty() && sampleVals.all { it.toDoubleOrNull() != null } -> "REAL"
                else -> "TEXT"
            }
            ImportColumnSpec(name = name, sqliteType = inferred)
        }
        return ImportPreviewState(
            fileName = fileName,
            sourceType = "csv",
            columns = specs,
            sampleRows = dataRows.take(10),
            totalRowCount = dataRows.size,
            rawBytes = bytes
        )
    }

    private fun parseJsonPreview(fileName: String, bytes: ByteArray): ImportPreviewState {
        val raw = bytes.toString(Charsets.UTF_8)
        val elem = Json.parseToJsonElement(raw)
        require(elem is JsonArray && elem.isNotEmpty()) { "JSON 文件必须为非空对象数组 [...]" }
        val keys = linkedSetOf<String>()
        elem.forEach { item ->
            if (item is JsonObject) keys.addAll(item.keys)
        }
        val keyList = keys.toList()
        val rows = parseJsonAllRows(raw, keyList)
        val specs = keyList.mapIndexed { idx, k ->
            val samples = rows.take(50).mapNotNull { it.getOrNull(idx) }
            val inferred = when {
                samples.isNotEmpty() && samples.all { it.toLongOrNull() != null } -> "INTEGER"
                samples.isNotEmpty() && samples.all { it.toDoubleOrNull() != null } -> "REAL"
                else -> "TEXT"
            }
            ImportColumnSpec(name = k, sqliteType = inferred)
        }
        return ImportPreviewState(
            fileName = fileName,
            sourceType = "json",
            columns = specs,
            sampleRows = rows.take(10),
            totalRowCount = rows.size,
            rawBytes = bytes
        )
    }

    private fun parseCsvAllRows(text: String, delim: Char): List<List<String?>> {
        val rows = mutableListOf<List<String?>>()
        var curRow = mutableListOf<String?>()
        val curCell = StringBuilder()
        var inQuotes = false
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && text[i + 1] == '"') {
                        curCell.append('"')
                        i += 2
                        continue
                    }
                    inQuotes = false
                } else {
                    curCell.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    delim -> {
                        curRow.add(curCell.toString())
                        curCell.setLength(0)
                    }
                    '\r' -> {}
                    '\n' -> {
                        curRow.add(curCell.toString())
                        curCell.setLength(0)
                        if (curRow.any { !it.isNullOrBlank() }) rows.add(curRow)
                        curRow = mutableListOf()
                    }
                    else -> curCell.append(c)
                }
            }
            i++
        }
        if (curCell.isNotEmpty() || curRow.isNotEmpty()) {
            curRow.add(curCell.toString())
            if (curRow.any { !it.isNullOrBlank() }) rows.add(curRow)
        }
        return rows
    }

    private fun parseJsonAllRows(text: String, columns: List<String>): List<List<String?>> {
        val arr = Json.parseToJsonElement(text) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            columns.map { col ->
                val v = obj[col]
                when (v) {
                    null, is JsonNull -> null
                    is JsonPrimitive -> v.content
                    else -> v.toString()
                }
            }
        }
    }
}
