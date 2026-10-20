package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReadmeReferencedTest {

    private fun evaluate(dataset: String) = run("readmeReferenced", dmp(dataset)).result

    @Test
    fun `readme in dataset description passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "description": "Survey data, documented in a ReadMe file."}"""))
    }

    @Test
    fun `readme distribution passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "distribution": [{"title": "README.md"}]}"""))
    }

    @Test
    fun `readme in download url passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "distribution": [{"title": "d", "download_url": "https://example.org/files/read_me.txt"}]}"""))
    }

    @Test
    fun `no readme reference fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "description": "Data already measured in 2024.", "distribution": [{"title": "data.csv"}]}"""))
    }
}
