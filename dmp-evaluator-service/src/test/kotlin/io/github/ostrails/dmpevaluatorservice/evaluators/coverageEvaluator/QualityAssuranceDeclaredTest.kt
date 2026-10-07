package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class QualityAssuranceDeclaredTest {

    private val checked = """{"title": "a", "data_quality_assurance": ["Instruments are calibrated daily."]}"""

    private fun evaluate(vararg datasets: String) = run("qualityAssuranceDeclared", dmp(*datasets)).result

    @Test
    fun `datasets with quality assurance pass`() {
        assertEquals(ResultTestEnum.PASS, evaluate(checked, checked))
    }

    @Test
    fun `dataset without or with blank quality assurance fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(checked, """{"title": "b"}"""))
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a", "data_quality_assurance": [""]}"""))
    }

    @Test
    fun `no datasets fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate())
    }
}
