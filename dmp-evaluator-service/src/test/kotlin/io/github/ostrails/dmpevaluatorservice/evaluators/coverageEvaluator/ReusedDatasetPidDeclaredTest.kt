package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReusedDatasetPidDeclaredTest {

    private fun evaluate(vararg datasets: String) = run("reusedDatasetPidDeclared", dmp(*datasets)).result

    @Test
    fun `reused dataset with identifier and type passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "is_reused": true, "dataset_id": {"identifier": "10.5281/zenodo.1", "type": "doi"}}"""))
    }

    @Test
    fun `identifier without type fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": true, "dataset_id": {"identifier": "10.5281/zenodo.1"}}"""))
    }

    @Test
    fun `reused dataset without dataset_id fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": true}"""))
    }

    @Test
    fun `new dataset without identifier is ignored`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a", "is_reused": false}""", """{"title": "b"}"""))
    }
}
