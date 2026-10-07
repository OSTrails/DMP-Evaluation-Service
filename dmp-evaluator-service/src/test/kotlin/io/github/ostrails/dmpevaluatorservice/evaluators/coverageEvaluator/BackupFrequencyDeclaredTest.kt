package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BackupFrequencyDeclaredTest {

    private fun evaluate(host: String?) =
        run("backupFrequencyDeclared", dmp("""{"title": "a", "distribution": [{"title": "d"${if (host != null) """, "host": $host""" else ""}}]}""")).result

    @Test
    fun `host with backup frequency passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "University storage", "backup_frequency": "daily"}"""))
    }

    @Test
    fun `host without backup frequency fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "University storage"}"""))
    }

    @Test
    fun `distribution without host fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(null))
    }
}
