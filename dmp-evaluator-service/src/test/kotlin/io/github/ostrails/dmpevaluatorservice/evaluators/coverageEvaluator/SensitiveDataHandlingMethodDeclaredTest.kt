package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SensitiveDataHandlingMethodDeclaredTest {

    private fun evaluate(vararg datasets: String) = run("sensitiveDataHandlingMethodDeclared", dmp(*datasets)).result

    @Test
    fun `sensitive dataset with description passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate(
            """{"title": "a", "sensitive_data": "yes", "security_and_privacy": [{"title": "Pseudonymisation", "description": "Names are replaced by codes."}]}"""))
    }

    @Test
    fun `sensitive dataset without description fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "sensitive_data": "yes"}"""))
    }

    @Test
    fun `missing sensitive flag is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a"}"""))
    }

    @Test
    fun `no sensitive data is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a", "sensitive_data": "no"}"""))
    }
}
