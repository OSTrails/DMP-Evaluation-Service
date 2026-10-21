package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NewDatasetAccessDeclaredTest {

    private fun evaluate(vararg datasets: String) = run("newDatasetAccessDeclared", dmp(*datasets)).result

    private val license = """"license": [{"license_ref": "https://creativecommons.org/licenses/by/4.0/", "start_date": "2026-01-01"}]"""

    @Test
    fun `new dataset with declared access and license passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "is_reused": false, "distribution": [{"title": "d", "data_access": "restricted", $license}]}"""))
    }

    @Test
    fun `new dataset without access fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": false, "distribution": [{"title": "d", $license}]}"""))
    }

    @Test
    fun `invalid access value fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": false, "distribution": [{"title": "d", "data_access": "public", $license}]}"""))
    }

    @Test
    fun `new dataset without license fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": false, "distribution": [{"title": "d", "data_access": "open"}]}"""))
    }

    @Test
    fun `new dataset without distributions fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": false}"""))
    }

    @Test
    fun `reused dataset without access is ignored`() {
        assertEquals(ResultTestEnum.PASS, evaluate(
            """{"title": "a", "is_reused": false, "distribution": [{"title": "d", "data_access": "open", $license}]}""",
            """{"title": "b", "is_reused": true, "distribution": [{"title": "d"}]}""",
        ))
    }

    @Test
    fun `only reused datasets is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a", "is_reused": true, "distribution": [{"title": "d"}]}"""))
    }

    @Test
    fun `dataset without is_reused counts as new`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "distribution": [{"title": "d"}]}"""))
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "distribution": [{"title": "d", "data_access": "open", $license}]}"""))
    }
}
