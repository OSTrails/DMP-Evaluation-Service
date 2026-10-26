package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.DCSCoverageEvaluator.Companion.NO_DATASETS
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.DCSCoverageEvaluator.Companion.NO_DMP
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.DCSCoverageEvaluator.Companion.REPOSITORY_CERTIFICATIONS
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

// Group C - repository, storage and cost. Registered in DCSCoverageEvaluator.functionMap; only edit this file.
// Use the helpers in CoverageSupport.kt (datasetSubjects, distributionSubjects, dmpSubject, evaluateSubjects,
// SubjectCheck) and see DCSCoverageEvaluator for examples. Unit-test each function with
// CoverageTestSupport.run("<functionName>", dmp(...)). Above each function, state when it returns PASS / FAIL /
// INDETERMINATE.
object RepositoryStorageTests {

    val functions: Map<String, (JsonObject, String, TestRecord) -> Evaluation> = mapOf(
        "repositoryFairConformant" to ::repositoryFairConformant,
        "secureTrustedStorageUsed" to ::secureTrustedStorageUsed,
        "backupStrategyDeclared" to ::backupStrategyDeclared,
        "readmeReferenced" to ::readmeReferenced,
        "personnelAndMonetaryBudgetDeclared" to ::personnelAndMonetaryBudgetDeclared,
        "noAdditionalResourcesStated" to ::noAdditionalResourcesStated,
    )

    // DCS has no README or cost-category field, so these are found by keywords in free text
    private val README = Regex("(?<![a-z])read[ _-]?me(?![a-z])", RegexOption.IGNORE_CASE)
    private val PERSONNEL = Regex(
        "\\b(personnel|staff|salar(y|ies)|wages?|employees?|labou?r|fte|data (stewards?|managers?)|person[- ]?(months?|days?|hours?))\\b",
        RegexOption.IGNORE_CASE,
    )
    private val NO_ADDITIONAL_RESOURCES = Regex(
        "\\bno (additional|extra|further|specific|dedicated)( \\w+){0,3} (costs?|resources?|funding|budget)\\b" +
            "|\\b(costs?|resources?) (are|is|will be) (not required|not needed|covered by)\\b" +
            "|\\bno costs?\\b",
        RegexOption.IGNORE_CASE,
    )

