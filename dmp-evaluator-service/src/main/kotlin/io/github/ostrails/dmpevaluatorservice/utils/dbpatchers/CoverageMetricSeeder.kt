package io.github.ostrails.dmpevaluatorservice.utils.dbpatchers

import io.github.ostrails.dmpevaluatorservice.database.model.MetricRecord
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.exists
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component

// One OSTrails pilot metric implemented by one DCSCoverageEvaluator function. Each spec seeds a TestRecord
// and a MetricRecord with fixed ids, so seeding stays idempotent across deployments.
data class CoverageMetricSpec(
    val metricId: String,
    val testId: String,
    val abbreviation: String,
    val title: String,
    val description: String,
    val function: String,
    val keyword: String,
    val isApplicableFor: String = "https://vocabularies.coar-repositories.org/resource_types/c_ab20/",
)

@Component
class CoverageMetricSeeder(
    private val mongoTemplate: MongoTemplate,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(CoverageMetricSeeder::class.java)

        const val EVALUATOR = "DCSCoverageEvaluator"
        private const val SUPPORTED_BY = "https://github.com/OSTrails/DMP-Evaluation-Service"
        private const val DATASET = "https://schema.org/Dataset"

        val specs: List<CoverageMetricSpec> = listOf(
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000001", testId = "6ac62fd3616d0a2f64000002",
                abbreviation = "data.lice.co.1",
                title = "Dataset License Declared",
                description = "Checks that every dataset in the maDMP declares a license (license_ref) on at least one of its distributions. Fails if any dataset has no distributions or none of them declares a license.",
                function = "datasetLicenseDeclared",
                keyword = "license, dataset, distribution, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000003", testId = "6ac62fd3616d0a2f64000004",
                abbreviation = "data.shar.co.1",
                title = "Data License is Present",
                description = "Checks that every distribution of every dataset in the maDMP declares a license (license_ref). Fails if any distribution has no license or a dataset has no distributions.",
                function = "distributionLicensePresent",
                keyword = "license, distribution, data sharing, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000005", testId = "6ac62fd3616d0a2f64000006",
                abbreviation = "store.cov.1",
                title = "Data Storage Location mentioned in the DMP",
                description = "Checks that every distribution of every dataset declares where it is stored: a host with a title, url or host_id (DCS 1.3). Fails if any distribution has no host, or a host without any of these fields.",
                function = "storageLocationDeclared",
                keyword = "storage, host, repository, distribution, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000007", testId = "6ac62fd3616d0a2f64000008",
                abbreviation = "data.info.cov.2",
                title = "Dataset File Format Specified",
                description = "Checks that every distribution of every dataset in the maDMP specifies at least one file format (format). Fails if any distribution has no format or a dataset has no distributions.",
                function = "distributionFormatSpecified",
                keyword = "file format, media type, distribution, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000009", testId = "6ac62fd3616d0a2f6400000a",
                abbreviation = "repo.feas.2",
                title = "Long-Term Preservation Dataset",
                description = "Checks that every dataset in the maDMP provides a long-term preservation statement (preservation_statement). Fails if any dataset has no preservation statement.",
                function = "preservationStatementPresent",
                keyword = "preservation, long-term, dataset, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400000b", testId = "6ac62fd3616d0a2f6400000c",
                abbreviation = "ethics.co.1",
                title = "Ethical Issues Status Declared",
                description = "Checks that the maDMP declares whether ethical issues exist (ethical_issues_exist). Passes for 'yes' or 'no', is indeterminate for 'unknown', and fails if the field is missing or invalid.",
                function = "ethicalIssuesStatusDeclared",
                keyword = "ethics, ethical issues, DMP, DCS",
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400000d", testId = "6ac62fd3616d0a2f6400000e",
                abbreviation = "secur.co.1",
                title = "Security Measures Implementation",
                description = "Checks that every dataset in the maDMP declares at least one security and privacy measure (security_and_privacy with a title). Fails if any dataset declares none.",
                function = "securityMeasuresDeclared",
                keyword = "security, privacy, dataset, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400000f", testId = "6ac62fd3616d0a2f64000010",
                abbreviation = "store.comp.1",
                title = "Alignment of Storage and Backup with Information Sensitivity",
                description = "Checks that every dataset declaring personal or sensitive data ('yes') also declares security and privacy measures (security_and_privacy). Datasets declaring 'no' for both are skipped; 'unknown' or missing flags are indeterminate, as is a DMP with no such datasets.",
                function = "sensitiveDataProtected",
                keyword = "sensitive data, personal data, security, storage, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000011", testId = "6ac62fd3616d0a2f64000012",
                abbreviation = "data.pid.cov.1",
                title = "Repository Supports Persistent Identifiers for Datasets",
                description = "Checks that the repository (host) of every distribution declares at least one persistent identifier system (pid_system, e.g. doi or handle). Fails if any distribution has no host or its host declares no PID system.",
                function = "repositoryPidSystemDeclared",
                keyword = "persistent identifier, PID system, repository, host, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000013", testId = "6ac62fd3616d0a2f64000014",
                abbreviation = "data.info.cov.1",
                title = "Dataset Type Specified",
                description = "Checks that every dataset in the maDMP specifies its type (type). Fails if any dataset has no type.",
                function = "datasetTypeSpecified",
                keyword = "dataset type, dataset, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000015", testId = "6ac62fd3616d0a2f64000016",
                abbreviation = "data.info.cov.3",
                title = "Dataset Size Specified",
                description = "Checks that every distribution of every dataset in the maDMP specifies its size in bytes (byte_size, a non-negative integer). Fails if any distribution has no or an invalid size, or a dataset has no distributions.",
                function = "distributionSizeSpecified",
                keyword = "size, byte size, distribution, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000017", testId = "6ac62fd3616d0a2f64000018",
                abbreviation = "meta.stand.comp.1",
                title = "Metadata Standards Used",
                description = "Checks that every dataset in the maDMP declares at least one metadata standard (metadata[].metadata_standard_id). Fails if any dataset declares none.",
                function = "metadataStandardDeclared",
                keyword = "metadata standard, metadata, dataset, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000019", testId = "6ac62fd3616d0a2f6400001a",
                abbreviation = "data.exteresource.co.2",
                title = "Metadata Standard Specified in the DMP",
                description = "Checks that the maDMP specifies at least one metadata standard (metadata[].metadata_standard_id) on any of its datasets. Fails if no dataset declares a metadata standard.",
                function = "metadataStandardInDmp",
                keyword = "metadata standard, external resources, DMP, DCS",
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400001b", testId = "6ac62fd3616d0a2f6400001c",
                abbreviation = "qc.qual.1",
                title = "Quality Control Methods Stated",
                description = "Checks that every dataset in the maDMP states at least one quality control method (data_quality_assurance). Fails if any dataset states none.",
                function = "qualityAssuranceDeclared",
                keyword = "quality control, quality assurance, dataset, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400001d", testId = "6ac62fd3616d0a2f6400001e",
                abbreviation = "store.co.1",
                title = "Back up Frequency",
                description = "Checks that the host of every distribution states its backup frequency (host.backup_frequency). Fails if any distribution has no host or its host states no backup frequency.",
                function = "backupFrequencyDeclared",
                keyword = "backup, storage, host, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400001f", testId = "6ac62fd3616d0a2f64000020",
                abbreviation = "role.pid.co.1",
                title = "Contributors and Organisations PIDs",
                description = "Checks that every contributor in the maDMP is identified by a persistent identifier (contributor_id of type orcid or isni) and that every declared affiliation has an organisation identifier (affiliation_id of type ror, grid or isni, DCS 1.3). Fails if there are no contributors or any contributor or affiliation lacks a PID.",
                function = "contributorPidsDeclared",
                keyword = "contributor, ORCID, ROR, persistent identifier, affiliation, DCS",
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000021", testId = "6ac62fd3616d0a2f64000022",
                abbreviation = "repo.co.5",
                title = "Certification of Repository",
                description = "Checks that the host of every distribution declares a recognised repository certification (host.certified_with, e.g. coretrustseal). Fails if any distribution has no host, or its host declares no or an unrecognised certification.",
                function = "repositoryCertified",
                keyword = "certification, CoreTrustSeal, repository, host, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000023", testId = "6ac62fd3616d0a2f64000024",
                abbreviation = "data.pid.cov.2",
                title = "Trusted Repository Referenced",
                description = "Checks that the host of every distribution references its re3data registry entry (host.host_id of type re3data, DCS 1.3). Fails if any distribution has no host or its host has no re3data identifier. Registration itself is verified online by the RE3data compliance test.",
                function = "trustedRepositoryReferenced",
                keyword = "re3data, trusted repository, host identifier, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000025", testId = "6ac62fd3616d0a2f64000026",
                abbreviation = "data.shar.op.1",
                title = "Data Access Status Open for the Dataset",
                description = "Checks that every distribution of every dataset in the maDMP is openly accessible (data_access is 'open'). Fails if any distribution is restricted, closed, shared (deprecated) or declares no data access.",
                function = "distributionAccessOpen",
                keyword = "open access, data access, distribution, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000027", testId = "6ac62fd3616d0a2f64000028",
                abbreviation = "ethics.co.3",
                title = "Justification for Absence of Ethical Issues",
                description = "Checks that a maDMP declaring no ethical issues (ethical_issues_exist is 'no') justifies this in ethical_issues_description. Fails if the justification is missing; indeterminate when ethical issues are declared as 'yes', 'unknown' or not declared.",
                function = "noEthicalIssuesJustified",
                keyword = "ethics, ethical issues, justification, DMP, DCS",
            ),
            // New and reused datasets (DatasetProvenanceTests). A dataset is reused when is_reused is true and new
            // otherwise, so DCS 1.2 datasets without the flag count as new.
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000029", testId = "6ac62fd3616d0a2f6400002a",
                abbreviation = "data.new.1",
                title = "New Dataset Declared in the DMP",
                description = "Checks that the maDMP declares at least one new dataset (is_reused is false or not set). Fails if there are no datasets or all datasets are reused.",
                function = "newDatasetDeclared",
                keyword = "new data, dataset, data collection, DCS",
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400002b", testId = "6ac62fd3616d0a2f6400002c",
                abbreviation = "data.new.2",
                title = "New Dataset Collection or Creation Method Declared",
                description = "Checks that every new dataset declares the technical resources used to collect or create it (technical_resource with name and description; an identifier, if given, needs identifier and type). Fails if a new dataset has none or one is incomplete; indeterminate when there are no new datasets.",
                function = "newDatasetCollectionMethodDeclared",
                keyword = "new data, data collection, technical resource, methods, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400002d", testId = "6ac62fd3616d0a2f6400002e",
                abbreviation = "data.new.3",
                title = "New Dataset Has Declared Access Conditions",
                description = "Checks that every distribution of every new dataset declares its access mode (data_access: open, restricted or closed; 'shared' is accepted) and a license (license_ref). Fails if either is missing or invalid, or a new dataset has no distributions; indeterminate when there are no new datasets.",
                function = "newDatasetAccessDeclared",
                keyword = "new data, data access, license, distribution, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400002f", testId = "6ac62fd3616d0a2f64000030",
                abbreviation = "data.new.4",
                title = "New Dataset Has Sufficient Metadata",
                description = "Checks that every new dataset declares metadata, and that each metadata entry has a description, a language and a metadata_standard_id with identifier and type. Fails if a new dataset has no metadata or an entry is incomplete; indeterminate when there are no new datasets.",
                function = "newDatasetMetadataSufficient",
                keyword = "new data, metadata, metadata standard, reuse, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000031", testId = "6ac62fd3616d0a2f64000032",
                abbreviation = "data.reused.co.2",
                title = "Reused Dataset Has a Persistent Identifier",
                description = "Checks that every reused dataset (is_reused is true) is identified by a dataset_id with an identifier and a type. Fails if a reused dataset is missing either; indeterminate when there are no reused datasets.",
                function = "reusedDatasetPidDeclared",
                keyword = "reused data, persistent identifier, citation, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000033", testId = "6ac62fd3616d0a2f64000034",
                abbreviation = "data.reused.co.3",
                title = "Reused Dataset Has a Declared License",
                description = "Checks that every reused dataset declares a license on its distributions, with a license_ref and a start_date. Fails if a reused dataset has no license or an incomplete one; indeterminate when there are no reused datasets.",
                function = "reusedDatasetLicenseDeclared",
                keyword = "reused data, license, terms of use, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000035", testId = "6ac62fd3616d0a2f64000036",
                abbreviation = "data.reused.co.4",
                title = "Reused Dataset Has Distribution Information",
                description = "Checks that every reused dataset has distributions, each with a title and an access location (access_url or download_url). Fails if a distribution lacks either or a reused dataset has no distributions; indeterminate when there are no reused datasets.",
                function = "reusedDatasetDistributionDeclared",
                keyword = "reused data, distribution, access, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000037", testId = "6ac62fd3616d0a2f64000038",
                abbreviation = "data.reused.co.6",
                title = "Reused Dataset Contains Personal Data",
                description = "Checks that every reused dataset declares whether it contains personal data (personal_data: yes, no or unknown). Fails if the flag is missing or invalid; indeterminate when there are no reused datasets.",
                function = "reusedDatasetPersonalDataDeclared",
                keyword = "reused data, personal data, data protection, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000039", testId = "6ac62fd3616d0a2f6400003a",
                abbreviation = "data.reused.co.7",
                title = "Reused Dataset Contains Sensitive Data",
                description = "Checks that every reused dataset declares whether it contains sensitive data (sensitive_data: yes, no or unknown). Fails if the flag is missing or invalid; indeterminate when there are no reused datasets.",
                function = "reusedDatasetSensitiveDataDeclared",
                keyword = "reused data, sensitive data, security, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400003b", testId = "6ac62fd3616d0a2f6400003c",
                abbreviation = "data.reused.co.8",
                title = "Reused Dataset Has an Access URL",
                description = "Checks that every reused dataset has at least one distribution with an access_url, so the source data can be found. Fails if a reused dataset has none; indeterminate when there are no reused datasets.",
                function = "reusedDatasetAccessUrlDeclared",
                keyword = "reused data, access URL, distribution, DCS",
                isApplicableFor = DATASET,
            ),
            // Security, sensitive data and access (SecurityAccessTests). DCS has no fields for access control, user
            // roles or GDPR, so those are found by keywords in security_and_privacy.description.
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400003d", testId = "6ac62fd3616d0a2f6400003e",
                abbreviation = "secur.co.2",
                title = "Sensitive Data Protection Description",
                description = "Checks that every dataset with sensitive data (sensitive_data 'yes') has a security_and_privacy description that mentions access rights management (e.g. access control, authorisation, institutional storage). Fails if the description is missing or has no such reference; indeterminate for 'unknown' or missing flags and when no dataset contains sensitive data.",
                function = "sensitiveDataProtectionDescribed",
                keyword = "sensitive data, access rights, security, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400003f", testId = "6ac62fd3616d0a2f64000040",
                abbreviation = "secur.comp.1",
                title = "GDPR and Ethics Compliance",
                description = "Checks that every dataset with personal data (personal_data 'yes') references GDPR or data protection law in its security_and_privacy description, or that the maDMP references an ethical review (ethical_issues_report). Fails if neither is present; indeterminate for 'unknown' or missing flags and when no dataset contains personal data.",
                function = "gdprEthicsComplianceDeclared",
                keyword = "GDPR, data protection, ethics, personal data, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000041", testId = "6ac62fd3616d0a2f64000042",
                abbreviation = "secur.co.4",
                title = "Access Control and User Management",
                description = "Checks that every dataset with sensitive data has a security_and_privacy description that mentions both access control and user roles or permissions (e.g. role-based access, user groups, privileges). Fails if the description is missing or lacks either; indeterminate for 'unknown' or missing flags and when no dataset contains sensitive data.",
                function = "accessControlDescribed",
                keyword = "access control, user management, roles, permissions, sensitive data, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000043", testId = "6ac62fd3616d0a2f64000044",
                abbreviation = "sens.secure.co.1",
                title = "Sensitive Data Using Method",
                description = "Checks that every dataset with sensitive data describes the method used to protect it in a non-empty security_and_privacy description. Fails if the description is missing; indeterminate for 'unknown' or missing flags and when no dataset contains sensitive data.",
                function = "sensitiveDataHandlingMethodDeclared",
                keyword = "sensitive data, data protection method, security, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000045", testId = "6ac62fd3616d0a2f64000046",
                abbreviation = "data.shar.co.2",
                title = "Data Restrictions Reference",
                description = "Checks that every distribution states its access restrictions through its access mode (data_access) and a license (license_ref), as DCS has no rights field. Fails if either is missing or invalid, a dataset has no distributions, or there are no datasets.",
                function = "dataRestrictionsExplained",
                keyword = "data restrictions, data access, license, data sharing, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000047", testId = "6ac62fd3616d0a2f64000048",
                abbreviation = "data.shar.comp.2",
                title = "Embargo Declared in the DMP or Repository",
                description = "Checks that no license of any distribution starts after the maDMP was created (license.start_date later than dmp.created, or today if missing), as Horizon Europe does not allow embargoes. Fails if a license starts later; indeterminate if a start_date is not a date or there are no datasets.",
                function = "embargoDeclared",
                keyword = "embargo, license, start date, Horizon Europe, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000049", testId = "6ac62fd3616d0a2f6400004a",
                abbreviation = "own.co.2",
                title = "Data Ownership Role Declared",
                description = "Checks that at least one contributor has the role 'owner' or 'data owner' and is identified by a name or contributor_id. Fails if there are no contributors, none is an owner, or the owner is not identified.",
                function = "dataOwnershipRoleDeclared",
                keyword = "data ownership, contributor, role, DCS",
            ),
            // Repository, storage and cost (RepositoryStorageTests)
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400004b", testId = "6ac62fd3616d0a2f6400004c",
                abbreviation = "repo.comp.2",
                title = "Repository Conforms with FAIR Data Principles",
                description = "Checks that the host of every distribution declares FAIR-enabling attributes: a PID system and either a re3data host_id or a recognised certification. Fails if one is missing, a distribution has no host, or there are no datasets. The re3data record itself is not fetched.",
                function = "repositoryFairConformant",
                keyword = "FAIR, repository, PID system, certification, re3data, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400004d", testId = "6ac62fd3616d0a2f6400004e",
                abbreviation = "store.cov.2",
                title = "Use of Secure Storage for the Dataset in a Trusted Repository",
                description = "Checks that every dataset is stored in a trusted repository (each distribution's host has a re3data host_id or a recognised certification) and declares its security measures (security_and_privacy). Fails if a dataset has no distributions, an untrusted host or no security measures, or there are no datasets.",
                function = "secureTrustedStorageUsed",
                keyword = "secure storage, trusted repository, security, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f6400004f", testId = "6ac62fd3616d0a2f64000050",
                abbreviation = "repo.co.4",
                title = "Verification of Back-up Strategy",
                description = "Checks that the host of every distribution states its backup strategy: backup_type and backup_frequency. Fails if either is missing, a distribution has no host, or there are no datasets.",
                function = "backupStrategyDeclared",
                keyword = "backup, backup strategy, storage, host, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000051", testId = "6ac62fd3616d0a2f64000052",
                abbreviation = "meta.qual.2",
                title = "ReadMe File Reference",
                description = "Checks that every dataset references a README file, searched in the dataset and metadata descriptions and in the title, description and URLs of its distributions (DCS has no README field). Fails if a dataset has no reference or there are no datasets.",
                function = "readmeReferenced",
                keyword = "README, documentation, metadata quality, DCS",
                isApplicableFor = DATASET,
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000053", testId = "6ac62fd3616d0a2f64000054",
                abbreviation = "cost.co.1",
                title = "DMP Includes a Budget for Personnel and Monetary Resources",
                description = "Checks that the maDMP declares at least one personnel cost (e.g. staff, salary, FTE, data steward) and at least one other cost, and that every cost has a numeric value and a currency_code. Fails if there are no costs, a category is missing or a cost is incomplete.",
                function = "personnelAndMonetaryBudgetDeclared",
                keyword = "cost, budget, personnel, resources, DCS",
            ),
            CoverageMetricSpec(
                metricId = "6ac62fd3616d0a2f64000055", testId = "6ac62fd3616d0a2f64000056",
                abbreviation = "cost.co.2",
                title = "DMP States No Additional RDM Resources Are Required",
                description = "Checks that a maDMP declaring no cost above 0 states explicitly that no additional resources are needed (a cost with value 0, or a statement such as 'no additional costs'). Fails if there is no such statement; indeterminate when the maDMP declares a cost above 0.",
                function = "noAdditionalResourcesStated",
                keyword = "cost, resources, budget, DCS",
            ),
        )
    }

    @PostConstruct
    fun seed() {
        specs.forEach { spec ->
            seedIfMissing(spec.toTestRecord(), "tests", spec.testId)
            seedIfMissing(spec.toMetricRecord(), "metrics", spec.metricId)
        }
    }

    private fun CoverageMetricSpec.toTestRecord() = TestRecord(
        id = testId,
        title = title,
        description = description,
        license = "https://creativecommons.org/licenses/by/4.0/",
        version = "1.0.0",
        keyword = keyword,
        abbreviation = abbreviation,
        status = "active",
        supportedBy = SUPPORTED_BY,
        isApplicableFor = isApplicableFor,
        evaluator = EVALUATOR,
        functionEvaluator = function,
        metricImplemented = metricId,
    )

    private fun CoverageMetricSpec.toMetricRecord() = MetricRecord(
        id = metricId,
        title = title,
        description = description,
        version = "1.0.0",
        testAssociated = listOf(testId),
        abbreviation = abbreviation,
        supportedBy = SUPPORTED_BY,
        isApplicableFor = isApplicableFor,
        keyword = keyword,
        hasBenchmark = emptyList(),
        license = "http://creativecommons.org/licenses/by/2.0/",
        status = "active",
    )

    private inline fun <reified T : Any> seedIfMissing(record: T, collectionName: String, id: String) {
        if (mongoTemplate.exists<T>(Query(Criteria.where("_id").`is`(id)), collectionName)) {
            log.info("CoverageMetricSeeder: '$id' already exists in '$collectionName', skipping.")
        } else {
            mongoTemplate.insert(record, collectionName)
            log.info("CoverageMetricSeeder: inserted '$id' into '$collectionName'.")
        }
    }
}
