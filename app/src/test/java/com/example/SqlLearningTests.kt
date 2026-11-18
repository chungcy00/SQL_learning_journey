package com.example

import com.example.data.repository.ContentRepository
import com.example.engine.FsrsEngine
import com.example.engine.SqlCompatibilityEngine
import com.example.engine.SqlExecutionEngine
import com.example.model.FlashcardRating
import com.example.model.NumericTolerance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

class SqlEngineUnitTests {

    @Test
    fun `fsrs schedule intervals increase monotonically across ratings`() {
        val now = 1_700_000_000_000L
        val card = FsrsEngine.newCardState("fc0001", now)
        val again = FsrsEngine.schedule(card, FlashcardRating.AGAIN, 0.90, now)
        val hard = FsrsEngine.schedule(card, FlashcardRating.HARD, 0.90, now)
        val good = FsrsEngine.schedule(card, FlashcardRating.GOOD, 0.90, now)
        val easy = FsrsEngine.schedule(card, FlashcardRating.EASY, 0.90, now)

        assertTrue(again.dueAtMs < hard.dueAtMs)
        assertTrue(hard.dueAtMs < good.dueAtMs)
        assertTrue(good.dueAtMs <= easy.dueAtMs)
        assertEquals(1, again.state)
        assertEquals(2, good.state)

        // Reviewing again after 4 days grows stability further; AGAIN causes lapse -> state 3
        val later = now + 4L * 86_400_000L
        val nextGood = FsrsEngine.schedule(good, FlashcardRating.GOOD, 0.90, later)
        assertTrue(nextGood.stability > good.stability)
        val lapsed = FsrsEngine.schedule(good, FlashcardRating.AGAIN, 0.90, later)
        assertEquals(3, lapsed.state)
        assertEquals(1, lapsed.lapses)
    }

    @Test
    fun `sql compatibility translates mysql functions and ignores string literals`() {
        val raw = "SELECT CONCAT(first_name, 'CONCAT(a,b)') AS full_name, CHAR_LENGTH(name) AS len, YEAR(created_at) AS yr FROM users;"
        val res = SqlCompatibilityEngine.translateForSqlite(raw, allowWriteStatements = false)
        assertTrue(res.allowed)
        assertEquals("转换执行", res.compatibilityStatus)
        assertTrue(res.sqliteSql.contains("'CONCAT(a,b)'"))
        assertTrue(res.sqliteSql.contains("CASE WHEN"))
        assertTrue(res.sqliteSql.contains("LENGTH(name)"))
        assertTrue(res.sqliteSql.contains("strftime('%Y', created_at)"))
    }

    @Test
    fun `sql compatibility blocks unsafe system commands and checks ast features`() {
        val unsafe = SqlCompatibilityEngine.translateForSqlite("ATTACH DATABASE 'x.db' AS x;", allowWriteStatements = false)
        assertFalse(unsafe.allowed)

        val noGroupBy = SqlCompatibilityEngine.checkRequiredAstFeatures(
            "SELECT 'GROUP BY' AS fake FROM orders;",
            listOf("GROUP BY")
        )
        assertNotNull(noGroupBy)

        val validCteWindow = SqlCompatibilityEngine.checkRequiredAstFeatures(
            "WITH cte AS (SELECT id, ROW_NUMBER() OVER (PARTITION BY dept ORDER BY sal DESC) AS rn FROM emp) SELECT * FROM cte;",
            listOf("CTE", "WINDOW", "ROW_NUMBER", "PARTITION BY")
        )
        assertNull(validCteWindow)
    }

    @Test
    fun `compareResults validates multiset rows nulls and numeric tolerance`() {
        // Preserves duplicates cleanly
        val dupMismatch = SqlExecutionEngine.compareResults(
            actualCols = listOf("dept", "cnt"),
            actualRows = listOf(listOf("A", "1"), listOf("A", "1")),
            expectedCols = listOf("dept", "cnt"),
            expectedRows = listOf(listOf("A", "1"), listOf("B", "1")),
            orderPolicy = "unordered",
            columnPolicy = "exact_names_in_order",
            tolerance = NumericTolerance()
        )
        assertFalse(dupMismatch.first)

        // NULL never matches "0" or ""
        val nullMismatch = SqlExecutionEngine.compareResults(
            actualCols = listOf("val"),
            actualRows = listOf(listOf(null)),
            expectedCols = listOf("val"),
            expectedRows = listOf(listOf("0")),
            orderPolicy = "ordered",
            columnPolicy = "exact_names_in_order",
            tolerance = NumericTolerance()
        )
        assertFalse(nullMismatch.first)

        // Unordered multiset within tolerance passes
        val ok = SqlExecutionEngine.compareResults(
            actualCols = listOf("amt", "name"),
            actualRows = listOf(listOf("12.344", "Bob"), listOf("10.00", "Alice")),
            expectedCols = listOf("name", "amt"),
            expectedRows = listOf(listOf("Alice", "10"), listOf("Bob", "12.34")),
            orderPolicy = "unordered",
            columnPolicy = "exact_names_any_order",
            tolerance = NumericTolerance(absolute = 0.01)
        )
        assertTrue(ok.first)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ContentAndOfflineRobolectricTest {

    @Test
    fun `content bundle loads exact canonical counts and passes offline sha256 self check`() = runBlocking {
        val app = RuntimeEnvironment.getApplication()
        assertEquals("SQL 学习", app.getString(R.string.app_name))

        val repo = ContentRepository(app)
        repo.ensureLoaded()

        assertEquals(20, repo.chapters.size)
        assertEquals(152, repo.allKnowledgePoints.size)
        assertEquals(300, repo.allQuestions.size)
        assertEquals(200, repo.allFlashcards.size)
        assertEquals(6, repo.businessProjects.size)
        assertEquals(7, repo.schemas.size)

        val report = repo.runRealOfflineSelfCheck()
        assertTrue("Expected offline ready, errors: ${report.missingOrCorruptFiles}", report.isReady)
        assertTrue(report.sqliteSmokePassed)
        assertTrue("Expected >= 500 checked bundle files, got ${report.totalFilesChecked}", report.totalFilesChecked >= 500)

        // Verify query on extracted enterprise retail database
        val retailDb = repo.ensureDatabaseExtracted("retail")
        assertNotNull(retailDb)
        val execRes = SqlExecutionEngine.runOnDatabaseFile(
            dbFile = retailDb!!,
            rawSql = "SELECT COUNT(*) AS cnt FROM sqlite_master WHERE type = 'table';",
            readOnly = true
        )
        assertTrue(execRes.success)
        assertTrue((execRes.rows.firstOrNull()?.firstOrNull()?.toIntOrNull() ?: 0) > 0)
    }

    @Test
    fun `main activity launches without crashing`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}
