package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object CoverageTestSupport {

    private val evaluator = DCSCoverageEvaluator()

    fun testRecord(function: String) = TestRecord(
        id = "test-id",
        title = "Coverage test",
        description = "Coverage test description",
        license = "https://creativecommons.org/licenses/by/4.0/",
        version = "1.0.0",
        metricImplemented = null,
        evaluator = "DCSCoverageEvaluator",
        functionEvaluator = function,
    )

    // A maDMP with the given dataset JSON objects; [dmpFields] is spliced into the dmp object (e.g. "\"ethical_issues_exist\": \"no\"")
    fun dmp(vararg datasets: String, dmpFields: String? = null): JsonObject {
        val extra = if (dmpFields != null) ", $dmpFields" else ""
        return Json.parseToJsonElement("""{"dmp": {"title": "Test DMP", "dataset": [${datasets.joinToString()}]$extra}}""").jsonObject
    }

    // Runs a function through the plugin's functionMap, so the test also proves the function is registered
    fun run(function: String, maDMP: JsonObject): Evaluation =
        evaluator.functionMap.getValue(function)(maDMP, "report-id", testRecord(function))
}
