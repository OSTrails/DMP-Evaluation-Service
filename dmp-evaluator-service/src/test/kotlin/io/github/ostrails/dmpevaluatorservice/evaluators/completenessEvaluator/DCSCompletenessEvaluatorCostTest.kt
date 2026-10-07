package io.github.ostrails.dmpevaluatorservice.evaluators.completenessEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DCSCompletenessEvaluatorCostTest {

    private val evaluator = DCSCompletenessEvaluator()

    private val testRecord = TestRecord(
        id = "test-id",
        title = "Cost Entity Completeness",
        description = "Checks that cost fields are filled in.",
        license = "https://opensource.org/license/mit",
        version = "0.0.1",
        metricImplemented = null,
        evaluator = "DCSCompletenessEvaluator",
        functionEvaluator = "costEntityValuesPresent",
    )

    private val completeCost = """{"title": "Storage", "description": "Repository fees", "value": 1000, "currency_code": "EUR"}"""
    private val incompleteCost = """{"title": "Personnel"}"""

    private fun evaluateCosts(costsJson: String) =
        evaluator.costEntityValuesPresent(
            Json.parseToJsonElement("""{"dmp": {"cost": $costsJson}}""").jsonObject,
            "report-id",
            testRecord
        ).result

    @Test
    fun `incomplete cost followed by a complete one fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluateCosts("[$incompleteCost, $completeCost]"))
    }

    @Test
    fun `complete cost followed by an incomplete one fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluateCosts("[$completeCost, $incompleteCost]"))
    }

    @Test
    fun `all complete costs pass`() {
        assertEquals(ResultTestEnum.PASS, evaluateCosts("[$completeCost, $completeCost]"))
    }

    @Test
    fun `no cost entries fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluateCosts("[]"))
    }

    @Test
    fun `non-object cost entry fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluateCosts("""[$completeCost, "not an object"]"""))
    }
}
