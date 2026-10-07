package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SensitiveDataProtectedTest {

    private val measures = """"security_and_privacy": [{"title": "Encryption at rest"}]"""

    private fun dataset(personal: String?, sensitive: String?, secured: Boolean): String {
        val fields = listOfNotNull(
            """"title": "d"""",
            personal?.let { """"personal_data": "$it"""" },
            sensitive?.let { """"sensitive_data": "$it"""" },
            measures.takeIf { secured },
        )
        return "{${fields.joinToString()}}"
    }

    private fun evaluate(vararg datasets: String) = run("sensitiveDataProtected", dmp(*datasets)).result

    @Test
    fun `sensitive dataset with measures passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate(dataset("no", "yes", secured = true), dataset("no", "no", secured = false)))
    }

    @Test
    fun `personal dataset without measures fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(dataset("yes", "no", secured = false)))
    }

    @Test
    fun `unknown flag is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate(dataset("unknown", "no", secured = false)))
    }

    @Test
    fun `missing flag is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate(dataset(null, "no", secured = true)))
    }

    @Test
    fun `failure beats unknown`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(dataset("unknown", "no", secured = false), dataset("yes", "yes", secured = false)))
    }

    @Test
    fun `no sensitive datasets is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate(dataset("no", "no", secured = false)))
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate())
    }
}
