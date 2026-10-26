package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import java.time.format.DateTimeParseException

// Group B - security, sensitive data and access. Registered in DCSCoverageEvaluator.functionMap; only edit this file.
// Use the helpers in CoverageSupport.kt (datasetSubjects, distributionSubjects, contributorSubjects, dmpSubject,
// evaluateSubjects, SubjectCheck) and see DCSCoverageEvaluator for examples. Unit-test each function with
// CoverageTestSupport.run("<functionName>", dmp(...)). Above each function, state when it returns PASS / FAIL /
// INDETERMINATE (also used when the test does not apply, see DCSCoverageEvaluator.noEthicalIssuesJustified).
// DCS has no fields for access control, user roles or GDPR, so those are found by keywords (case-insensitive) in
// security_and_privacy.description.
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

    private val ACCESS_KEYWORDS = listOf(
        "access control", "access rights", "authoris", "authoriz", "authentication", "permission",
        "restricted access", "institutional storage", "login", "vpn",
    )
    private val ROLE_KEYWORDS = listOf("role", "permission level", "user group", "privilege", "rbac", "user management")
    private val GDPR_KEYWORDS = listOf("gdpr", "general data protection regulation", "data protection", "dsgvo", "privacy regulation")

    // secur.co.2 - Sensitive Data Protection Description
    // Every dataset with sensitive_data 'yes' has a security_and_privacy description that mentions access rights
    // management (keywords: access control, access rights, authorisation, authentication, permission, restricted
    // access, institutional storage, login, VPN).
    // PASS: every sensitive dataset mentions it. FAIL: a sensitive dataset has no description or no such reference.
    // INDETERMINATE: a dataset's sensitive_data is 'unknown' or missing, or no dataset contains sensitive data.
    fun sensitiveDataProtectionDescribed(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("sensitiveDataProtectionDescribed"),
            subjects = flaggedDatasets(maDMP, "sensitive_data"),
            noun = "dataset with sensitive data",
            requirement = "the security description addresses access rights management",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = NO_SENSITIVE_DATA,
        ) { dataset ->
            val texts = descriptions(dataset)
            val keyword = matchedKeyword(texts, ACCESS_KEYWORDS)
            when {
                texts.isEmpty() -> SubjectCheck.Problem(NO_DESCRIPTION)
                keyword == null -> SubjectCheck.Problem("The security and privacy description does not mention access rights management. Explain who may access the data and how (e.g. access control on institutional storage).")
                else -> SubjectCheck.Ok("Access rights management is described ('$keyword').")
            }
        }

    // secur.comp.1 - GDPR and Ethics Compliance
    // Every dataset with personal_data 'yes' references data protection law in its security_and_privacy description
    // (keywords: GDPR, General Data Protection Regulation, data protection, DSGVO, privacy regulation), or the DMP
    // references an ethical review with a non-empty ethical_issues_report.
    // PASS: every personal dataset is covered by either. FAIL: a personal dataset is covered by neither.
    // INDETERMINATE: a dataset's personal_data is 'unknown' or missing, or no dataset contains personal data.
    fun gdprEthicsComplianceDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation {
        val ethicsReport = maDMP.obj("dmp")?.text("ethical_issues_report")
        return evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("gdprEthicsComplianceDeclared"),
            subjects = flaggedDatasets(maDMP, "personal_data"),
            noun = "dataset with personal data",
            requirement = "GDPR compliance or an ethical review is referenced",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = "No dataset declares personal data, so there is nothing to assess.",
        ) { dataset ->
            val keyword = matchedKeyword(descriptions(dataset), GDPR_KEYWORDS)
            when {
                keyword != null -> SubjectCheck.Ok("Data protection law is referenced ('$keyword').")
                ethicsReport != null -> SubjectCheck.Ok("An ethical review is referenced: '$ethicsReport'.")
                else -> SubjectCheck.Problem("Neither the security and privacy description references GDPR or data protection law, nor does the DMP reference an ethics report ('ethical_issues_report'). State how the processing of personal data complies with the GDPR.")
            }
        }
    }

    // secur.co.4 - Access Control and User Management
    // Every dataset with sensitive_data 'yes' has a security_and_privacy description that mentions both access
    // control (the secur.co.2 keywords) and user roles or permissions (keywords: role, permission level, user group,
    // privilege, RBAC, user management).
    // PASS: every sensitive dataset mentions both. FAIL: a sensitive dataset has no description or misses either.
    // INDETERMINATE: a dataset's sensitive_data is 'unknown' or missing, or no dataset contains sensitive data.
    fun accessControlDescribed(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("accessControlDescribed"),
            subjects = flaggedDatasets(maDMP, "sensitive_data"),
            noun = "dataset with sensitive data",
            requirement = "access control and user roles or permissions are described",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = NO_SENSITIVE_DATA,
        ) { dataset ->
            val texts = descriptions(dataset)
            val access = matchedKeyword(texts, ACCESS_KEYWORDS)
            val role = matchedKeyword(texts, ROLE_KEYWORDS)
            val missing = listOfNotNull("access control".takeIf { access == null }, "user roles or permissions".takeIf { role == null })
            when {
                texts.isEmpty() -> SubjectCheck.Problem(NO_DESCRIPTION)
                missing.isNotEmpty() -> SubjectCheck.Problem("The security and privacy description does not mention ${missing.joinToString(" or ")}. Describe how access is controlled and which user roles or permission levels exist.")
                else -> SubjectCheck.Ok("Access control ('$access') and user roles ('$role') are described.")
            }
        }

    // sens.secure.co.1 - Sensitive Data Using Method
    // Every dataset with sensitive_data 'yes' describes the method used to protect it in a non-empty
    // security_and_privacy description.
    // PASS: every sensitive dataset has one. FAIL: a sensitive dataset has no description.
    // INDETERMINATE: a dataset's sensitive_data is 'unknown' or missing, or no dataset contains sensitive data.
    fun sensitiveDataHandlingMethodDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("sensitiveDataHandlingMethodDeclared"),
            subjects = flaggedDatasets(maDMP, "sensitive_data"),
            noun = "dataset with sensitive data",
            requirement = "the protection method is described",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = NO_SENSITIVE_DATA,
        ) { dataset ->
            if (descriptions(dataset).isNotEmpty()) SubjectCheck.Ok("The protection method is described.")
            else SubjectCheck.Problem(NO_DESCRIPTION)
        }

    // data.shar.co.2 - Data Restrictions Reference
    // DCS has no 'rights' field, so every distribution must state its access restrictions through a data_access
    // value (open, restricted, closed or the deprecated shared) and a license with a license_ref.
    // PASS: every distribution declares both. FAIL: either is missing or data_access is invalid, a dataset has no
    // distributions, or there are no datasets.
    fun dataRestrictionsExplained(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("dataRestrictionsExplained"),
            subjects = distributionSubjects(datasetSubjects(maDMP)),
            noun = "distribution",
            requirement = "the access mode and the license are declared",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DATASETS,
        ) { distribution ->
            val access = distribution.text("data_access")
            val problems = listOfNotNull(
                when (access) {
                    null -> "No 'data_access' declared. State whether access is open, restricted or closed."
                    in DATA_ACCESS_VALUES -> null
                    else -> "'data_access' has the invalid value '$access'. Allowed values: open, restricted, closed."
                },
                if (licenseRefs(distribution).isNotEmpty()) null
                else "No license with a 'license_ref' declared. Add the license that states the terms of use.",
            )
            if (problems.isEmpty()) SubjectCheck.Ok("Data access is '$access' under ${licenseRefs(distribution).joinToString()}.")
            else SubjectCheck.Problem(problems.joinToString(" "))
        }

    // data.shar.comp.2 - Embargo Declared in the DMP or Repository
    // Horizon Europe does not allow embargoes, so no distribution may have a license that starts after the DMP was
    // created (dmp.created, or today if it is missing). DCS hosts have no embargo field, so only licenses are checked.
    // PASS: no license of the dataset starts later. FAIL: a license start_date is later (an embargo).
    // INDETERMINATE: a start_date is not a date. A distribution without license or start_date has no embargo.
    // The test is not applicable without datasets (INDETERMINATE).
    fun embargoDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation {
        val created = maDMP.obj("dmp")?.text("created")?.let(::parseDate)
        val reference = created ?: LocalDate.now()
        val referenceLabel = if (created != null) "the DMP creation date ($created)" else "today ($reference)"
        return evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("embargoDeclared"),
            subjects = datasetSubjects(maDMP),
            noun = "dataset",
            requirement = "no license starts after $referenceLabel",
            whenEmpty = ResultTestEnum.INDETERMINATE,
            emptyMessage = NO_DATASETS,
        ) { dataset ->
            val startDates = dataset.array("distribution").orEmpty()
                .flatMap { (it as? JsonObject)?.array("license").orEmpty() }
                .mapNotNull { (it as? JsonObject)?.text("start_date") }
            val unreadable = startDates.filter { parseDate(it) == null }
            val embargoed = startDates.filter { date -> parseDate(date)?.isAfter(reference) == true }
            when {
                embargoed.isNotEmpty() -> SubjectCheck.Problem("A license starts on ${embargoed.joinToString()}, after $referenceLabel, which is an embargo. Make the data available under its license when it is deposited.")
                unreadable.isNotEmpty() -> SubjectCheck.Undetermined("The license start_date ${unreadable.joinToString { "'$it'" }} is not a date (YYYY-MM-DD).")
                else -> SubjectCheck.Ok("No embargo declared.")
            }
        }
    }

    // own.co.2 - Data Ownership Role Declared
    // At least one contributor has the role 'owner' or 'data owner' (case-insensitive) and is identified by a name
    // or a contributor_id.
    // PASS: such a contributor exists. FAIL: there are no contributors, none is an owner, an owner has neither a name
    // nor a contributor_id, or there is no DMP.
    fun dataOwnershipRoleDeclared(maDMP: JsonObject, reportId: String, testRecord: TestRecord): Evaluation =
        evaluateSubjects(
            maDMP, reportId, testRecord, generatedBy("dataOwnershipRoleDeclared"),
            subjects = dmpSubject(maDMP),
            noun = "DMP",
            requirement = "a data owner is declared among the contributors",
            whenEmpty = ResultTestEnum.FAIL,
            emptyMessage = NO_DMP,
        ) { dmp ->
            val contributors = dmp.array("contributor").orEmpty().mapNotNull { it as? JsonObject }
            val owners = contributors.filter { contributor -> roles(contributor).any { it.trim().lowercase() in OWNER_ROLES } }
            val named = owners.mapNotNull { it.text("name") ?: it.obj("contributor_id")?.text("identifier") }
            when {
                contributors.isEmpty() -> SubjectCheck.Problem("No contributors declared. Add the data owner as a contributor with the role 'owner'.")
                owners.isEmpty() -> SubjectCheck.Problem("No contributor has the role 'owner'. Add the role to the person or organisation that owns the data.")
                named.isEmpty() -> SubjectCheck.Problem("The contributor with the role 'owner' has no name or contributor_id. Identify the data owner.")
                else -> SubjectCheck.Ok("Data owner(s): ${named.joinToString()}.")
            }
        }

    private const val NO_DMP = "No 'dmp' object found in the maDMP."
    private const val NO_DATASETS = "No datasets found in the maDMP."
    private const val NO_SENSITIVE_DATA = "No dataset declares sensitive data, so there is nothing to assess."
    private const val NO_DESCRIPTION = "No 'security_and_privacy' entry with a description. Describe how the data is protected."
    private val DATA_ACCESS_VALUES = setOf("open", "restricted", "closed", "shared")
    private val OWNER_ROLES = setOf("owner", "data owner")

    // Datasets whose yes/no/unknown [field] is 'yes'; 'no' is skipped and 'unknown' or missing cannot be assessed
    private fun flaggedDatasets(maDMP: JsonObject, field: String): List<Subject> =
        datasetSubjects(maDMP).mapNotNull { subject ->
            when (val value = subject.json?.text(field)) {
                "yes" -> subject
                "no" -> null
                else -> subject.copy(preset = subject.preset ?: SubjectCheck.Undetermined(
                    "'$field' is '${value ?: "missing"}'. State 'yes' or 'no'."))
            }
        }

    private fun descriptions(dataset: JsonObject): List<String> =
        dataset.array("security_and_privacy").orEmpty().mapNotNull { (it as? JsonObject)?.text("description") }

    // The first keyword found in any of [texts], or null
    private fun matchedKeyword(texts: List<String>, keywords: List<String>): String? =
        keywords.firstOrNull { keyword -> texts.any { it.contains(keyword, ignoreCase = true) } }

    private fun licenseRefs(distribution: JsonObject): List<String> =
        distribution.array("license").orEmpty().mapNotNull { (it as? JsonObject)?.text("license_ref") }

    // DCS role is an array of strings; a single string is accepted too
    private fun roles(contributor: JsonObject): List<String> =
        (contributor["role"] as? JsonArray)?.mapNotNull { it.textOrNull() } ?: listOfNotNull(contributor.text("role"))

    // Reads the date part of a DCS date or date-time ("2026-01-15" or "2026-01-15T10:00:00Z")
    private fun parseDate(value: String): LocalDate? =
        try { LocalDate.parse(value.take(10)) } catch (e: DateTimeParseException) { null }

    private fun generatedBy(function: String) = "${this::class.qualifiedName}::$function"
}
