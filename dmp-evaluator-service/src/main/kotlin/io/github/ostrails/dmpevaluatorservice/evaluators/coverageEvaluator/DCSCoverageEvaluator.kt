package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import kotlinx.serialization.json.JsonObject
import org.springframework.stereotype.Component

// Coverage checks for the OSTrails pilot metrics: each test verifies that a DMP Common Standard (DCS 1.2/1.3)
// field is declared for every relevant dataset, distribution or contributor.
@Component
class DCSCoverageEvaluator : EvaluatorPlugin {

    override fun supports(t: String): Boolean = t == getPluginIdentifier()

    override fun getPluginIdentifier(): String = "DCSCoverageEvaluator"

    override fun getPluginInformation(): PluginInfo = PluginInfo(
        pluginId = getPluginIdentifier(),
        description = "Evaluator to perform DMP Common Standard coverage tests for the OSTrails pilot metrics",
        functions = listOf()
    )

    companion object {
        const val NO_DATASETS = "No datasets found in the maDMP."
    }

    override val functionMap: Map<String, (JsonObject, String, TestRecord) -> Evaluation> = mapOf(
        "datasetLicenseDeclared" to ::datasetLicenseDeclared,
        "distributionLicensePresent" to ::distributionLicensePresent,
        "storageLocationDeclared" to ::storageLocationDeclared,
    )

    override fun evaluate(maDMP: Map<String, Any>, config: Map<String, Any>, tests: List<String>, report: EvaluationReport): List<Evaluation> =
        emptyList()

    // data.lice.co.1 - every dataset declares a license on at least one of its distributions
    fun datasetLicenseDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("datasetLicenseDeclared"),
            subjects = datasetSubjects(maDMP),
            noun = "dataset",
            requirement = "a license is declared on at least one distribution",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { dataset ->
            val licensed = dataset.array("distribution").orEmpty().count { (it as? JsonObject)?.let(::hasLicenseRef) == true }
            if (licensed > 0) SubjectCheck.Ok("$licensed distribution(s) declare a license.")
            else SubjectCheck.Problem("No distribution declares a license. Add a license with a 'license_ref' URL (e.g. https://creativecommons.org/licenses/by/4.0/).")
        }

    // data.shar.co.1 - every distribution declares a license
    fun distributionLicensePresent(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("distributionLicensePresent"),
            subjects = distributionSubjects(datasetSubjects(maDMP)),
            noun = "distribution",
            requirement = "a license is declared",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { distribution ->
            if (hasLicenseRef(distribution)) SubjectCheck.Ok("License declared.")
            else SubjectCheck.Problem("No license declared. Add a license with a 'license_ref' URL (e.g. https://creativecommons.org/licenses/by/4.0/).")
        }

    // store.cov.1 - every distribution names where it is stored (host title, url or host_id; url is optional in DCS 1.3)
    fun storageLocationDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("storageLocationDeclared"),
            subjects = distributionSubjects(datasetSubjects(maDMP)),
            noun = "distribution",
            requirement = "a storage location (host) is declared",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { distribution ->
            val host = distribution.obj("host")
            val name = host?.text("title") ?: host?.text("url") ?: host?.array("host_id")?.firstNotNullOfOrNull { (it as? JsonObject)?.text("identifier") }
            if (name != null) SubjectCheck.Ok("Stored at '$name'.")
            else if (host == null) SubjectCheck.Problem("No host declared. Add a 'host' describing where the data is stored (e.g. the repository title and URL).")
            else SubjectCheck.Problem("The host has no title, url or host_id. Identify where the data is stored.")
        }

    private fun hasLicenseRef(distribution: JsonObject): Boolean =
        distribution.array("license").orEmpty().any { (it as? JsonObject)?.text("license_ref") != null }

    private fun generatedBy(function: String) = "${this::class.qualifiedName}::$function"
}
