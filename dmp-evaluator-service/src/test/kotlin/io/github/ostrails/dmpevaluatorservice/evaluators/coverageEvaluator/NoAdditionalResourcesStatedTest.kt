package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NoAdditionalResourcesStatedTest {

    private fun evaluate(dmpFields: String?) = run("noAdditionalResourcesStated", dmp(dmpFields = dmpFields)).result

    @Test
    fun `statement in dmp description passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate(""""description": "No additional costs are expected; storage is covered by the institution.""""))
    }

    @Test
    fun `cost with value 0 passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate(""""cost": [{"title": "Data management", "value": 0, "currency_code": "EUR"}]"""))
    }

    @Test
    fun `no costs and no statement fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(""""description": "A study on soil samples.""""))
    }

    @Test
    fun `declared cost above 0 is not applicable`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate(""""cost": [{"title": "Repository fees", "value": 500, "currency_code": "EUR"}]"""))
    }
}
