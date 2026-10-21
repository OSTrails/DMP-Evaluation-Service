package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GdprEthicsComplianceDeclaredTest {

    private fun evaluate(vararg datasets: String, dmpFields: String? = null) =
        run("gdprEthicsComplianceDeclared", dmp(*datasets, dmpFields = dmpFields)).result

    private fun personal(description: String) =
        """{"title": "a", "personal_data": "yes", "security_and_privacy": [{"title": "Privacy", "description": "$description"}]}"""

    @Test
    fun `gdpr mentioned passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate(personal("Processing follows the GDPR.")))
    }

    @Test
    fun `ethics report passes without gdpr mention`() {
        assertEquals(ResultTestEnum.PASS, evaluate(personal("Pseudonymised."), dmpFields = """"ethical_issues_report": "https://example.org/ethics.pdf""""))
    }

    @Test
    fun `neither gdpr nor ethics report fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(personal("Pseudonymised.")))
    }

    @Test
    fun `no personal data is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a", "personal_data": "no"}"""))
    }
}
