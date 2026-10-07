package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RepositoryPidSystemDeclaredTest {

    private fun evaluate(host: String?) =
        run("repositoryPidSystemDeclared", dmp("""{"title": "a", "distribution": [{"title": "d"${if (host != null) """, "host": $host""" else ""}}]}""")).result

    @Test
    fun `host with pid system passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "Zenodo", "pid_system": ["doi"]}"""))
    }

    @Test
    fun `host without pid system fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "Zenodo"}"""))
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "Zenodo", "pid_system": []}"""))
    }

    @Test
    fun `distribution without host fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(null))
    }
}
