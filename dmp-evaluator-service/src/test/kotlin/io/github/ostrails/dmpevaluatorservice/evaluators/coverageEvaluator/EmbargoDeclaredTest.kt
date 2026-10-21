package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EmbargoDeclaredTest {

    private val created = """"created": "2026-03-01T09:00:00Z""""

    private fun evaluate(startDate: String, dmpFields: String? = created) = run("embargoDeclared", dmp(
        """{"title": "a", "distribution": [{"title": "d", "license": [{"license_ref": "https://creativecommons.org/licenses/by/4.0/", "start_date": "$startDate"}]}]}""",
        dmpFields = dmpFields,
    )).result

    @Test
    fun `license starting before creation passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("2026-03-01"))
    }

    @Test
    fun `license starting after creation fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("2027-03-01"))
    }

    @Test
    fun `without creation date today is the reference`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("2999-01-01", dmpFields = null))
        assertEquals(ResultTestEnum.PASS, evaluate("2020-01-01", dmpFields = null))
    }

    @Test
    fun `unreadable start date is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("next year"))
    }

    @Test
    fun `distribution without license has no embargo`() {
        assertEquals(ResultTestEnum.PASS, run("embargoDeclared", dmp("""{"title": "a", "distribution": [{"title": "d"}]}""")).result)
    }
}
