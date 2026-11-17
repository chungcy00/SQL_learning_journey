package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.KeyboardMode
import com.example.ui.theme.CloudSeaBlue
import com.example.ui.theme.JetBrainsMonoFont
import com.example.ui.theme.PrimaryAction
import com.example.ui.theme.SqlSyntaxColors

data class FieldGroup(
    val tableName: String,
    val columns: List<String>
)

class SqlEditorController(
    initialText: String = "",
    var autoPair: Boolean = true,
    var autoUppercase: Boolean = false,
    var autoIndent: Boolean = true
) {
    var value by mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
        private set

    private val undoStack = mutableStateListOf<TextFieldValue>()
    private val redoStack = mutableStateListOf<TextFieldValue>()

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun onValueChange(next: TextFieldValue) {
        if (next.text == value.text) {
            value = next
            return
        }
        undoStack.add(value)
        if (undoStack.size > 60) undoStack.removeAt(0)
        redoStack.clear()

        val oldText = value.text
        var adjusted = next

        if (autoIndent && next.text.length == oldText.length + 1 && next.selection.collapsed) {
            val pos = next.selection.start
            if (pos > 0 && next.text[pos - 1] == '\n') {
                val prevLineStart = next.text.lastIndexOf('\n', pos - 2).let { if (it < 0) 0 else it + 1 }
                val prevLine = next.text.substring(prevLineStart, pos - 1)
                val indent = prevLine.takeWhile { it == ' ' || it == '\t' }
                if (indent.isNotEmpty()) {
                    val inserted = next.text.substring(0, pos) + indent + next.text.substring(pos)
                    adjusted = TextFieldValue(inserted, TextRange(pos + indent.length))
                }
            } else if (autoPair && pos > 0) {
                val typed = next.text[pos - 1]
                val pair = when (typed) {
                    '(' -> ')'
                    '\'' -> '\''
                    else -> null
                }
                if (pair != null && next.text.getOrNull(pos) != pair) {
                    val inserted = next.text.substring(0, pos) + pair + next.text.substring(pos)
                    adjusted = TextFieldValue(inserted, TextRange(pos))
                }
            }
        }
        value = adjusted
    }

    fun setExternalText(text: String) {
        if (text == value.text) return
        undoStack.add(value)
        redoStack.clear()
        value = TextFieldValue(text, TextRange(text.length))
    }

    fun insertToken(token: String, cursorBackOffset: Int = 0) {
        undoStack.add(value)
        redoStack.clear()
        val sel = value.selection
        val start = sel.min.coerceIn(0, value.text.length)
        val end = sel.max.coerceIn(0, value.text.length)
        val newText = value.text.substring(0, start) + token + value.text.substring(end)
        val newCursor = (start + token.length - cursorBackOffset).coerceIn(0, newText.length)
        value = TextFieldValue(newText, TextRange(newCursor))
    }

    fun insertSnippet(code: String) {
        insertToken(if (value.text.isBlank()) code else "\n$code")
    }

    fun backspace() {
        val sel = value.selection
        val start = sel.min.coerceIn(0, value.text.length)
        val end = sel.max.coerceIn(0, value.text.length)
        if (start == 0 && end == 0) return
        undoStack.add(value)
        redoStack.clear()
        if (start != end) {
            val newText = value.text.removeRange(start, end)
            value = TextFieldValue(newText, TextRange(start))
        } else {
            val newText = value.text.removeRange(start - 1, start)
            value = TextFieldValue(newText, TextRange(start - 1))
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(value)
        value = undoStack.removeAt(undoStack.lastIndex)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(value)
        value = redoStack.removeAt(redoStack.lastIndex)
    }

    fun clear() {
        if (value.text.isEmpty()) return
        undoStack.add(value)
        redoStack.clear()
        value = TextFieldValue("", TextRange(0))
    }
}

private val SQL_KEYWORDS = setOf(
    "SELECT", "FROM", "WHERE", "GROUP", "BY", "HAVING", "ORDER", "ASC", "DESC",
    "LIMIT", "OFFSET", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "CROSS", "ON",
    "AS", "DISTINCT", "AND", "OR", "NOT", "IN", "BETWEEN", "LIKE", "IS", "NULL",
    "CASE", "WHEN", "THEN", "ELSE", "END", "WITH", "RECURSIVE", "UNION", "ALL",
    "EXISTS", "OVER", "PARTITION", "ROWS", "RANGE", "UNBOUNDED", "PRECEDING",
    "FOLLOWING", "CURRENT", "ROW", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
    "DELETE", "CREATE", "TABLE", "VIEW", "INDEX", "DROP", "ALTER", "ADD", "EXPLAIN"
)

private val SQL_FUNCTIONS = setOf(
    "COUNT", "SUM", "AVG", "MIN", "MAX", "COALESCE", "IFNULL", "NULLIF",
    "ROW_NUMBER", "RANK", "DENSE_RANK", "LAG", "LEAD", "FIRST_VALUE", "LAST_VALUE",
    "ROUND", "ABS", "LENGTH", "SUBSTR", "UPPER", "LOWER", "TRIM", "STRFTIME", "CAST"
)

private class SqlSyntaxTransformation(private val isDark: Boolean) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val annotated = buildAnnotatedString {
            append(raw)
            val kwColor = if (isDark) SqlSyntaxColors.DarkKeyword else SqlSyntaxColors.LightKeyword
            val fnColor = if (isDark) SqlSyntaxColors.DarkFunction else SqlSyntaxColors.LightFunction
            val strColor = if (isDark) SqlSyntaxColors.DarkString else SqlSyntaxColors.LightString
            val numColor = if (isDark) SqlSyntaxColors.DarkNumber else SqlSyntaxColors.LightNumber
            val cmtColor = if (isDark) SqlSyntaxColors.DarkComment else SqlSyntaxColors.LightComment

            Regex("""--[^\n]*""").findAll(raw).forEach { m ->
                addStyle(SpanStyle(color = cmtColor), m.range.first, m.range.last + 1)
            }
            Regex("""'[^']*'""").findAll(raw).forEach { m ->
                addStyle(SpanStyle(color = strColor), m.range.first, m.range.last + 1)
            }
            Regex("""\b\d+(\.\d+)?\b""").findAll(raw).forEach { m ->
                addStyle(SpanStyle(color = numColor), m.range.first, m.range.last + 1)
            }
            Regex("""\b[A-Za-z_][A-Za-z0-9_]*\b""").findAll(raw).forEach { m ->
                val up = m.value.uppercase()
                when {
                    up in SQL_KEYWORDS -> addStyle(
                        SpanStyle(color = kwColor, fontWeight = FontWeight.Bold),
                        m.range.first,
                        m.range.last + 1
                    )
                    up in SQL_FUNCTIONS -> addStyle(
                        SpanStyle(color = fnColor, fontWeight = FontWeight.SemiBold),
                        m.range.first,
                        m.range.last + 1
                    )
                }
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }
}

@Composable
fun SqlCodeEditorBox(
    controller: SqlEditorController,
    isDarkEditor: Boolean,
    fontSizeSp: Int,
    useCustomKeyboard: Boolean,
    onToggleCustomKeyboard: () -> Unit,
    onToggleFullscreen: () -> Unit,
    minHeightDp: Int = 140
) {
    val bgColor = if (isDarkEditor) SqlSyntaxColors.DarkBg else SqlSyntaxColors.LightBg
    val fgColor = if (isDarkEditor) SqlSyntaxColors.DarkText else SqlSyntaxColors.LightText
    val lineCount = controller.value.text.count { it == '\n' } + 1

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Editor Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDarkEditor) SqlSyntaxColors.DarkBg else CloudSeaBlue)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SQL 编辑器 ($lineCount 行)",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryAction,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { controller.undo() },
                        enabled = controller.canUndo(),
                        modifier = Modifier.size(36.dp).testTag("editor_undo_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "撤销", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { controller.redo() },
                        enabled = controller.canRedo(),
                        modifier = Modifier.size(36.dp).testTag("editor_redo_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "重做", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { controller.clear() },
                        modifier = Modifier.size(36.dp).testTag("editor_clear_btn")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "清空", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onToggleCustomKeyboard,
                        modifier = Modifier.size(36.dp).testTag("editor_keyboard_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (useCustomKeyboard) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                            contentDescription = "切换 SQL 键盘",
                            tint = if (useCustomKeyboard) PrimaryAction else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier.size(36.dp).testTag("editor_fullscreen_btn")
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "全屏编辑", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Gutter + BasicTextField
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeightDp.dp, max = 300.dp)
                    .verticalScroll(scrollState)
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.width(26.dp).padding(end = 6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (ln in 1..lineCount) {
                        Text(
                            text = "$ln",
                            fontFamily = JetBrainsMonoFont,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp + 6).sp,
                            color = if (isDarkEditor) SqlSyntaxColors.DarkComment else SqlSyntaxColors.LightComment
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (controller.value.text.isEmpty()) {
                        Text(
                            text = "输入 SQL 语句，可点击下方专用键盘快速插入关键字与表字段…",
                            fontFamily = JetBrainsMonoFont,
                            fontSize = fontSizeSp.sp,
                            color = if (isDarkEditor) SqlSyntaxColors.DarkComment else SqlSyntaxColors.LightComment
                        )
                    }
                    BasicTextField(
                        value = controller.value,
                        onValueChange = { controller.onValueChange(it) },
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFont,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp + 6).sp,
                            color = fgColor
                        ),
                        cursorBrush = SolidColor(PrimaryAction),
                        visualTransformation = remember(isDarkEditor) { SqlSyntaxTransformation(isDarkEditor) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = (minHeightDp - 24).dp)
                            .testTag("sql_editor_input")
                    )
                }
            }
        }
    }
}

