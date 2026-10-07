package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.database.model.Guidance
import io.github.ostrails.dmpevaluatorservice.database.model.GuidanceEntry
import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import io.github.ostrails.dmpevaluatorservice.utils.buildDatasetLabel
import io.github.ostrails.dmpevaluatorservice.utils.extractAssessmentTarget
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import java.util.UUID

// Outcome of checking one subject (a dataset, distribution, contributor or the DMP itself)
sealed interface SubjectCheck {
    data class Ok(val message: String) : SubjectCheck
    data class Problem(val reason: String) : SubjectCheck
    data class Undetermined(val reason: String) : SubjectCheck
}

// Something a coverage test inspects. [path] is reported in affectedElements and [label] in the guidance.
// A preset outcome (e.g. "no distributions declared") is used instead of running the check on [json].
data class Subject(
    val path: String,
    val label: String,
    val json: JsonObject?,
    val preset: SubjectCheck? = null,
)

enum class DatasetScope { ALL, NEW, REUSED }

const val CANNOT_CLASSIFY_REUSE =
    "'is_reused' is not set, so the dataset cannot be classified as new or reused (DCS 1.3)."

fun JsonObject.text(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray

fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

fun JsonElement.textOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

fun isReused(dataset: JsonObject): Boolean? = (dataset["is_reused"] as? JsonPrimitive)?.booleanOrNull

fun datasetSubjects(maDMP: JsonObject, scope: DatasetScope = DatasetScope.ALL): List<Subject> {
    val datasets = maDMP.obj("dmp")?.array("dataset") ?: return emptyList()
    return datasets.mapIndexedNotNull { index, element ->
        val dataset = element as? JsonObject
            ?: return@mapIndexedNotNull Subject("dataset[$index]", "Dataset at position $index", null,
                SubjectCheck.Problem("Dataset entry is not a JSON object."))
        val datasetId = dataset.obj("dataset_id")
        val label = buildDatasetLabel(dataset.text("title"), datasetId?.text("type"), datasetId?.text("identifier"), index)
        val subject = Subject("dataset[$index]", label, dataset)
        when (scope) {
            DatasetScope.ALL -> subject
            DatasetScope.NEW, DatasetScope.REUSED -> {
                val reused = isReused(dataset)
                when {
                    reused == null -> subject.copy(preset = SubjectCheck.Undetermined(CANNOT_CLASSIFY_REUSE))
                    reused == (scope == DatasetScope.REUSED) -> subject
                    else -> null
                }
            }
        }
    }
}

// Distributions of the given datasets. A dataset without distributions becomes a single subject with a
// preset problem; a dataset with a preset outcome (e.g. unclassified) is passed through unchanged.
fun distributionSubjects(datasets: List<Subject>): List<Subject> =
    datasets.flatMap { dataset ->
        if (dataset.preset != null || dataset.json == null) return@flatMap listOf(dataset)
        val distributions = dataset.json.array("distribution")
        if (distributions.isNullOrEmpty()) {
            return@flatMap listOf(dataset.copy(preset = SubjectCheck.Problem("No distributions declared.")))
        }
        distributions.mapIndexed { index, element ->
            val distribution = element as? JsonObject
            val title = distribution?.text("title") ?: "distribution ${index + 1}"
            Subject(
                "${dataset.path}.distribution[$index]",
                "${dataset.label} / $title",
                distribution,
                if (distribution == null) SubjectCheck.Problem("Distribution entry is not a JSON object.") else null,
            )
        }
    }

fun contributorSubjects(maDMP: JsonObject): List<Subject> {
    val contributors = maDMP.obj("dmp")?.array("contributor") ?: return emptyList()
    return contributors.mapIndexed { index, element ->
        val contributor = element as? JsonObject
        Subject(
            "contributor[$index]",
            contributor?.text("name") ?: "Unnamed contributor (position $index)",
            contributor,
            if (contributor == null) SubjectCheck.Problem("Contributor entry is not a JSON object.") else null,
        )
    }
}

fun dmpSubject(maDMP: JsonObject): List<Subject> {
    val dmp = maDMP.obj("dmp") ?: return emptyList()
    return listOf(Subject("dmp", dmp.text("title") ?: "DMP", dmp))
}

// Runs [check] on every subject and builds the Evaluation: FAIL if any subject has a problem, otherwise
// INDETERMINATE if any could not be assessed, otherwise PASS. With no subjects the result is [whenEmpty].
fun evaluateSubjects(
    maDMP: JsonObject,
    reportId: String,
    testRecord: TestRecord,
    generatedBy: String,
    subjects: List<Subject>,
    noun: String,
    requirement: String,
    whenEmpty: ResultTestEnum,
    emptyMessage: String,
    check: (JsonObject) -> SubjectCheck,
): Evaluation {
    val logMessages = mutableListOf<String>()
    val affected = mutableListOf<String>()
    val issues = mutableListOf<GuidanceEntry>()
    var problems = 0
    var undetermined = 0

    subjects.forEach { subject ->
        val outcome = subject.preset ?: check(subject.json!!)
        when (outcome) {
            is SubjectCheck.Ok -> logMessages.add("${subject.path} '${subject.label}': ${outcome.message}")
            is SubjectCheck.Problem -> {
                problems++
                logMessages.add("${subject.path} '${subject.label}': ${outcome.reason}")
                affected.add(subject.path)
                issues.add(GuidanceEntry(dataset = subject.label, reason = outcome.reason))
            }
            is SubjectCheck.Undetermined -> {
                undetermined++
                logMessages.add("${subject.path} '${subject.label}': could not be assessed - ${outcome.reason}")
                affected.add(subject.path)
                issues.add(GuidanceEntry(dataset = subject.label, reason = "Could not be assessed: ${outcome.reason}"))
            }
        }
    }

    val total = subjects.size
    val result = when {
        subjects.isEmpty() -> whenEmpty
        problems > 0 -> ResultTestEnum.FAIL
        undetermined > 0 -> ResultTestEnum.INDETERMINATE
        else -> ResultTestEnum.PASS
    }
    if (subjects.isEmpty()) logMessages.add(emptyMessage)

    val summary = when {
        subjects.isEmpty() -> emptyMessage
        result == ResultTestEnum.PASS -> "All $total $noun(s) meet the requirement: $requirement."
        result == ResultTestEnum.FAIL -> "$problems of $total $noun(s) do not meet the requirement: $requirement." +
            if (undetermined > 0) " $undetermined could not be assessed." else ""
        else -> "$undetermined of $total $noun(s) could not be assessed for the requirement: $requirement."
    }

    return Evaluation(
        evaluationId = UUID.randomUUID().toString(),
        result = result,
        details = testRecord.description,
        title = testRecord.title,
        reportId = reportId,
        log = logMessages.joinToString("\n"),
        affectedElements = affected.ifEmpty { null },
        assessmentTarget = extractAssessmentTarget(maDMP),
        wasGeneratedBy = generatedBy,
        outputFromTest = testRecord.id,
        completion = 100,
        guidance = Guidance(summary = summary, issues = issues),
    )
}
