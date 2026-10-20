package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PersonnelAndMonetaryBudgetDeclaredTest {

    private val personnel = """{"title": "Data steward, 0.2 FTE", "value": 12000, "currency_code": "EUR"}"""
    private val storage = """{"title": "Repository fees", "value": 500, "currency_code": "EUR"}"""

    private fun evaluate(costs: String?) =
        run("personnelAndMonetaryBudgetDeclared", dmp(dmpFields = costs?.let { """"cost": [$it]""" })).result

    @Test
    fun `personnel and other cost with value and currency pass`() {
        assertEquals(ResultTestEnum.PASS, evaluate("$personnel, $storage"))
    }

    @Test
    fun `only personnel cost fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(personnel))
    }

    @Test
    fun `only other cost fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(storage))
    }

    @Test
    fun `cost without currency fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""$personnel, {"title": "Storage", "value": 500}"""))
    }

    @Test
    fun `cost with value as string fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""$personnel, {"title": "Storage", "value": "500", "currency_code": "EUR"}"""))
    }

    @Test
    fun `no costs fail`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(null))
    }
}
