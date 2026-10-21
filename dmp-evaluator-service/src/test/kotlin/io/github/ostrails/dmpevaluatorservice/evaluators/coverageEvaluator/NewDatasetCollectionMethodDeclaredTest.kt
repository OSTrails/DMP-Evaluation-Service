package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NewDatasetCollectionMethodDeclaredTest {

    private fun evaluate(vararg datasets: String) = run("newDatasetCollectionMethodDeclared", dmp(*datasets)).result

    @Test
    fun `technical resource with name and description passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "technical_resource": [{"name": "Microscope", "description": "Confocal microscope"}]}"""))
    }

    @Test
    fun `complete identifier passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate(
            """{"title": "a", "technical_resource": [{"name": "M", "description": "D", "technical_resource_id": [{"identifier": "https://w3id.org/x", "type": "url"}]}]}"""))
    }

    @Test
    fun `identifier without type fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(
            """{"title": "a", "technical_resource": [{"name": "M", "description": "D", "id": {"identifier": "https://w3id.org/x"}}]}"""))
    }

    @Test
    fun `technical resource without description fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "technical_resource": [{"name": "Microscope"}]}"""))
    }

    @Test
    fun `new dataset without technical resource fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "is_reused": false}"""))
    }

    @Test
    fun `only reused datasets is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("""{"title": "a", "is_reused": true}"""))
    }
}
