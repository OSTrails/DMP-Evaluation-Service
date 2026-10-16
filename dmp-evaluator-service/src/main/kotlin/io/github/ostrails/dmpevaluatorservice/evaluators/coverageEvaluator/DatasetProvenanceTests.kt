package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import kotlinx.serialization.json.JsonObject

// Group A - new and reused datasets. Registered in DCSCoverageEvaluator.functionMap; only edit this file.
// Use the helpers in CoverageSupport.kt (datasetSubjects(maDMP, DatasetScope.NEW / REUSED), distributionSubjects,
// evaluateSubjects, SubjectCheck) and see DCSCoverageEvaluator for examples. Unit-test each function with
// CoverageTestSupport.run("<functionName>", dmp(...)). Above each function, state when it returns PASS / FAIL /
// INDETERMINATE / NOT_APPLICABLE.
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
    fun reusedDatasetLicenseDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("data.reused.co.3 not implemented yet")

    // data.new.2 - New Dataset Collection or Creation Method Declared
    fun newDatasetCollectionMethodDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("data.new.2 not implemented yet")

    // data.new.3 - New Dataset Has Declared Access Conditions
    fun newDatasetAccessDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("data.new.3 not implemented yet")

    // data.new.1 - New Dataset Declared in the DMP
    fun newDatasetDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("data.new.1 not implemented yet")

    // data.new.4 - New Dataset Has Sufficient Metadata
    fun newDatasetMetadataSufficient(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("data.new.4 not implemented yet")

    // data.reused.co.2 - Reused Dataset Has a Persistent Identifier
    fun reusedDatasetPidDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("data.reused.co.2 not implemented yet")

    // data.reused.co.4 - Reused Dataset Has Distribution Information
    fun reusedDatasetDistributionDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("data.reused.co.4 not implemented yet")

    // data.reused.co.6 - Reused Dataset Contains Personal Data
    fun reusedDatasetPersonalDataDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("data.reused.co.6 not implemented yet")

    // data.reused.co.7 - Reused Dataset Contains Sensitive Data
    fun reusedDatasetSensitiveDataDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("data.reused.co.7 not implemented yet")

    // data.reused.co.8 - Reused Dataset Has an Access URL
    fun reusedDatasetAccessUrlDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("data.reused.co.8 not implemented yet")

    private fun generatedBy(function: String) = "${this::class.qualifiedName}::$function"
}
