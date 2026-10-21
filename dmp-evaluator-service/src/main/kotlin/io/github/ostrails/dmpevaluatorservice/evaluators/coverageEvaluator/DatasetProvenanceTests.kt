package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// Group A - new and reused datasets. Registered in DCSCoverageEvaluator.functionMap; only edit this file.
// Use the helpers in CoverageSupport.kt (datasetSubjects(maDMP, DatasetScope.NEW / REUSED), distributionSubjects,
// evaluateSubjects, SubjectCheck) and see DCSCoverageEvaluator for examples. Unit-test each function with
// CoverageTestSupport.run("<functionName>", dmp(...)). Above each function, state when it returns PASS / FAIL /
// INDETERMINATE (also used when the test does not apply, see DCSCoverageEvaluator.noEthicalIssuesJustified).
// A dataset is reused when is_reused is true and new otherwise, so DCS 1.2 datasets without the flag count as new.
object DatasetProvenanceTests {

    val functions: Map<String, (JsonObject, String, TestRecord) -> Evaluation> = mapOf(
        "reusedDatasetLicenseDeclared" to ::reusedDatasetLicenseDeclared,
        "newDatasetCollectionMethodDeclared" to ::newDatasetCollectionMethodDeclared,
        "newDatasetAccessDeclared" to ::newDatasetAccessDeclared,
        "newDatasetDeclared" to ::newDatasetDeclared,
        "newDatasetMetadataSufficient" to ::newDatasetMetadataSufficient,
        "reusedDatasetPidDeclared" to ::reusedDatasetPidDeclared,
        "reusedDatasetDistributionDeclared" to ::reusedDatasetDistributionDeclared,
        "reusedDatasetPersonalDataDeclared" to ::reusedDatasetPersonalDataDeclared,
        "reusedDatasetSensitiveDataDeclared" to ::reusedDatasetSensitiveDataDeclared,
        "reusedDatasetAccessUrlDeclared" to ::reusedDatasetAccessUrlDeclared,
    )

