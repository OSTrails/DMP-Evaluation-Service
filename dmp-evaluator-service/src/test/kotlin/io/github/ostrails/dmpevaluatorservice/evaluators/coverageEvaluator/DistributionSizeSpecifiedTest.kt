package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DistributionSizeSpecifiedTest {

    private fun evaluate(sizeField: String?) =
        run("distributionSizeSpecified", dmp("""{"title": "a", "distribution": [{"title": "d"${if (sizeField != null) ", \"byte_size\": $sizeField" else ""}}]}""")).result

    @Test
    fun `integer size passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("1048576"))
        assertEquals(ResultTestEnum.PASS, evaluate("0"))
    }

    @Test
    fun `missing size fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(null))
    }

    @Test
    fun `negative, decimal or text size fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("-5"))
        assertEquals(ResultTestEnum.FAIL, evaluate("1.5"))
        assertEquals(ResultTestEnum.FAIL, evaluate("\"1 GB\""))
    }
}
