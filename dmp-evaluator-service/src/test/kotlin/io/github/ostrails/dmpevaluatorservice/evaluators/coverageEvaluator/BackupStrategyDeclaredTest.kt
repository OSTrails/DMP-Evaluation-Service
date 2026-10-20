package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BackupStrategyDeclaredTest {

    private fun evaluate(host: String?) =
        run("backupStrategyDeclared", dmp("""{"title": "a", "distribution": [{"title": "d"${if (host != null) """, "host": $host""" else ""}}]}""")).result

    @Test
    fun `host with backup type and frequency passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "University storage", "backup_type": "tape", "backup_frequency": "daily"}"""))
    }

    @Test
    fun `host without backup type fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "University storage", "backup_frequency": "daily"}"""))
    }

    @Test
    fun `host without backup frequency fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "University storage", "backup_type": "tape"}"""))
    }

    @Test
    fun `distribution without host fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(null))
    }

    @Test
    fun `dmp without datasets fails`() {
        assertEquals(ResultTestEnum.FAIL, run("backupStrategyDeclared", dmp()).result)
    }
}