    // data.reused.co.3 - Reused Dataset Has a Declared License
    // Every reused dataset declares a license on its distributions, and each license has a license_ref and a start_date.
    // PASS: every reused dataset has complete licenses. FAIL: a reused dataset has no license or an incomplete one.
    // INDETERMINATE: the DMP has no reused datasets (the test does not apply).
    fun reusedDatasetLicenseDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("reusedDatasetLicenseDeclared"),
            subjects = datasetSubjects(maDMP, DatasetScope.REUSED),
            noun = "reused dataset",
            requirement = "a license with license_ref and start_date is declared",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = NO_REUSED_DATASETS,
        ) { dataset ->
            val licenses = dataset.array("distribution").orEmpty().flatMap { (it as? JsonObject)?.array("license").orEmpty() }
            val issues = entryIssues(licenses, "license", "license_ref", "start_date")
            when {
                licenses.isEmpty() -> SubjectCheck.Problem("No license declared on any distribution. Add the license under which the reused data may be used, with its license_ref and start_date.")
                issues.isNotEmpty() -> SubjectCheck.Problem(issues.joinToString(" "))
                else -> SubjectCheck.Ok("License(s): ${licenses.mapNotNull { (it as? JsonObject)?.text("license_ref") }.joinToString()}.")
            }
        }

    // data.new.2 - New Dataset Collection or Creation Method Declared
    // Every new dataset declares the technical resources used to collect or create it: each technical_resource has a
    // name and a description. DCS 1.2 has no identifier for technical resources, so one is optional; if given
    // (technical_resource_id or id), it needs an identifier and a type.
    // PASS: every new dataset has complete technical resources. FAIL: a new dataset has none, or one is incomplete.
    // INDETERMINATE: the DMP has no new datasets (the test does not apply).
    fun newDatasetCollectionMethodDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("newDatasetCollectionMethodDeclared"),
            subjects = datasetSubjects(maDMP, DatasetScope.NEW),
            noun = "new dataset",
            requirement = "the technical resources used to collect or create it are described",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = NO_NEW_DATASETS,
        ) { dataset ->
            val resources = dataset.array("technical_resource").orEmpty()
            val issues = entryIssues(resources, "technical_resource", "name", "description") +
                resources.mapIndexedNotNull { index, element ->
                    val missing = technicalResourceIds(element as? JsonObject).flatMap { id ->
                        if (id == null) listOf("identifier", "type") else missingFields(id, "identifier", "type")
                    }.distinct()
                    if (missing.isEmpty()) null else "technical_resource ${index + 1} has an identifier without ${missing.joinToString(" and ")}."
                }
            when {
                resources.isEmpty() -> SubjectCheck.Problem("No 'technical_resource' declared. Describe the instruments, software or methods used to collect or create the data, with a name and a description.")
                issues.isNotEmpty() -> SubjectCheck.Problem(issues.joinToString(" "))
                else -> SubjectCheck.Ok("Technical resource(s): ${resources.mapNotNull { (it as? JsonObject)?.text("name") }.joinToString()}.")
            }
        }

    // data.new.3 - New Dataset Has Declared Access Conditions
    // Every distribution of a new dataset declares its data_access (open, restricted or closed; 'shared' is
    // deprecated in DCS 1.3 but still accepted) and a license with a license_ref, which carries its rights.
    // PASS: all declare both. FAIL: either is missing or data_access is invalid, or a new dataset has no distributions.
    // INDETERMINATE: the DMP has no new datasets (the test does not apply).
    fun newDatasetAccessDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("newDatasetAccessDeclared"),
            subjects = distributionSubjects(datasetSubjects(maDMP, DatasetScope.NEW)),
            noun = "distribution of a new dataset",
            requirement = "the access mode and the license are declared",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = NO_NEW_DATASETS,
        ) { distribution ->
            val access = distribution.text("data_access")
            val problems = listOfNotNull(
                when (access) {
                    null -> "No 'data_access' declared. State whether access is open, restricted or closed."
                    in DATA_ACCESS_VALUES -> null
                    else -> "'data_access' has the invalid value '$access'. Allowed values: open, restricted, closed."
                },
                if (hasLicenseRef(distribution)) null
                else "No license with a 'license_ref' declared. Add the license that states the terms of use.",
            )
            if (problems.isEmpty()) SubjectCheck.Ok("Data access is '$access' and a license is declared.")
            else SubjectCheck.Problem(problems.joinToString(" "))
        }

    // data.new.1 - New Dataset Declared in the DMP
    // The DMP declares at least one new dataset (is_reused is false or not set).
    // PASS: at least one dataset is new. FAIL: there are no datasets, all datasets are reused, or there is no DMP.
    fun newDatasetDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("newDatasetDeclared"),
            subjects = dmpSubject(maDMP),
            noun = "DMP",
            requirement = "at least one new dataset is declared",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DMP,
        ) { dmp ->
            val datasets = dmp.array("dataset").orEmpty().mapNotNull { it as? JsonObject }
            val newCount = datasets.count { isReused(it) != true }
            when {
                datasets.isEmpty() -> SubjectCheck.Problem("No datasets declared. Add the data the project will collect or produce.")
                newCount == 0 -> SubjectCheck.Problem("All ${datasets.size} dataset(s) are reused (is_reused: true). Declare the new data the project will collect or produce.")
                else -> SubjectCheck.Ok("$newCount of ${datasets.size} dataset(s) are new.")
            }
        }

    // data.new.4 - New Dataset Has Sufficient Metadata
    // Every new dataset declares metadata, and each metadata entry has a description, a language and a
    // metadata_standard_id with an identifier and a type.
    // PASS: every new dataset has complete metadata. FAIL: a new dataset has none, or an entry is incomplete.
    // INDETERMINATE: the DMP has no new datasets (the test does not apply).
    fun newDatasetMetadataSufficient(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("newDatasetMetadataSufficient"),
            subjects = datasetSubjects(maDMP, DatasetScope.NEW),
            noun = "new dataset",
            requirement = "metadata with description, language and metadata standard is declared",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = NO_NEW_DATASETS,
        ) { dataset ->
            val metadata = dataset.array("metadata").orEmpty()
            val issues = entryIssues(metadata, "metadata", "description", "language",
                "metadata_standard_id.identifier", "metadata_standard_id.type")
            when {
                metadata.isEmpty() -> SubjectCheck.Problem("No 'metadata' declared. Add the metadata standard used to describe the dataset, with a description, a language and a metadata_standard_id (identifier and type).")
                issues.isNotEmpty() -> SubjectCheck.Problem(issues.joinToString(" "))
                else -> SubjectCheck.Ok("Metadata standard(s): ${metadata.mapNotNull { (it as? JsonObject)?.obj("metadata_standard_id")?.text("identifier") }.joinToString()}.")
            }
        }

    // data.reused.co.2 - Reused Dataset Has a Persistent Identifier
    // Every reused dataset is identified by a dataset_id with an identifier and a type, so the source can be cited.
    // PASS: every reused dataset has both. FAIL: a reused dataset is missing either.
    // INDETERMINATE: the DMP has no reused datasets (the test does not apply).
    fun reusedDatasetPidDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("reusedDatasetPidDeclared"),
            subjects = datasetSubjects(maDMP, DatasetScope.REUSED),
            noun = "reused dataset",
            requirement = "a persistent identifier with its type is declared",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = NO_REUSED_DATASETS,
        ) { dataset ->
            val missing = missingFields(dataset, "dataset_id.identifier", "dataset_id.type")
            val id = dataset.obj("dataset_id")
            if (missing.isEmpty()) SubjectCheck.Ok("Identifier: ${id?.text("identifier")} (${id?.text("type")}).")
            else SubjectCheck.Problem("Missing ${missing.joinToString(" and ")}. Cite the source dataset with its persistent identifier (e.g. a DOI) and the identifier type.")
        }

    // data.reused.co.4 - Reused Dataset Has Distribution Information
    // Every reused dataset has distributions, and each one has a title and an access_url or download_url.
    // PASS: all distributions have both. FAIL: one is missing either, or a reused dataset has no distributions.
    // INDETERMINATE: the DMP has no reused datasets (the test does not apply).
    fun reusedDatasetDistributionDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("reusedDatasetDistributionDeclared"),
            subjects = distributionSubjects(datasetSubjects(maDMP, DatasetScope.REUSED)),
            noun = "distribution of a reused dataset",
            requirement = "a title and an access location are declared",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = NO_REUSED_DATASETS,
        ) { distribution ->
            val missing = listOfNotNull(
                "a 'title'".takeIf { distribution.text("title") == null },
                "an 'access_url' or 'download_url'".takeIf { distribution.text("access_url") == null && distribution.text("download_url") == null },
            )
            if (missing.isEmpty()) SubjectCheck.Ok("Title and access location declared.")
            else SubjectCheck.Problem("The distribution has no ${missing.joinToString(" and no ")}. State its title and where the reused data can be accessed.")
        }

    // data.reused.co.6 - Reused Dataset Contains Personal Data
    // Every reused dataset declares personal_data as yes, no or unknown.
    // PASS: every reused dataset declares it. FAIL: the flag is missing or invalid.
    // INDETERMINATE: the DMP has no reused datasets (the test does not apply).
    fun reusedDatasetPersonalDataDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("reusedDatasetPersonalDataDeclared"),
            subjects = datasetSubjects(maDMP, DatasetScope.REUSED),
            noun = "reused dataset",
            requirement = "whether it contains personal data is declared",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = NO_REUSED_DATASETS,
        ) { dataset -> declaredFlag(dataset, "personal_data") }

    // data.reused.co.7 - Reused Dataset Contains Sensitive Data
    // Every reused dataset declares sensitive_data as yes, no or unknown.
    // PASS: every reused dataset declares it. FAIL: the flag is missing or invalid.
    // INDETERMINATE: the DMP has no reused datasets (the test does not apply).
    fun reusedDatasetSensitiveDataDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("reusedDatasetSensitiveDataDeclared"),
            subjects = datasetSubjects(maDMP, DatasetScope.REUSED),
            noun = "reused dataset",
            requirement = "whether it contains sensitive data is declared",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = NO_REUSED_DATASETS,
        ) { dataset -> declaredFlag(dataset, "sensitive_data") }

    // data.reused.co.8 - Reused Dataset Has an Access URL
    // Every reused dataset (is_reused: true) has at least one distribution with an access_url, so it can be found.
    // PASS: every reused dataset has one. FAIL: a reused dataset has none.
    // INDETERMINATE: the DMP has no reused datasets (the test does not apply).
    fun reusedDatasetAccessUrlDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("reusedDatasetAccessUrlDeclared"),
            subjects = datasetSubjects(maDMP, DatasetScope.REUSED),
            noun = "reused dataset",
            requirement = "a distribution has an access URL",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = NO_REUSED_DATASETS,
        ) { dataset ->
            val url = dataset.array("distribution").orEmpty().firstNotNullOfOrNull { (it as? JsonObject)?.text("access_url") }
            if (url != null) SubjectCheck.Ok("Access URL: '$url'.")
            else SubjectCheck.Problem("No distribution has an 'access_url'. Add the URL of the landing page where the reused data can be accessed.")
        }

    private const val NO_DMP = "No 'dmp' object found in the maDMP."
    private const val NO_NEW_DATASETS = "The DMP declares no new datasets, so there is nothing to assess."
    private const val NO_REUSED_DATASETS = "The DMP declares no reused datasets (is_reused: true), so there is nothing to assess."
    private val DATA_ACCESS_VALUES = setOf("open", "restricted", "closed", "shared")
    private val FLAG_VALUES = setOf("yes", "no", "unknown")

    // Checks a DCS yes/no/unknown field; 'unknown' counts as declared
    private fun declaredFlag(dataset: JsonObject, field: String): SubjectCheck =
        when (val value = dataset.text(field)) {
            in FLAG_VALUES -> SubjectCheck.Ok("'$field' is '$value'.")
            null -> SubjectCheck.Problem("'$field' is missing. Declare it as 'yes', 'no' or 'unknown'.")
            else -> SubjectCheck.Problem("'$field' has the invalid value '$value'. Allowed values: yes, no, unknown.")
        }

    // Fields that are missing or blank; a dotted field such as "dataset_id.type" is looked up in the nested object
    private fun missingFields(json: JsonObject, vararg fields: String): List<String> =
        fields.filter { field ->
            val keys = field.split(".")
            val parent = keys.dropLast(1).fold<String, JsonObject?>(json) { obj, key -> obj?.obj(key) }
            parent?.text(keys.last()) == null
        }

    // One message per entry of [entries] that is not an object or misses any of [fields]
    private fun entryIssues(entries: List<JsonElement>, name: String, vararg fields: String): List<String> =
        entries.mapIndexedNotNull { index, element ->
            val missing = (element as? JsonObject)?.let { missingFields(it, *fields) } ?: fields.toList()
            if (missing.isEmpty()) null else "$name ${index + 1} is missing ${missing.joinToString()}."
        }

    // Identifiers declared on a technical resource, as one object or an array; null for an entry that is not an object
    private fun technicalResourceIds(resource: JsonObject?): List<JsonObject?> =
        listOfNotNull(resource?.get("technical_resource_id"), resource?.get("id"))
            .flatMap { (it as? JsonArray)?.toList() ?: listOf(it) }
            .map { it as? JsonObject }

    private fun hasLicenseRef(distribution: JsonObject): Boolean =
        distribution.array("license").orEmpty().any { (it as? JsonObject)?.text("license_ref") != null }

    private fun generatedBy(function: String) = "${this::class.qualifiedName}::$function"
}
