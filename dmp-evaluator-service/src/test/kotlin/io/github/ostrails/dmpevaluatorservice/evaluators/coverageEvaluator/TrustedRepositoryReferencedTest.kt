package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TrustedRepositoryReferencedTest {

    private fun evaluate(host: String?) =
        run("trustedRepositoryReferenced", dmp("""{"title": "a", "distribution": [{"title": "d"${if (host != null) """, "host": $host""" else ""}}]}""")).result

    @Test
    fun `host with re3data id passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "Zenodo", "host_id": [{"identifier": "https://doi.org/10.17616/R3QP53", "type": "re3data"}]}"""))
    }

    @Test
    fun `host with only a url id fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "Zenodo", "host_id": [{"identifier": "https://zenodo.org", "type": "url"}]}"""))
    }

    @Test
    fun `host without host_id or no host fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "Zenodo", "url": "https://zenodo.org"}"""))
        assertEquals(ResultTestEnum.FAIL, evaluate(null))
    }
}
