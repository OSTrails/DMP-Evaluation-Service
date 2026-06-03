package io.github.ostrails.dmpevaluatorservice.utils

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull

fun extractAssessmentTarget(maDMP: JsonObject): String =
    (maDMP["dmp"] as? JsonObject)
        ?.get("dmp_id")?.let { it as? JsonObject }
        ?.get("identifier")?.jsonPrimitiveOrNull?.contentOrNull
        ?.takeIf { it.isNotBlank() }
        ?: "https://www.rd-alliance.org/group/dmp-common-standards-wg/outcomes/rda"

fun buildDatasetLabel(title: String?, type: String?, identifier: String?, index: Int): String {
    val identifierPart = if (!type.isNullOrBlank() && !identifier.isNullOrBlank()) "$type:$identifier" else null
    return when {
        !title.isNullOrBlank() && identifierPart != null -> "$title ($identifierPart)"
        !title.isNullOrBlank() -> title
        identifierPart != null -> identifierPart
        else -> "Unnamed dataset (position $index)"
    }
}
