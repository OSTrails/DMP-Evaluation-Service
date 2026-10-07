package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RepositoryCertifiedTest {

    private fun evaluate(host: String?) =
        run("repositoryCertified", dmp("""{"title": "a", "distribution": [{"title": "d"${if (host != null) """, "host": $host""" else ""}}]}""")).result

    @Test
    fun `recognised certification passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "Repo", "certified_with": "coretrustseal"}"""))
    }

    @Test
    fun `missing or unrecognised certification fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "Repo"}"""))
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "Repo", "certified_with": "iso9001"}"""))
    }

    @Test
    fun `distribution without host fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(null))
    }
}
