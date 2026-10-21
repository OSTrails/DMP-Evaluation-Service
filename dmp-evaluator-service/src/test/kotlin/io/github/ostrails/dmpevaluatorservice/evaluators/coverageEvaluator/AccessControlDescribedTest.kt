package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AccessControlDescribedTest {

    private fun evaluate(vararg datasets: String) = run("accessControlDescribed", dmp(*datasets)).result

    private fun sensitive(description: String) =
        """{"title": "a", "sensitive_data": "yes", "security_and_privacy": [{"title": "Access", "description": "$description"}]}"""

    @Test
    fun `access control and roles mentioned passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate(sensitive("Authentication via SSO; each user group has its own privileges.")))
    }

    @Test
    fun `access control without roles fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(sensitive("Access only via VPN login.")))
    }

    @Test
    fun `roles without access control fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(sensitive("The PI has the data steward role.")))
    }

    @Test
    fun `no sensitive data is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a", "sensitive_data": "no"}"""))
    }
}
