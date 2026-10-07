package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StorageLocationDeclaredTest {

    private fun evaluate(host: String?) =
        run("storageLocationDeclared", dmp("""{"title": "a", "distribution": [{"title": "d"${if (host != null) """, "host": $host""" else ""}}]}""")).result

    @Test
    fun `host with title passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "Zenodo"}"""))
    }

    @Test
    fun `host with only url passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"url": "https://zenodo.org"}"""))
    }

    @Test
    fun `host with only host_id passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"host_id": [{"identifier": "https://doi.org/10.17616/R3QP53", "type": "re3data"}]}"""))
    }

    @Test
    fun `host without title url or host_id fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"description": "somewhere"}"""))
    }

    @Test
    fun `distribution without host fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(null))
    }
}
