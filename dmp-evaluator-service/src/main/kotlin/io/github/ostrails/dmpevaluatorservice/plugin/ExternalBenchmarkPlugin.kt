package io.github.ostrails.dmpevaluatorservice.plugin

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import kotlinx.serialization.json.JsonObject

interface ExternalBenchmarkPlugin : EvaluatorPlugin {
    val benchmarkFunctionMap: Map<String, (JsonObject, reportId: String, testRecord: TestRecord) -> List<Evaluation>>
}
