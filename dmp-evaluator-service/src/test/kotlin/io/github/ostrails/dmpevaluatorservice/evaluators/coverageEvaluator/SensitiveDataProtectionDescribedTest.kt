package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SensitiveDataProtectionDescribedTest {

    private fun evaluate(vararg datasets: String) = run("sensitiveDataProtectionDescribed", dmp(*datasets)).result

    private fun sensitive(description: String) =
        """{"title": "a", "sensitive_data": "yes", "security_and_privacy": [{"title": "Storage", "description": "$description"}]}"""

    @Test
    fun `access rights management mentioned passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate(sensitive("Kept on institutional storage with Access Control lists.")))
    }

    @Test
    fun `description without access reference fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(sensitive("Data is encrypted at rest.")))
    }

    @Test
    fun `sensitive dataset without description fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "sensitive_data": "yes", "security_and_privacy": [{"title": "Encryption"}]}"""))
    }

    @Test
    fun `non-sensitive datasets are skipped`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a", "sensitive_data": "no"}"""))
        assertEquals(ResultTestEnum.PASS, evaluate(sensitive("Login via VPN only."), """{"title": "b", "sensitive_data": "no"}"""))
    }

    @Test
    fun `unknown sensitive flag is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a", "sensitive_data": "unknown"}"""))
    }
}
