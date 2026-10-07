package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DistributionFormatSpecifiedTest {

    private fun evaluate(vararg distributions: String) =
        run("distributionFormatSpecified", dmp("""{"title": "a", "distribution": [${distributions.joinToString()}]}""")).result

    @Test
    fun `all distributions with format pass`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "d1", "format": ["text/csv"]}""", """{"title": "d2", "format": ["application/json"]}"""))
    }

    @Test
    fun `distribution without format fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "d1", "format": ["text/csv"]}""", """{"title": "d2"}"""))
    }

    @Test
    fun `empty or blank format list fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "d1", "format": []}"""))
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "d1", "format": [" "]}"""))
    }

    @Test
    fun `dataset without distributions fails`() {
        assertEquals(ResultTestEnum.FAIL, run("distributionFormatSpecified", dmp("""{"title": "a"}""")).result)
    }
}
