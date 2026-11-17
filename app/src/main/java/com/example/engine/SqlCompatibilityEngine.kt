package com.example.engine

/**
 * Translates common MySQL 8.4 teaching expressions into SQLite-compatible SQL
 * while strictly preserving quoted literals and comments, blocking unsafe system
 * commands, and checking structural/AST requirements on unquoted SQL code.
 */
object SqlCompatibilityEngine {

    data class TranslationResult(
        val allowed: Boolean,
        val sqliteSql: String,
        val compatibilityStatus: String,
        val notes: List<String> = emptyList(),
        val blockReason: String? = null
    )

    private val UNSAFE_SYSTEM_REGEX = Regex(
        """\b(ATTACH\s+DATABASE|DETACH\s+DATABASE|PRAGMA|VACUUM|LOAD_EXTENSION)\b""",
        RegexOption.IGNORE_CASE
    )

    private val WRITE_VERB_REGEX = Regex(
        """^\s*(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|REPLACE|TRUNCATE)\b""",
        RegexOption.IGNORE_CASE
    )

    fun translateForSqlite(rawSql: String, allowWriteStatements: Boolean): TranslationResult {
        val trimmed = rawSql.trim()
        if (trimmed.isEmpty()) {
            return TranslationResult(
                allowed = false,
                sqliteSql = "",
                compatibilityStatus = "无法执行",
                blockReason = "SQL 语句不能为空"
            )
        }

        val masked = maskLiteralsAndComments(trimmed)
        val unsafeMatch = UNSAFE_SYSTEM_REGEX.find(masked)
        if (unsafeMatch != null) {
            return TranslationResult(
                allowed = false,
                sqliteSql = trimmed,
                compatibilityStatus = "安全拦截",
                blockReason = "沙盒禁止执行系统级命令：${unsafeMatch.value.uppercase()}"
            )
        }

        val statements = splitTopLevelStatements(trimmed)
        if (!allowWriteStatements) {
            for (stmt in statements) {
                val cleanStmt = maskLiteralsAndComments(stmt).trim()
                if (cleanStmt.isNotEmpty()) {
                    val m = WRITE_VERB_REGEX.find(cleanStmt)
                    if (m != null) {
                        return TranslationResult(
                            allowed = false,
                            sqliteSql = trimmed,
                            compatibilityStatus = "只读保护",
                            blockReason = "当前查询环境为只读模式，禁止执行 ${m.groupValues[1].uppercase()} 写操作。"
                        )
                    }
                }
            }
        }

        // Token-safe rewriting outside string literals/comments
        val notes = mutableListOf<String>()
        var rewritten = rewriteOutsideLiterals(trimmed, notes)
        val status = if (notes.isEmpty()) "直接兼容" else "转换执行"
        return TranslationResult(
            allowed = true,
            sqliteSql = rewritten,
            compatibilityStatus = status,
            notes = notes.distinct()
        )
    }

