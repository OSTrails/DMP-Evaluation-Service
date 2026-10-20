package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SecureTrustedStorageUsedTest {

    private val trustedHost = """{"title": "Zenodo", "host_id": [{"identifier": "https://doi.org/10.17616/R3QP53", "type": "re3data"}]}"""
    private val measures = """"security_and_privacy": [{"title": "Encryption at rest"}]"""

    private fun evaluate(dataset: String) = run("secureTrustedStorageUsed", dmp(dataset)).result

    @Test
    fun `re3data host and security measures pass`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", $measures, "distribution": [{"title": "d", "host": $trustedHost}]}"""))
    }

    @Test
    fun `certified host and security measures pass`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", $measures, "distribution": [{"title": "d", "host": {"title": "Archive", "certified_with": "coretrustseal"}}]}"""))
    }

    @Test
    fun `untrusted host fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", $measures, "distribution": [{"title": "d", "host": {"title": "Laptop"}}]}"""))
    }

    @Test
    fun `one distribution without host fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", $measures, "distribution": [{"title": "d1", "host": $trustedHost}, {"title": "d2"}]}"""))
    }

    @Test
    fun `missing security measures fail`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "distribution": [{"title": "d", "host": $trustedHost}]}"""))
    }

    @Test
    fun `dataset without distributions fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", $measures}"""))
    }
}
