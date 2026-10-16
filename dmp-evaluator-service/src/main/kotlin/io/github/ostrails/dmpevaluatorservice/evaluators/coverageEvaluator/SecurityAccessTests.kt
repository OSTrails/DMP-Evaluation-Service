package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import kotlinx.serialization.json.JsonObject

// Group B - security, sensitive data and access. Registered in DCSCoverageEvaluator.functionMap; only edit this file.
// Use the helpers in CoverageSupport.kt (datasetSubjects, distributionSubjects, contributorSubjects, dmpSubject,
// evaluateSubjects, SubjectCheck) and see DCSCoverageEvaluator for examples. Unit-test each function with
// CoverageTestSupport.run("<functionName>", dmp(...)). Above each function, state when it returns PASS / FAIL /
// INDETERMINATE / NOT_APPLICABLE.
object SecurityAccessTests {

    val functions: Map<String, (JsonObject, String, TestRecord) -> Evaluation> = mapOf(
        "sensitiveDataProtectionDescribed" to ::sensitiveDataProtectionDescribed,
        "gdprEthicsComplianceDeclared" to ::gdprEthicsComplianceDeclared,
        "accessControlDescribed" to ::accessControlDescribed,
        "sensitiveDataHandlingMethodDeclared" to ::sensitiveDataHandlingMethodDeclared,
        "dataRestrictionsExplained" to ::dataRestrictionsExplained,
        "embargoDeclared" to ::embargoDeclared,
        "dataOwnershipRoleDeclared" to ::dataOwnershipRoleDeclared,
    )

    // secur.co.2 - Sensitive Data Protection Description
    fun sensitiveDataProtectionDescribed(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("secur.co.2 not implemented yet")

    // secur.comp.1 - GDPR and Ethics Compliance
    fun gdprEthicsComplianceDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("secur.comp.1 not implemented yet")

    // secur.co.4 - Access Control and User Management
    fun accessControlDescribed(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("secur.co.4 not implemented yet")

    // sens.secure.co.1 - Sensitive Data Using Method
    fun sensitiveDataHandlingMethodDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("sens.secure.co.1 not implemented yet")

    // data.shar.co.2 - Data Restrictions Reference
    fun dataRestrictionsExplained(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("data.shar.co.2 not implemented yet")

    // data.shar.comp.2 - Embargo Declared in the DMP or Repository
    fun embargoDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("data.shar.comp.2 not implemented yet")

    // own.co.2 - Data Ownership Role Declared
    fun dataOwnershipRoleDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("own.co.2 not implemented yet")

    private fun generatedBy(function: String) = "${this::class.qualifiedName}::$function"
}