    fun checkRequiredAstFeatures(rawSql: String, requiredFeatures: List<String>): String? {
        if (requiredFeatures.isEmpty()) return null
        val codeOnly = maskLiteralsAndComments(rawSql).uppercase()
        val missing = mutableListOf<String>()
        for (feat in requiredFeatures) {
            val f = feat.trim().uppercase()
            val ok = when (f) {
                "CTE" -> Regex("""\bWITH\s+[A-Z_][A-Z0-9_`"]*\s*(\([^)]*\))?\s*AS\s*\(""").containsMatchIn(codeOnly)
                "RECURSIVE_CTE" -> Regex("""\bWITH\s+RECURSIVE\b""").containsMatchIn(codeOnly)
                "WINDOW" -> Regex("""\bOVER\s*\(""").containsMatchIn(codeOnly)
                "PARTITION BY" -> Regex("""\bPARTITION\s+BY\b""").containsMatchIn(codeOnly)
                "ROW_NUMBER" -> Regex("""\bROW_NUMBER\s*\(""").containsMatchIn(codeOnly)
                "RANK" -> Regex("""\bRANK\s*\(""").containsMatchIn(codeOnly)
                "DENSE_RANK" -> Regex("""\bDENSE_RANK\s*\(""").containsMatchIn(codeOnly)
                "LAG" -> Regex("""\bLAG\s*\(""").containsMatchIn(codeOnly)
                "LEAD" -> Regex("""\bLEAD\s*\(""").containsMatchIn(codeOnly)
                "GROUP BY" -> Regex("""\bGROUP\s+BY\b""").containsMatchIn(codeOnly)
                "HAVING" -> Regex("""\bHAVING\b""").containsMatchIn(codeOnly)
                "JOIN" -> Regex("""\bJOIN\b""").containsMatchIn(codeOnly)
                "LEFT JOIN" -> Regex("""\bLEFT\s+(OUTER\s+)?JOIN\b""").containsMatchIn(codeOnly)
                "CASE" -> Regex("""\bCASE\b""").containsMatchIn(codeOnly)
                "SUBQUERY" -> Regex("""\(\s*SELECT\b""").containsMatchIn(codeOnly)
                "EXISTS" -> Regex("""\bEXISTS\s*\(""").containsMatchIn(codeOnly)
                "UNION" -> Regex("""\bUNION(\s+ALL)?\b""").containsMatchIn(codeOnly)
                "ORDER BY" -> Regex("""\bORDER\s+BY\b""").containsMatchIn(codeOnly)
                "DISTINCT" -> Regex("""\bDISTINCT\b""").containsMatchIn(codeOnly)
                "COALESCE" -> Regex("""\b(COALESCE|IFNULL)\s*\(""").containsMatchIn(codeOnly)
                else -> codeOnly.contains(f)
            }
            if (!ok) missing.add(feat)
        }
        return if (missing.isEmpty()) null
        else "本题为专项语法验证题，当前语句缺少必需的 SQL 结构：${missing.joinToString("、")}。"
    }

    /**
     * Replaces string literals and SQL comments with equal-length spaces so keyword & AST regexes
     * never produce false positives or corrupt literals.
     */
    fun maskLiteralsAndComments(sql: String): String {
        val sb = StringBuilder(sql.length)
        var i = 0
        val n = sql.length
        while (i < n) {
            val ch = sql[i]
            when {
                ch == '\'' -> {
                    sb.append('\'')
                    i++
                    while (i < n) {
                        val c = sql[i]
                        sb.append(' ')
                        if (c == '\'' && i + 1 < n && sql[i + 1] == '\'') {
                            sb.append(' ')
                            i += 2
                        } else if (c == '\\' && i + 1 < n) {
                            sb.append(' ')
                            i += 2
                        } else if (c == '\'') {
                            i++
                            break
                        } else {
                            i++
                        }
                    }
                }
                ch == '-' && i + 1 < n && sql[i + 1] == '-' -> {
                    while (i < n && sql[i] != '\n') {
                        sb.append(' ')
                        i++
                    }
                }
                ch == '/' && i + 1 < n && sql[i + 1] == '*' -> {
                    sb.append("  ")
                    i += 2
                    while (i < n) {
                        if (sql[i] == '*' && i + 1 < n && sql[i + 1] == '/') {
                            sb.append("  ")
                            i += 2
                            break
                        }
                        sb.append(' ')
                        i++
                    }
                }
                else -> {
                    sb.append(ch)
                    i++
                }
            }
        }
        return sb.toString()
    }

