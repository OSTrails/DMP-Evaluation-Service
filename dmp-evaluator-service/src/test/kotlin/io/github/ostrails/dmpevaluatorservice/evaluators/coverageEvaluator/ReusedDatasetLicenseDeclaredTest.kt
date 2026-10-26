package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReusedDatasetLicenseDeclaredTest {

    private fun evaluate(vararg datasets: String) = run("reusedDatasetLicenseDeclared", dmp(*datasets)).result

    @Test
    fun `reused dataset with complete license passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate(
            """{"title": "a", "is_reused": true, "distribution": [{"title": "d", "license": [{"license_ref": "https://creativecommons.org/licenses/by/4.0/", "start_date": "2020-01-01"}]}]}"""))
    }

    @Test
    fun `license without start date fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(
            """{"title": "a", "is_reused": true, "distribution": [{"title": "d", "license": [{"license_ref": "https://creativecommons.org/licenses/by/4.0/"}]}]}"""))
    }

    @Test
    fun `reused dataset without license fails`() {
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
}
