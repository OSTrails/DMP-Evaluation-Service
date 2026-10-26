package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NewDatasetMetadataSufficientTest {

    private fun evaluate(vararg datasets: String) = run("newDatasetMetadataSufficient", dmp(*datasets)).result

    private val standard = """"metadata_standard_id": {"identifier": "https://schema.datacite.org/", "type": "url"}"""

    @Test
    fun `complete metadata passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "metadata": [{"description": "DataCite", "language": "eng", $standard}]}"""))
    }

    @Test
    fun `metadata without language fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "metadata": [{"description": "DataCite", $standard}]}"""))
    }

    @Test
    fun `metadata standard without type fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(
            """{"title": "a", "metadata": [{"description": "DataCite", "language": "eng", "metadata_standard_id": {"identifier": "https://schema.datacite.org/"}}]}"""))
    }

    @Test
    fun `new dataset without metadata fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": false}"""))
    }

    @Test
    fun `only reused datasets is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a", "is_reused": true}"""))
    }
}
