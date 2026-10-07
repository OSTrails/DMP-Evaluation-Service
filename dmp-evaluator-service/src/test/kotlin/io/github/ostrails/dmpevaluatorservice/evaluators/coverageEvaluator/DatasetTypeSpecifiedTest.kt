package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DatasetTypeSpecifiedTest {

    private fun evaluate(vararg datasets: String) = run("datasetTypeSpecified", dmp(*datasets)).result

    @Test
    fun `datasets with type pass`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "type": "Dataset"}""", """{"title": "b", "type": "Software"}"""))
    }

    @Test
    fun `dataset without or with blank type fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "type": "Dataset"}""", """{"title": "b"}"""))
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "type": " "}"""))
    }

    @Test
    fun `no datasets fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate())
    }
}
