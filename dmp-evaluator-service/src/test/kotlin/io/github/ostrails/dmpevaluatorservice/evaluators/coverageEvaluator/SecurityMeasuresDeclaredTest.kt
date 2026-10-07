package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SecurityMeasuresDeclaredTest {

    private val secured = """{"title": "a", "security_and_privacy": [{"title": "Encryption at rest", "description": "AES-256"}]}"""

    private fun evaluate(vararg datasets: String) = run("securityMeasuresDeclared", dmp(*datasets)).result

    @Test
    fun `datasets with measures pass`() {
        assertEquals(ResultTestEnum.PASS, evaluate(secured, secured))
    }

    @Test
    fun `dataset without measures fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(secured, """{"title": "b"}"""))
    }

    @Test
    fun `measure without title does not count`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "security_and_privacy": [{"description": "AES-256"}]}"""))
    }

    @Test
    fun `no datasets fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate())
    }
}
