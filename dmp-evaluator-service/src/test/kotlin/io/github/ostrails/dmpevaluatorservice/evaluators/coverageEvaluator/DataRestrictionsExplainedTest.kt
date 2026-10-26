package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DataRestrictionsExplainedTest {

    private fun evaluate(vararg datasets: String) = run("dataRestrictionsExplained", dmp(*datasets)).result

    private val license = """"license": [{"license_ref": "https://creativecommons.org/licenses/by/4.0/", "start_date": "2026-01-01"}]"""

    @Test
    fun `access mode and license passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "distribution": [{"title": "d", "data_access": "closed", $license}]}"""))
    }

    @Test
    fun `missing license fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "distribution": [{"title": "d", "data_access": "open"}]}"""))
    }

    @Test
    fun `missing access mode fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "distribution": [{"title": "d", $license}]}"""))
    }

    @Test
    fun `no datasets fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate())
    }
}
