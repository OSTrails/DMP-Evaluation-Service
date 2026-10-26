package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReusedDatasetDistributionDeclaredTest {

    private fun evaluate(vararg datasets: String) = run("reusedDatasetDistributionDeclared", dmp(*datasets)).result

    @Test
    fun `distribution with title and access url passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "is_reused": true, "distribution": [{"title": "d", "access_url": "https://zenodo.org/records/1"}]}"""))
    }

    @Test
    fun `download url is accepted as access location`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "is_reused": true, "distribution": [{"title": "d", "download_url": "https://zenodo.org/records/1/files/x.csv"}]}"""))
    }

    @Test
    fun `distribution without access location fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": true, "distribution": [{"title": "d"}]}"""))
    }

    @Test
    fun `distribution without title fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": true, "distribution": [{"access_url": "https://zenodo.org/records/1"}]}"""))
    }

    @Test
    fun `reused dataset without distributions fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": true}"""))
    }

    @Test
    fun `only new datasets is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a"}"""))
    }
}