private val KEYBOARD_SYNTAX_TOKENS = listOf(
    "SELECT ", "FROM ", "WHERE ", "GROUP BY ", "HAVING ", "ORDER BY ",
    "LIMIT ", "JOIN ", "LEFT JOIN ", "ON ", "AS ", "DISTINCT ",
    "WITH ", "OVER (", "PARTITION BY ", "CASE WHEN ", "THEN ", "ELSE ", "END"
)

private val KEYBOARD_FUNC_TOKENS = listOf(
    "COUNT(*)", "SUM()", "AVG()", "MIN()", "MAX()", "COALESCE(, 0)",
    "ROW_NUMBER() OVER ()", "RANK() OVER ()", "DENSE_RANK() OVER ()",
    "ROUND(, 2)", "IFNULL(, 0)", "LENGTH()", "CAST( AS REAL)"
)

private val KEYBOARD_SYMBOL_TOKENS = listOf(
    " = ", " != ", " > ", " < ", " >= ", " <= ",
    "(", ")", ",", ";", "'", ".", "*", "+", "-", "/", " AND ", " OR ", " IS NULL"
)

@Composable
fun SqlCustomKeyboardPanel(
    controller: SqlEditorController,
    fieldGroups: List<FieldGroup>,
    initialMode: KeyboardMode,
    enableHaptic: Boolean,
    onHideKeyboard: () -> Unit
) {
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    var selectedTableIdx by remember(fieldGroups) { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current

    fun pressToken(token: String) {
        if (enableHaptic) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val back = when {
            token.endsWith("()") -> 1
            token == "COALESCE(, 0)" -> 4
            token == "ROUND(, 2)" -> 4
            token == "IFNULL(, 0)" -> 4
            else -> 0
        }
        controller.insertToken(token, cursorBackOffset = back)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().testTag("sql_custom_keyboard")
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            // Mode Switcher Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    KeyboardMode.entries.forEach { km ->
                        val active = mode == km
                        Surface(
                            onClick = { mode = km },
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) PrimaryAction else CloudSeaBlue,
                            modifier = Modifier.testTag("kb_mode_${km.id}")
                        ) {
                            Text(
                                text = km.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else PrimaryAction,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        onClick = { controller.backspace() },
                        shape = RoundedCornerShape(8.dp),
                        color = CloudSeaBlue,
                        modifier = Modifier.testTag("kb_backspace_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "退格",
                            tint = PrimaryAction,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).size(16.dp)
                        )
                    }
                    Surface(
                        onClick = onHideKeyboard,
                        shape = RoundedCornerShape(8.dp),
                        color = CloudSeaBlue,
                        modifier = Modifier.testTag("kb_hide_btn")
                    ) {
                        Icon(
                            Icons.Default.KeyboardHide,
                            contentDescription = "收起键盘",
                            tint = PrimaryAction,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (mode) {
                KeyboardMode.SYNTAX -> {
                    TokenStrip(KEYBOARD_SYNTAX_TOKENS, onTokenClick = ::pressToken)
                }
                KeyboardMode.FUNCTION -> {
                    TokenStrip(KEYBOARD_FUNC_TOKENS, onTokenClick = ::pressToken)
                }
                KeyboardMode.SYMBOL -> {
                    TokenStrip(KEYBOARD_SYMBOL_TOKENS, onTokenClick = ::pressToken)
                }
                KeyboardMode.FIELD -> {
                    if (fieldGroups.isEmpty()) {
                        Text(
                            text = "当前题目或工作区暂无表结构提示，可前往「数据表」查看",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            fieldGroups.forEachIndexed { idx, fg ->
                                val active = idx == selectedTableIdx
                                Surface(
                                    onClick = {
                                        selectedTableIdx = idx
                                        pressToken(fg.tableName + " ")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (active) PrimaryAction else CloudSeaBlue
                                ) {
                                    Text(
                                        text = fg.tableName,
                                        fontFamily = JetBrainsMonoFont,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else PrimaryAction,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val activeCols = fieldGroups.getOrNull(selectedTableIdx)?.columns.orEmpty()
                        TokenStrip(activeCols) { col -> pressToken(col) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenStrip(tokens: List<String>, onTokenClick: (String) -> Unit) {
    val rows = tokens.chunked((tokens.size + 1) / 2.coerceAtLeast(1))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { rowTokens ->
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowTokens.forEach { tok ->
                    Surface(
                        onClick = { onTokenClick(tok) },
                        shape = RoundedCornerShape(8.dp),
                        color = CloudSeaBlue,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = tok.trim(),
                            fontFamily = JetBrainsMonoFont,
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryAction,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenSqlEditorDialog(
    controller: SqlEditorController,
    isDarkEditor: Boolean,
    fontSizeSp: Int,
    useCustomKeyboard: Boolean,
    onToggleCustomKeyboard: () -> Unit,
    fieldGroups: List<FieldGroup>,
    initialMode: KeyboardMode,
    enableHaptic: Boolean,
    onDismiss: () -> Unit,
    actionBar: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            bottomBar = {
                if (useCustomKeyboard) {
                    SqlCustomKeyboardPanel(
                        controller = controller,
                        fieldGroups = fieldGroups,
                        initialMode = initialMode,
                        enableHaptic = enableHaptic,
                        onHideKeyboard = onToggleCustomKeyboard
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("全屏专注 SQL 编写", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "退出全屏")
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    SqlCodeEditorBox(
                        controller = controller,
                        isDarkEditor = isDarkEditor,
                        fontSizeSp = fontSizeSp,
                        useCustomKeyboard = useCustomKeyboard,
                        onToggleCustomKeyboard = onToggleCustomKeyboard,
                        onToggleFullscreen = onDismiss,
                        minHeightDp = 320
                    )
                }
                actionBar()
            }
        }
    }
}
