package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NoEthicalIssuesJustifiedTest {

    private fun result(status: String?, description: String?): ResultTestEnum {
        val fields = listOfNotNull(
            status?.let { """"ethical_issues_exist": "$it"""" },
            description?.let { """"ethical_issues_description": "$it"""" },
        ).joinToString().ifEmpty { null }
        return run("noEthicalIssuesJustified", dmp(dmpFields = fields)).result
    }

    @Test
    fun `no with justification passes`() {
        assertEquals(ResultTestEnum.PASS, result("no", "No human participants or personal data are involved."))
    }

    @Test
    fun `no without justification fails`() {
        assertEquals(ResultTestEnum.FAIL, result("no", null))
        assertEquals(ResultTestEnum.FAIL, result("no", " "))
    }

    @Test
    fun `yes, unknown or missing status is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, result("yes", null))
        assertEquals(ResultTestEnum.INDETERMINATE, result("unknown", null))
        assertEquals(ResultTestEnum.INDETERMINATE, result(null, null))
    }
}
