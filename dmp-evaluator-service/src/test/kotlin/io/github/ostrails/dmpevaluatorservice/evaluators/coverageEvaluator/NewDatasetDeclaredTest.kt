package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NewDatasetDeclaredTest {

    private fun evaluate(vararg datasets: String) = run("newDatasetDeclared", dmp(*datasets)).result

    @Test
    fun `dataset marked as not reused passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "is_reused": false}""", """{"title": "b", "is_reused": true}"""))
    }

    @Test
    fun `dataset without is_reused counts as new`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a"}"""))
    }

    @Test
    fun `only reused datasets fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": true}"""))
    }

    @Test
    fun `no datasets fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate())
    }

    @Test
    fun `missing dmp fails`() {
        assertEquals(ResultTestEnum.FAIL, run("newDatasetDeclared", JsonObject(emptyMap())).result)
    }
}
