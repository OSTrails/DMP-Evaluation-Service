package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import kotlinx.serialization.json.JsonObject

// Group C - repository, storage and cost. Registered in DCSCoverageEvaluator.functionMap; only edit this file.
// Use the helpers in CoverageSupport.kt (datasetSubjects, distributionSubjects, dmpSubject, evaluateSubjects,
// SubjectCheck) and see DCSCoverageEvaluator for examples. Unit-test each function with
// CoverageTestSupport.run("<functionName>", dmp(...)). Above each function, state when it returns PASS / FAIL /
// INDETERMINATE / NOT_APPLICABLE.
object RepositoryStorageTests {

    val functions: Map<String, (JsonObject, String, TestRecord) -> Evaluation> = mapOf(
        "repositoryFairConformant" to ::repositoryFairConformant,
        "secureTrustedStorageUsed" to ::secureTrustedStorageUsed,
        "backupStrategyDeclared" to ::backupStrategyDeclared,
        "readmeReferenced" to ::readmeReferenced,
        "personnelAndMonetaryBudgetDeclared" to ::personnelAndMonetaryBudgetDeclared,
        "noAdditionalResourcesStated" to ::noAdditionalResourcesStated,
    )

    // repo.comp.2 - Repository Conforms with FAIR Data Principles
    fun repositoryFairConformant(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("repo.comp.2 not implemented yet")

    // store.cov.2 - Use of Secure Storage for the Dataset in a Trusted Repository
    fun secureTrustedStorageUsed(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("store.cov.2 not implemented yet")

    // repo.co.4 - Verification of Back-up Strategy
    fun backupStrategyDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("repo.co.4 not implemented yet")

    // meta.qual.2 - ReadMe File Reference
    fun readmeReferenced(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("meta.qual.2 not implemented yet")

    // cost.co.1 - DMP Includes a Budget for Personnel and Monetary Resources
    fun personnelAndMonetaryBudgetDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("cost.co.1 not implemented yet")

    // cost.co.2 - DMP States No Additional RDM Resources Are Required
    fun noAdditionalResourcesStated(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        TODO("cost.co.2 not implemented yet")

    private fun generatedBy(function: String) = "${this::class.qualifiedName}::$function"
}