    fun splitTopLevelStatements(sql: String): List<String> {
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var i = 0
        val n = sql.length
        while (i < n) {
            val ch = sql[i]
            when {
                ch == '\'' || ch == '"' || ch == '`' -> {
                    val quote = ch
                    cur.append(ch)
                    i++
                    while (i < n) {
                        val c = sql[i]
                        cur.append(c)
                        if (c == '\\' && i + 1 < n && quote == '\'') {
                            cur.append(sql[i + 1])
                            i += 2
                            continue
                        }
                        if (c == quote) {
                            if (i + 1 < n && sql[i + 1] == quote) {
                                cur.append(sql[i + 1])
                                i += 2
                                continue
                            }
                            i++
                            break
                        }
                        i++
                    }
                }
                ch == '-' && i + 1 < n && sql[i + 1] == '-' -> {
                    while (i < n && sql[i] != '\n') {
                        cur.append(sql[i])
                        i++
                    }
                }
                ch == '/' && i + 1 < n && sql[i + 1] == '*' -> {
                    cur.append("/*")
                    i += 2
                    while (i < n) {
                        if (sql[i] == '*' && i + 1 < n && sql[i + 1] == '/') {
                            cur.append("*/")
                            i += 2
                            break
                        }
                        cur.append(sql[i])
                        i++
                    }
                }
                ch == ';' -> {
                    val s = cur.toString().trim()
                    if (s.isNotEmpty()) result.add(s)
                    cur.setLength(0)
                    i++
                }
                else -> {
                    cur.append(ch)
                    i++
                }
            }
        }
        val tail = cur.toString().trim()
        if (tail.isNotEmpty()) result.add(tail)
        return result
    }

    private fun rewriteOutsideLiterals(sql: String, notes: MutableList<String>): String {
        // Segment text into literal/comment segments vs unquoted SQL segments
        val segments = mutableListOf<Pair<Boolean, String>>() // true = literal/comment
        var i = 0
        val n = sql.length
        val buf = StringBuilder()
        while (i < n) {
            val ch = sql[i]
            if (ch == '\'' || (ch == '-' && i + 1 < n && sql[i + 1] == '-') || (ch == '/' && i + 1 < n && sql[i + 1] == '*')) {
                if (buf.isNotEmpty()) {
                    segments.add(false to buf.toString())
                    buf.setLength(0)
                }
                val lit = StringBuilder()
                when (ch) {
                    '\'' -> {
                        lit.append(ch)
                        i++
                        while (i < n) {
                            val c = sql[i]
                            lit.append(c)
                            if (c == '\'' && i + 1 < n && sql[i + 1] == '\'') {
                                lit.append(sql[i + 1])
                                i += 2
                            } else if (c == '\\' && i + 1 < n) {
                                lit.append(sql[i + 1])
                                i += 2
                            } else if (c == '\'') {
                                i++
                                break
                            } else {
                                i++
                            }
                        }
                    }
                    '-' -> {
                        while (i < n && sql[i] != '\n') {
                            lit.append(sql[i])
                            i++
                        }
                    }
                    '/' -> {
                        lit.append("/*")
                        i += 2
                        while (i < n) {
                            if (sql[i] == '*' && i + 1 < n && sql[i + 1] == '/') {
                                lit.append("*/")
                                i += 2
                                break
                            }
                            lit.append(sql[i])
                            i++
                        }
                    }
                }
                segments.add(true to lit.toString())
            } else {
                buf.append(ch)
                i++
            }
        }
        if (buf.isNotEmpty()) segments.add(false to buf.toString())

        // Apply simple token replacements on non-literal segments first,
        // then function calls over the merged string while respecting literals
        val simpleRewritten = segments.joinToString("") { (isLit, text) ->
            if (isLit) text else applySimpleTokenRewrites(text, notes)
        }
        return rewriteFunctionsAware(simpleRewritten, notes)
    }