    // repo.comp.2 - Repository Conforms with FAIR Data Principles
    // The host of every distribution declares FAIR-enabling attributes: a PID system (findable) and a re3data
    // host_id or a recognised certification (trustworthy). Only checks the declaration; the re3data record is not
    // fetched (ComplianceEvaluator.datasetRepositoryIsInRe3data does the online lookup).
    // PASS: every host declares both. FAIL: one is missing, a distribution has no host, or there are no datasets.
    fun repositoryFairConformant(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("repositoryFairConformant"),
            subjects = distributionSubjects(datasetSubjects(maDMP)),
            noun = "distribution",
            requirement = "the repository declares a PID system and a re3data entry or certification",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { distribution ->
            withHost(distribution) { host ->
                val systems = host.array("pid_system").orEmpty().mapNotNull { it.textOrNull() }
                val trust = trustedBy(host)
                val missing = listOfNotNull(
                    "a 'pid_system' (e.g. 'doi')".takeIf { systems.isEmpty() },
                    "a re3data 'host_id' or a recognised 'certified_with'".takeIf { trust == null },
                )
                if (missing.isEmpty()) SubjectCheck.Ok("PID system(s): ${systems.joinToString()}; $trust.")
                else SubjectCheck.Problem("The host does not declare ${missing.joinToString(" or ")}, so it cannot be shown to support FAIR data.")
            }
        }

    // store.cov.2 - Use of Secure Storage for the Dataset in a Trusted Repository
    // Every dataset is stored in a trusted repository (each distribution's host has a re3data host_id or a
    // recognised certification) and declares the security measures applied to it (security_and_privacy).
    // PASS: both hold. FAIL: no distributions, an untrusted host, no security measures, or no datasets.
    fun secureTrustedStorageUsed(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("secureTrustedStorageUsed"),
            subjects = datasetSubjects(maDMP),
            noun = "dataset",
            requirement = "it is stored in a trusted repository with security measures declared",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { dataset ->
            val distributions = dataset.array("distribution").orEmpty().mapNotNull { it as? JsonObject }
            val untrusted = distributions.filter { distribution -> distribution.obj("host")?.let(::trustedBy) == null }
                .map { it.text("title") ?: "untitled distribution" }
            val measures = dataset.array("security_and_privacy").orEmpty().mapNotNull { (it as? JsonObject)?.text("title") }
            val problems = listOfNotNull(
                "No distributions declared, so the storage location is unknown.".takeIf { distributions.isEmpty() },
                "Not stored in a trusted repository: ${untrusted.joinToString()}. Use a host with a re3data 'host_id' or a recognised 'certified_with'."
                    .takeIf { untrusted.isNotEmpty() },
                "No 'security_and_privacy' measures declared. Add each measure with a title (e.g. 'Encryption at rest')."
                    .takeIf { measures.isEmpty() },
            )
            if (problems.isEmpty()) SubjectCheck.Ok("Stored in trusted repositories; security measures: ${measures.joinToString()}.")
            else SubjectCheck.Problem(problems.joinToString(" "))
        }

    // repo.co.4 - Verification of Back-up Strategy
    // The host of every distribution states its backup strategy: backup_type and backup_frequency. A DMP cannot
    // show that backups are verified, so the test checks the declaration.
    // PASS: every host states both. FAIL: a field is missing, a distribution has no host, or there are no datasets.
    fun backupStrategyDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("backupStrategyDeclared"),
            subjects = distributionSubjects(datasetSubjects(maDMP)),
            noun = "distribution",
            requirement = "the backup type and frequency of the host are stated",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { distribution ->
            withHost(distribution) { host ->
                val type = host.text("backup_type")
                val frequency = host.text("backup_frequency")
                val missing = listOfNotNull("'backup_type'".takeIf { type == null }, "'backup_frequency'".takeIf { frequency == null })
                if (missing.isEmpty()) SubjectCheck.Ok("Backup type '$type', frequency '$frequency'.")
                else SubjectCheck.Problem("The host states no ${missing.joinToString(" or ")}. Describe how (e.g. 'tape', 'second data centre') and how often (e.g. 'daily') the data is backed up.")
            }
        }

    // meta.qual.2 - ReadMe File Reference
    // Every dataset references a README file. DCS has no README field, so 'readme' is searched in the dataset and
    // metadata descriptions and in the title, description, access_url and download_url of its distributions.
    // PASS: a reference is found for every dataset. FAIL: a dataset has none, or there are no datasets.
    fun readmeReferenced(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("readmeReferenced"),
            subjects = datasetSubjects(maDMP),
            noun = "dataset",
            requirement = "a README file is referenced",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { dataset ->
            val reference = readmeTexts(dataset).firstOrNull { README.containsMatchIn(it) }
            if (reference != null) SubjectCheck.Ok("README referenced: '$reference'.")
            else SubjectCheck.Problem("No README file is referenced. Mention it in the dataset description or add it as a distribution (e.g. title 'README.md').")
        }

    private fun readmeTexts(dataset: JsonObject): List<String> {
        val metadata = dataset.array("metadata").orEmpty().mapNotNull { (it as? JsonObject)?.text("description") }
        val distributions = dataset.array("distribution").orEmpty().mapNotNull { it as? JsonObject }
            .flatMap { distribution -> listOf("title", "description", "access_url", "download_url").mapNotNull(distribution::text) }
        return listOfNotNull(dataset.text("description")) + metadata + distributions
    }

    // cost.co.1 - DMP Includes a Budget for Personnel and Monetary Resources
    // The DMP declares at least one personnel cost (title or description mentions staff, salary, FTE, data steward,
    // person-months, ...) and at least one other cost, and every cost has a numeric value and a currency_code.
    // PASS: all three hold. FAIL: no costs, a category is missing, a cost is incomplete, or there is no DMP.
    fun personnelAndMonetaryBudgetDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("personnelAndMonetaryBudgetDeclared"),
            subjects = dmpSubject(maDMP),
            noun = "DMP",
            requirement = "personnel and other monetary costs are budgeted with value and currency",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DMP,
        ) { dmp ->
            val costs = costs(dmp)
            val personnel = costs.filter(::isPersonnelCost)
            val incomplete = costs.filter { amount(it) == null || it.text("currency_code") == null }.map { it.text("title") ?: "untitled cost" }
            val problems = if (costs.isEmpty()) {
                listOf("No 'cost' declared. Add the personnel costs (e.g. 'Data steward, 0.2 FTE') and other costs (e.g. 'Repository fees'), each with a value and currency_code.")
            } else listOfNotNull(
                "No personnel cost declared (e.g. 'Data steward salary', 'Research assistant, 3 person-months').".takeIf { personnel.isEmpty() },
                "No monetary cost other than personnel declared (e.g. storage, repository fees, hardware).".takeIf { personnel.size == costs.size },
                "Missing 'value' or 'currency_code' on: ${incomplete.joinToString()}.".takeIf { incomplete.isNotEmpty() },
            )
            if (problems.isEmpty()) SubjectCheck.Ok("${personnel.size} personnel and ${costs.size - personnel.size} other cost(s) declared with value and currency.")
            else SubjectCheck.Problem(problems.joinToString(" "))
        }

    // cost.co.2 - DMP States No Additional RDM Resources Are Required
    // Applies only when the DMP declares no cost above 0. It then has to say so explicitly: a cost entry with value 0,
    // or a statement such as 'no additional costs' in the DMP description or a cost title/description.
    // PASS: such a statement exists. FAIL: it does not, or there is no DMP.
    // INDETERMINATE: the DMP declares a cost above 0, so the test does not apply.
    fun noAdditionalResourcesStated(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("noAdditionalResourcesStated"),
            subjects = dmpSubject(maDMP).map { subject ->
                val budgeted = subject.json?.let(::costs).orEmpty().count { (amount(it) ?: 0.0) > 0 }
                if (budgeted == 0) subject
                else subject.copy(preset = SubjectCheck.Undetermined("Only applies when the DMP declares no costs; it declares $budgeted cost(s) above 0."))
            },
            noun = "DMP",
            requirement = "it states that no additional RDM resources are required",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DMP,
        ) { dmp ->
            val costs = costs(dmp)
            val zeroCost = costs.firstOrNull { amount(it) == 0.0 }
            val statement = (listOfNotNull(dmp.text("description")) + costs.flatMap { listOfNotNull(it.text("title"), it.text("description")) })
                .firstOrNull { NO_ADDITIONAL_RESOURCES.containsMatchIn(it) }
            when {
                zeroCost != null -> SubjectCheck.Ok("Cost '${zeroCost.text("title") ?: "untitled cost"}' is declared with value 0.")
                statement != null -> SubjectCheck.Ok("States that no additional resources are required: '$statement'.")
                else -> SubjectCheck.Problem("No costs are declared and the DMP does not state that none are needed. Say so explicitly (e.g. 'No additional costs are expected; storage is covered by the institution') or add a cost with value 0.")
            }
        }

    private fun costs(dmp: JsonObject): List<JsonObject> = dmp.array("cost").orEmpty().mapNotNull { it as? JsonObject }

    // DCS cost.value is a number; a string such as "100" is not accepted
    private fun amount(cost: JsonObject): Double? = (cost["value"] as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull

    private fun isPersonnelCost(cost: JsonObject): Boolean =
        listOfNotNull(cost.text("title"), cost.text("description")).any { PERSONNEL.containsMatchIn(it) }

    // Describes why the host is trusted (re3data entry or recognised certification), or null if it is not
    private fun trustedBy(host: JsonObject): String? {
        val re3data = host.array("host_id").orEmpty()
            .mapNotNull { it as? JsonObject }
            .firstOrNull { it.text("type")?.lowercase() == "re3data" }
            ?.text("identifier")
        val certification = host.text("certified_with")?.lowercase()?.takeIf { it in REPOSITORY_CERTIFICATIONS }
        return re3data?.let { "re3data '$it'" } ?: certification?.let { "certified with '$it'" }
    }

    // Runs [check] on the distribution's host, or reports a problem when no host is declared
    private fun withHost(distribution: JsonObject, check: (JsonObject) -> SubjectCheck): SubjectCheck =
        distribution.obj("host")?.let(check)
            ?: SubjectCheck.Problem("No host declared. Add the repository or storage system as 'host'.")

    private fun generatedBy(function: String) = "${this::class.qualifiedName}::$function"
}
