package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MetadataStandardInDmpTest {

    private val described = """{"title": "a", "metadata": [{"language": "eng", "metadata_standard_id": {"identifier": "https://schema.datacite.org/", "type": "url"}}]}"""

    private fun evaluate(vararg datasets: String) = run("metadataStandardInDmp", dmp(*datasets)).result

    @Test
    fun `one dataset with a standard is enough`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "b"}""", described))
    }

    @Test
    fun `no standard anywhere fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a"}""", """{"title": "b"}"""))
        assertEquals(ResultTestEnum.FAIL, evaluate())
    }
}