    private fun applySimpleTokenRewrites(text: String, notes: MutableList<String>): String {
        var out = text
        val backtickRegex = Regex("""`([^`]+)`""")
        if (backtickRegex.containsMatchIn(out)) {
            out = backtickRegex.replace(out, "\"$1\"")
            notes.add("反引号标识符已转为标准双引号")
        }

        val charLenRegex = Regex("""\bCHAR_LENGTH\s*\(""", RegexOption.IGNORE_CASE)
        if (charLenRegex.containsMatchIn(out)) {
            out = charLenRegex.replace(out, "LENGTH(")
            notes.add("CHAR_LENGTH() -> LENGTH()")
        }

        val nvlRegex = Regex("""\bNVL\s*\(""", RegexOption.IGNORE_CASE)
        if (nvlRegex.containsMatchIn(out)) {
            out = nvlRegex.replace(out, "IFNULL(")
            notes.add("NVL() -> IFNULL()")
        }

        val nowRegex = Regex("""\bNOW\s*\(\s*\)""", RegexOption.IGNORE_CASE)
        if (nowRegex.containsMatchIn(out)) {
            out = nowRegex.replace(out, "datetime('now')")
            notes.add("NOW() -> datetime('now')")
        }

        val curDateRegex = Regex("""\bCURDATE\s*\(\s*\)""", RegexOption.IGNORE_CASE)
        if (curDateRegex.containsMatchIn(out)) {
            out = curDateRegex.replace(out, "date('now')")
            notes.add("CURDATE() -> date('now')")
        }

        val limitOffsetCommaRegex = Regex("""\bLIMIT\s+(\d+)\s*,\s*(\d+)\b""", RegexOption.IGNORE_CASE)
        if (limitOffsetCommaRegex.containsMatchIn(out)) {
            out = limitOffsetCommaRegex.replace(out) { m ->
                "LIMIT ${m.groupValues[2]} OFFSET ${m.groupValues[1]}"
            }
            notes.add("LIMIT offset, count -> LIMIT count OFFSET offset")
        }

        return out
    }

    private fun rewriteFunctionsAware(sql: String, notes: MutableList<String>): String {
        var current = sql
        // Apply multi-pass so nested MySQL functions rewrite cleanly
        repeat(4) {
            val before = current
            current = rewriteSinglePassFunction(current, "CONCAT", notes) { args ->
                if (args.size == 1) args[0]
                else {
                    val nullChecks = args.joinToString(" OR ") { "(${it.trim()}) IS NULL" }
                    val joined = args.joinToString(" || ") { "(${it.trim()})" }
                    "CASE WHEN ($nullChecks) THEN NULL ELSE ($joined) END"
                }
            }
            current = rewriteSinglePassFunction(current, "CONCAT_WS", notes) { args ->
                if (args.size <= 1) "''"
                else {
                    val sep = args[0].trim()
                    val rest = args.drop(1).map { "NULLIF(${it.trim()}, NULL)" }
                    val joined = rest.joinToString(" || $sep || ") { "COALESCE(${it}, '')" }
                    joined
                }
            }
            current = rewriteSinglePassFunction(current, "YEAR", notes) { args ->
                "CAST(strftime('%Y', ${args[0].trim()}) AS INTEGER)"
            }
            current = rewriteSinglePassFunction(current, "MONTH", notes) { args ->
                "CAST(strftime('%m', ${args[0].trim()}) AS INTEGER)"
            }
            current = rewriteSinglePassFunction(current, "DAY", notes) { args ->
                "CAST(strftime('%d', ${args[0].trim()}) AS INTEGER)"
            }
            current = rewriteSinglePassFunction(current, "IF", notes) { args ->
                if (args.size >= 3) {
                    "CASE WHEN (${args[0].trim()}) THEN (${args[1].trim()}) ELSE (${args[2].trim()}) END"
                } else {
                    "IF(${args.joinToString(", ")})"
                }
            }
            current = rewriteSinglePassFunction(current, "DATEDIFF", notes) { args ->
                if (args.size >= 2) {
                    "CAST(julianday(${args[0].trim()}) - julianday(${args[1].trim()}) AS INTEGER)"
                } else {
                    "DATEDIFF(${args.joinToString(", ")})"
                }
            }
            current = rewriteSinglePassFunction(current, "DATE_FORMAT", notes) { args ->
                if (args.size >= 2) {
                    "strftime(${args[1].trim()}, ${args[0].trim()})"
                } else {
                    "DATE_FORMAT(${args.joinToString(", ")})"
                }
            }
            current = rewriteSinglePassFunction(current, "LEFT", notes) { args ->
                if (args.size >= 2) "SUBSTR(${args[0].trim()}, 1, ${args[1].trim()})" else "LEFT(${args.joinToString(", ")})"
            }
            if (current == before) return@repeat
        }
        return current
    }

