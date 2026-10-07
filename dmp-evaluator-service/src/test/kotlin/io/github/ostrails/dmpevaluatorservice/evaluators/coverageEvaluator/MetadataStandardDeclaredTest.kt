package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MetadataStandardDeclaredTest {

    private val described = """{"title": "a", "metadata": [{"language": "eng", "metadata_standard_id": {"identifier": "https://schema.datacite.org/", "type": "url"}}]}"""

    private fun evaluate(vararg datasets: String) = run("metadataStandardDeclared", dmp(*datasets)).result

    @Test
    fun `datasets with a metadata standard pass`() {
        assertEquals(ResultTestEnum.PASS, evaluate(described, described))
    }

    @Test
    fun `dataset without metadata fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(described, """{"title": "b"}"""))
    }

    @Test
    fun `metadata entry without standard identifier fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "metadata": [{"language": "eng", "description": "Dublin Core"}]}"""))
    }

    @Test
    fun `no datasets fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate())
    }
}
