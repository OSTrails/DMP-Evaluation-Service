package io.github.ostrails.dmpevaluatorservice.evaluators.completenessEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import kotlinx.serialization.json.JsonObject
import org.everit.json.schema.Schema
import org.everit.json.schema.ValidationException
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONObject
import org.json.JSONTokener
import org.springframework.stereotype.Component
import java.io.InputStream
import java.util.*

@Component
class DCSCompletenessEvaluator: EvaluatorPlugin {

    override fun supports(t: String): Boolean = t == getPluginIdentifier()

    override fun getPluginIdentifier(): String {
        return "DCSCompletenessEvaluator"
    }

    override val functionMap = mapOf(
        "evaluateStructure" to ::evaluateStructure,
    )

    override fun getPluginInformation(): PluginInfo {
        return PluginInfo(
            pluginId = getPluginIdentifier(),
            description = "Evaluator to perform completeness tests",
            tests = listOf()
        )
    }

    override fun evaluate(maDMP: Map<String, Any>, config: Map<String, Any>, tests: List<String>, report: EvaluationReport): List<Evaluation> {
        val evaluationsResults = tests.map { test ->
            Evaluation(
                evaluationId = UUID.randomUUID().toString(),
                result = ResultTestEnum.PASS,
                title = "Testing ",
                details = "Auto-generated evaluation of the test" + test ,
                reportId = report.reportId,
                generated = "${this::class.qualifiedName}:: evaluate"
            )
        }
        return evaluationsResults
    }

    fun evaluateStructure(
        maDMP: JsonObject,
        reportId: String
    ): Evaluation {
        val validationmaDMP = Validator.validate(maDMP.toString())
        println("The validationMaDMP object-------------------: $validationmaDMP")
        return Evaluation(
            evaluationId = UUID.randomUUID().toString(),
            result = if (validationmaDMP.isEmpty()) ResultTestEnum.PASS else ResultTestEnum.FAIL,
            details = "Test to check the completness of a maDMP agains the RDA Commom standart ",
            title = "Testing ",
            reportId = reportId,
            log = validationmaDMP.toString(),
            generated = "${this::class.qualifiedName}:: evaluateStructure"
        )
    }


    object Validator {
        private val schema: Schema by lazy {
            val inputStream: InputStream = javaClass.classLoader
                .getResourceAsStream("maDMPSchemas/maDMP-schema-1.2.json")
                ?: throw IllegalStateException("Schema file not found")
            val rawSchema = JSONObject(JSONTokener(inputStream))
            SchemaLoader.load(rawSchema)
        }

        fun validate(inputJson: String): List<String> {
            return try {
                val jsonObject = JSONObject(inputJson)
                schema.validate(jsonObject)
                emptyList()
            } catch (e: ValidationException) {
                e.allMessages
            }
        }
    }







}