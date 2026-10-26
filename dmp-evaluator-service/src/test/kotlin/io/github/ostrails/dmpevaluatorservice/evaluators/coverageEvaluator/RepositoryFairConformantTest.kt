package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RepositoryFairConformantTest {

    private val re3data = """"host_id": [{"identifier": "https://doi.org/10.17616/R3QP53", "type": "re3data"}]"""

    private fun evaluate(host: String?) =
        run("repositoryFairConformant", dmp("""{"title": "a", "distribution": [{"title": "d"${if (host != null) """, "host": $host""" else ""}}]}""")).result

    @Test
    fun `host with pid system and re3data entry passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "Zenodo", "pid_system": ["doi"], $re3data}"""))
    }

    @Test
    fun `host with pid system and recognised certification passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "Archive", "pid_system": ["handle"], "certified_with": "CoreTrustSeal"}"""))
    }

    @Test
    fun `host without pid system fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "Zenodo", $re3data}"""))
    }

    @Test
    fun `host without re3data entry or recognised certification fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "Archive", "pid_system": ["doi"], "certified_with": "unknown-seal"}"""))
    }

    @Test
    fun `distribution without host fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(null))
    }
}