    private fun rewriteSinglePassFunction(
        sql: String,
        funcName: String,
        notes: MutableList<String>,
        builder: (List<String>) -> String
    ): String {
        val sb = StringBuilder()
        var i = 0
        val n = sql.length
        while (i < n) {
            val ch = sql[i]
            if (ch == '\'' || ch == '"') {
                val q = ch
                sb.append(q)
                i++
                while (i < n) {
                    val c = sql[i]
                    sb.append(c)
                    if (c == '\\' && i + 1 < n && q == '\'') {
                        sb.append(sql[i + 1])
                        i += 2
                        continue
                    }
                    if (c == q) {
                        if (i + 1 < n && sql[i + 1] == q) {
                            sb.append(sql[i + 1])
                            i += 2
                            continue
                        }
                        i++
                        break
                    }
                    i++
                }
                continue
            }
            if (ch == '-' && i + 1 < n && sql[i + 1] == '-') {
                while (i < n && sql[i] != '\n') {
                    sb.append(sql[i])
                    i++
                }
                continue
            }
            if (ch == '/' && i + 1 < n && sql[i + 1] == '*') {
                sb.append("/*")
                i += 2
                while (i < n) {
                    if (sql[i] == '*' && i + 1 < n && sql[i + 1] == '/') {
                        sb.append("*/")
                        i += 2
                        break
                    }
                    sb.append(sql[i])
                    i++
                }
                continue
            }

            if (i + funcName.length < n &&
                sql.regionMatches(i, funcName, 0, funcName.length, ignoreCase = true) &&
                (i == 0 || (!sql[i - 1].isLetterOrDigit() && sql[i - 1] != '_'))
            ) {
                var j = i + funcName.length
                while (j < n && sql[j].isWhitespace()) j++
                if (j < n && sql[j] == '(') {
                    val argsRange = extractBalancedArgs(sql, j)
                    if (argsRange != null) {
                        val (closeIdx, args) = argsRange
                        // Recursively check inner args for nested function name too
                        val replacement = builder(args)
                        notes.add("$funcName() 已自动兼容转换")
                        sb.append(replacement)
                        i = closeIdx + 1
                        continue
                    }
                }
            }
            sb.append(ch)
            i++
        }
        return sb.toString()
    }

    private fun extractBalancedArgs(sql: String, openParenIdx: Int): Pair<Int, List<String>>? {
        val args = mutableListOf<String>()
        val cur = StringBuilder()
        var depth = 0
        var i = openParenIdx
        val n = sql.length
        while (i < n) {
            val ch = sql[i]
            if (ch == '\'' || ch == '"') {
                val q = ch
                if (depth >= 1) cur.append(ch)
                i++
                while (i < n) {
                    val c = sql[i]
                    if (depth >= 1) cur.append(c)
                    if (c == '\\' && i + 1 < n && q == '\'') {
                        if (depth >= 1) cur.append(sql[i + 1])
                        i += 2
                        continue
                    }
                    if (c == q) {
                        if (i + 1 < n && sql[i + 1] == q) {
                            if (depth >= 1) cur.append(sql[i + 1])
                            i += 2
                            continue
                        }
                        i++
                        break
                    }
                    i++
                }
                continue
            }
            when (ch) {
                '(' -> {
                    if (depth >= 1) cur.append(ch)
                    depth++
                }
                ')' -> {
                    depth--
                    if (depth == 0) {
                        val lastArg = cur.toString().trim()
                        if (lastArg.isNotEmpty() || args.isNotEmpty()) {
                            args.add(lastArg)
                        }
                        return i to args
                    } else {
                        cur.append(ch)
                    }
                }
                ',' -> {
                    if (depth == 1) {
                        args.add(cur.toString().trim())
                        cur.setLength(0)
                    } else {
                        cur.append(ch)
                    }
                }
                else -> if (depth >= 1) cur.append(ch)
            }
            i++
        }
        return null
    }
}
