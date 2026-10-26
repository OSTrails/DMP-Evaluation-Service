package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReusedDatasetSensitiveDataDeclaredTest {

    private fun evaluate(vararg datasets: String) = run("reusedDatasetSensitiveDataDeclared", dmp(*datasets)).result

    @Test
    fun `yes no and unknown pass`() {
        listOf("yes", "no", "unknown").forEach { value ->
            assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "is_reused": true, "sensitive_data": "$value"}"""), value)
        }
    }

    @Test
    fun `missing flag fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": true, "personal_data": "no"}"""))
    }

    @Test
    fun `invalid flag fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": true, "sensitive_data": "true"}"""))
    }

    @Test
    fun `only new datasets is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a"}"""))
    }
}
