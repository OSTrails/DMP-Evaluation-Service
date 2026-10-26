package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReusedDatasetAccessUrlDeclaredTest {

    private fun evaluate(vararg datasets: String) = run("reusedDatasetAccessUrlDeclared", dmp(*datasets)).result

    @Test
    fun `reused dataset with access url passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "is_reused": true, "distribution": [{"title": "d", "access_url": "https://zenodo.org/records/1"}]}"""))
    }

    @Test
    fun `reused dataset without access url fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": true, "distribution": [{"title": "d"}]}"""))
    }

    @Test
    fun `reused dataset without distributions fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": true}"""))
    }

    @Test
    fun `only new datasets is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a", "is_reused": false}"""))
    }

    @Test
    fun `dataset without is_reused counts as new and is skipped`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a", "distribution": [{"title": "d"}]}"""))
    }
}
