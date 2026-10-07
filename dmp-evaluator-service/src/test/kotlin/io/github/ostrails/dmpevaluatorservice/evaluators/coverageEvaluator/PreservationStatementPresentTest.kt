package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PreservationStatementPresentTest {

    private val preserved = """{"title": "a", "preservation_statement": "Kept in Zenodo for at least 10 years."}"""

    private fun evaluate(vararg datasets: String) = run("preservationStatementPresent", dmp(*datasets)).result

    @Test
    fun `all datasets with a statement pass`() {
        assertEquals(ResultTestEnum.PASS, evaluate(preserved, preserved))
    }

    @Test
    fun `dataset without statement fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(preserved, """{"title": "b"}"""))
    }

    @Test
    fun `blank statement fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "preservation_statement": "  "}"""))
    }

    @Test
    fun `no datasets fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate())
    }
}
