package io.github.ostrails.dmpevaluatorservice.utils

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import kotlinx.serialization.json.*
import java.util.UUID

fun parseFairChampionResponse(
    data: JsonObject,
    reportId: String,
    testRecord: TestRecord
): List<Evaluation> {
    val testedGuid = data["testedguid"]?.jsonPrimitive?.contentOrNull ?: ""

    val resultSetStr = data["resultset"]?.jsonPrimitive?.contentOrNull
        ?: return emptyList()

    val resultSetJson = try {
        Json.parseToJsonElement(resultSetStr).jsonObject
    } catch (e: Exception) {
        return emptyList()
    }

    val graph = resultSetJson["@graph"]?.jsonArray ?: return emptyList()

    return graph
        .filterIsInstance<JsonObject>()
        .filter { node -> node.isTestResult() }
        .map { node -> node.toEvaluation(testedGuid, reportId, testRecord) }
}

private fun JsonObject.isTestResult(): Boolean =
    when (val type = this["@type"]) {
        is JsonArray -> type.any { it.jsonPrimitiveOrNull?.contentOrNull == "ftr:TestResult" }
        is JsonPrimitive -> type.contentOrNull == "ftr:TestResult"
        else -> false
    }

private fun JsonObject.toEvaluation(
    testedGuid: String,
    reportId: String,
    testRecord: TestRecord
): Evaluation {
    val title = this.langValue("dct:title") ?: "Unknown test"
    val description = this.langValue("dct:description") ?: ""
    val value = this.langValue("prov:value")?.lowercase()
    val log = this.langValue("ftr:log") ?: ""

    val result = when (value) {
        "pass" -> ResultTestEnum.PASS
        "fail" -> ResultTestEnum.FAIL
        else -> ResultTestEnum.INDERTERMINATED
    }

    return Evaluation(
        evaluationId = UUID.randomUUID().toString(),
        title = title,
        details = description,
        result = result,
        reportId = reportId,
        log = log,
        assessmentTarget = testedGuid,
        wasGeneratedBy = "FAIRChampionEvaluator::evaluateFAIRnessDatasetByChampion",
        outputFromTest = testRecord.id,
        completion = 100
    )
}

// Reads {"@language": "en", "@value": "..."} and returns the @value string
private fun JsonObject.langValue(key: String): String? =
    this[key]?.jsonObjectOrNull()?.get("@value")?.jsonPrimitiveOrNull?.contentOrNull

private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject
