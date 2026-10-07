package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.EvaluationReport
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.PluginInfo
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import io.github.ostrails.dmpevaluatorservice.plugin.EvaluatorPlugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
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
        const val NO_DMP = "No 'dmp' object found in the maDMP."
        val PERSON_PID_TYPES = setOf("orcid", "isni")
        val ORGANISATION_PID_TYPES = setOf("ror", "grid", "isni")
    }

    override val functionMap: Map<String, (JsonObject, String, TestRecord) -> Evaluation> = mapOf(
        "datasetLicenseDeclared" to ::datasetLicenseDeclared,
        "distributionLicensePresent" to ::distributionLicensePresent,
        "storageLocationDeclared" to ::storageLocationDeclared,
        "distributionFormatSpecified" to ::distributionFormatSpecified,
        "preservationStatementPresent" to ::preservationStatementPresent,
        "ethicalIssuesStatusDeclared" to ::ethicalIssuesStatusDeclared,
        "securityMeasuresDeclared" to ::securityMeasuresDeclared,
        "sensitiveDataProtected" to ::sensitiveDataProtected,
        "repositoryPidSystemDeclared" to ::repositoryPidSystemDeclared,
        "datasetTypeSpecified" to ::datasetTypeSpecified,
        "distributionSizeSpecified" to ::distributionSizeSpecified,
        "metadataStandardDeclared" to ::metadataStandardDeclared,
        "metadataStandardInDmp" to ::metadataStandardInDmp,
        "qualityAssuranceDeclared" to ::qualityAssuranceDeclared,
        "backupFrequencyDeclared" to ::backupFrequencyDeclared,
        "contributorPidsDeclared" to ::contributorPidsDeclared,
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

    // data.info.cov.2 - every distribution specifies its file format(s)
    fun distributionFormatSpecified(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("distributionFormatSpecified"),
            subjects = distributionSubjects(datasetSubjects(maDMP)),
            noun = "distribution",
            requirement = "the file format is specified",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { distribution ->
            val formats = distribution.array("format").orEmpty().mapNotNull { it.textOrNull() }
            if (formats.isNotEmpty()) SubjectCheck.Ok("Format(s): ${formats.joinToString()}.")
            else SubjectCheck.Problem("No file format specified. Add the format, preferably as an IANA media type (e.g. 'text/csv').")
        }

    // repo.feas.2 - every dataset describes how it will be preserved long-term
    fun preservationStatementPresent(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("preservationStatementPresent"),
            subjects = datasetSubjects(maDMP),
            noun = "dataset",
            requirement = "a long-term preservation statement is provided",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { dataset ->
            if (dataset.text("preservation_statement") != null) SubjectCheck.Ok("Preservation statement provided.")
            else SubjectCheck.Problem("No 'preservation_statement'. Describe how and for how long the dataset will be preserved (e.g. repository retention period, integrity checks).")
        }

    // ethics.co.1 - the DMP states whether ethical issues exist ('unknown' cannot be assessed)
    fun ethicalIssuesStatusDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("ethicalIssuesStatusDeclared"),
            subjects = dmpSubject(maDMP),
            noun = "DMP",
            requirement = "the existence of ethical issues is declared as yes or no",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DMP,
        ) { dmp -> declaredStatus(dmp, "ethical_issues_exist") }

    // secur.co.1 - every dataset declares the security and privacy measures applied to it
    fun securityMeasuresDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("securityMeasuresDeclared"),
            subjects = datasetSubjects(maDMP),
            noun = "dataset",
            requirement = "security and privacy measures are declared",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { dataset ->
            val measures = securityMeasures(dataset)
            if (measures.isNotEmpty()) SubjectCheck.Ok("Security measures: ${measures.joinToString()}.")
            else SubjectCheck.Problem("No 'security_and_privacy' measures declared. Add each measure with a title (e.g. 'Encryption at rest').")
        }

    // store.comp.1 - datasets with personal or sensitive data declare security measures; datasets declaring
    // neither are skipped, and 'unknown' or missing flags cannot be assessed
    fun sensitiveDataProtected(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation {
        val flags = listOf("personal_data", "sensitive_data")
        val subjects = datasetSubjects(maDMP).mapNotNull { subject ->
            val dataset = subject.json ?: return@mapNotNull subject
            val values = flags.map { dataset.text(it) }
            when {
                "yes" in values -> subject
                values.all { it == "no" } -> null
                else -> subject.copy(preset = SubjectCheck.Undetermined(
                    "personal_data is '${values[0] ?: "missing"}' and sensitive_data is '${values[1] ?: "missing"}'. State 'yes' or 'no' for both."))
            }
        }
        return evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("sensitiveDataProtected"),
            subjects = subjects,
            noun = "dataset with personal or sensitive data",
            requirement = "security and privacy measures are declared",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = "No dataset declares personal or sensitive data, so there is nothing to assess.",
        ) { dataset ->
            val measures = securityMeasures(dataset)
            if (measures.isNotEmpty()) SubjectCheck.Ok("Contains personal or sensitive data; security measures: ${measures.joinToString()}.")
            else SubjectCheck.Problem("Contains personal or sensitive data but declares no 'security_and_privacy' measures. Describe how the data is protected (e.g. access control, encryption, pseudonymisation).")
        }
    }

    private fun securityMeasures(dataset: JsonObject): List<String> =
        dataset.array("security_and_privacy").orEmpty().mapNotNull { (it as? JsonObject)?.text("title") }

    // Checks a DCS yes/no/unknown field: yes/no is declared, 'unknown' cannot be assessed, anything else is a problem
    private fun declaredStatus(json: JsonObject, field: String): SubjectCheck =
        when (val value = json.text(field)) {
            "yes", "no" -> SubjectCheck.Ok("'$field' is '$value'.")
            "unknown" -> SubjectCheck.Undetermined("'$field' is 'unknown'. State 'yes' or 'no' once it has been assessed.")
            null -> SubjectCheck.Problem("'$field' is missing. Declare it as 'yes' or 'no'.")
            else -> SubjectCheck.Problem("'$field' has the invalid value '$value'. Allowed values: yes, no, unknown.")
        }

    // data.pid.cov.1 - the repository of every distribution declares the PID system(s) it supports
    fun repositoryPidSystemDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("repositoryPidSystemDeclared"),
            subjects = distributionSubjects(datasetSubjects(maDMP)),
            noun = "distribution",
            requirement = "the repository's persistent identifier system is declared",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { distribution ->
            val host = distribution.obj("host")
            val systems = host?.array("pid_system").orEmpty().mapNotNull { it.textOrNull() }
            when {
                host == null -> SubjectCheck.Problem("No host declared, so no PID system can be determined. Add the repository as 'host'.")
                systems.isEmpty() -> SubjectCheck.Problem("The host declares no 'pid_system'. Add the PID system(s) the repository assigns (e.g. 'doi', 'handle').")
                else -> SubjectCheck.Ok("PID system(s): ${systems.joinToString()}.")
            }
        }

    // data.info.cov.1 - every dataset specifies its type
    fun datasetTypeSpecified(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("datasetTypeSpecified"),
            subjects = datasetSubjects(maDMP),
            noun = "dataset",
            requirement = "the dataset type is specified",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { dataset ->
            val type = dataset.text("type")
            if (type != null) SubjectCheck.Ok("Type: '$type'.")
            else SubjectCheck.Problem("No 'type' specified. Add the dataset type, preferably from the DataCite or COAR vocabulary (e.g. 'Dataset', 'Software', 'raw data').")
        }

    // data.info.cov.3 - every distribution specifies its size in bytes
    fun distributionSizeSpecified(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("distributionSizeSpecified"),
            subjects = distributionSubjects(datasetSubjects(maDMP)),
            noun = "distribution",
            requirement = "the size in bytes is specified",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { distribution ->
            val raw = distribution["byte_size"]
            val size = (raw as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull
            when {
                raw == null -> SubjectCheck.Problem("No 'byte_size' specified. Add the (expected) size of the distribution in bytes.")
                size == null || size < 0 -> SubjectCheck.Problem("'byte_size' must be a non-negative whole number of bytes, found '$raw'.")
                else -> SubjectCheck.Ok("Size: $size bytes.")
            }
        }

    // meta.stand.comp.1 - every dataset names the metadata standard(s) used to describe it
    fun metadataStandardDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("metadataStandardDeclared"),
            subjects = datasetSubjects(maDMP),
            noun = "dataset",
            requirement = "a metadata standard is declared",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { dataset ->
            val standards = metadataStandards(dataset)
            if (standards.isNotEmpty()) SubjectCheck.Ok("Metadata standard(s): ${standards.joinToString()}.")
            else SubjectCheck.Problem("No metadata standard declared. Add a 'metadata' entry with a 'metadata_standard_id' (e.g. https://schema.datacite.org/ for DataCite).")
        }

    // data.exteresource.co.2 - the DMP names at least one metadata standard (on any dataset)
    fun metadataStandardInDmp(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("metadataStandardInDmp"),
            subjects = dmpSubject(maDMP),
            noun = "DMP",
            requirement = "at least one metadata standard is specified",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DMP,
        ) { dmp ->
            val standards = dmp.array("dataset").orEmpty().flatMap { (it as? JsonObject)?.let(::metadataStandards).orEmpty() }.distinct()
            if (standards.isNotEmpty()) SubjectCheck.Ok("Metadata standard(s): ${standards.joinToString()}.")
            else SubjectCheck.Problem("No metadata standard is specified anywhere in the DMP. Add a 'metadata' entry with a 'metadata_standard_id' to the datasets.")
        }

    private fun metadataStandards(dataset: JsonObject): List<String> =
        dataset.array("metadata").orEmpty().mapNotNull { (it as? JsonObject)?.obj("metadata_standard_id")?.text("identifier") }

    // qc.qual.1 - every dataset states the quality control methods applied to it
    fun qualityAssuranceDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("qualityAssuranceDeclared"),
            subjects = datasetSubjects(maDMP),
            noun = "dataset",
            requirement = "quality control methods are stated",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { dataset ->
            val methods = dataset.array("data_quality_assurance").orEmpty().mapNotNull { it.textOrNull() }
            if (methods.isNotEmpty()) SubjectCheck.Ok("${methods.size} quality assurance statement(s).")
            else SubjectCheck.Problem("No 'data_quality_assurance' stated. Describe how data quality is ensured (e.g. instrument calibration, validation, peer review).")
        }

    // store.co.1 - the host of every distribution states how often it backs up the data
    fun backupFrequencyDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("backupFrequencyDeclared"),
            subjects = distributionSubjects(datasetSubjects(maDMP)),
            noun = "distribution",
            requirement = "the backup frequency of the host is stated",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { distribution ->
            withHost(distribution) { host ->
                val frequency = host.text("backup_frequency")
                if (frequency != null) SubjectCheck.Ok("Backup frequency: '$frequency'.")
                else SubjectCheck.Problem("The host states no 'backup_frequency'. Add how often the data is backed up (e.g. 'daily').")
            }
        }

    // role.pid.co.1 - every contributor has a person PID (orcid/isni) and every declared affiliation an
    // organisation PID (ror/grid/isni, DCS 1.3); contributors without affiliations are not penalised
    fun contributorPidsDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("contributorPidsDeclared"),
            subjects = contributorSubjects(maDMP),
            noun = "contributor",
            requirement = "the contributor and their organisations are identified by persistent identifiers",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = "No contributors found in the maDMP.",
        ) { contributor ->
            val problems = mutableListOf<String>()
            val id = contributor.obj("contributor_id")
            val idType = id?.text("type")?.lowercase()
            when {
                id?.text("identifier") == null -> problems.add("No 'contributor_id'. Add the person's ORCID.")
                idType !in PERSON_PID_TYPES -> problems.add("The contributor_id type is '${idType ?: "missing"}', which is not a persistent identifier. Use an ORCID or ISNI.")
            }
            val affiliations = contributor.array("affiliation").orEmpty()
            affiliations.forEachIndexed { index, element ->
                val affiliation = element as? JsonObject
                val name = affiliation?.text("name") ?: "affiliation ${index + 1}"
                val affiliationId = affiliation?.obj("affiliation_id")
                val affiliationType = affiliationId?.text("type")?.lowercase()
                if (affiliationId?.text("identifier") == null || affiliationType !in ORGANISATION_PID_TYPES) {
                    problems.add("Affiliation '$name' has no organisation PID. Add an 'affiliation_id' of type ror, grid or isni.")
                }
            }
            if (problems.isEmpty()) SubjectCheck.Ok("Identified by $idType; ${affiliations.size} affiliation(s) with organisation PIDs.")
            else SubjectCheck.Problem(problems.joinToString(" "))
        }

    // Runs [check] on the distribution's host, or reports a problem when no host is declared
    private fun withHost(distribution: JsonObject, check: (JsonObject) -> SubjectCheck): SubjectCheck =
        distribution.obj("host")?.let(check)
            ?: SubjectCheck.Problem("No host declared. Add the repository or storage system as 'host'.")

    private fun hasLicenseRef(distribution: JsonObject): Boolean =
        distribution.array("license").orEmpty().any { (it as? JsonObject)?.text("license_ref") != null }

    private fun generatedBy(function: String) = "${this::class.qualifiedName}::$function"
}
