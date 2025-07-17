package io.github.ostrails.dmpevaluatorservice.utils

import kotlinx.serialization.json.*

inline fun <reified T> extractValuesByPath(maDMP: JsonObject, path: String): List<Any?>  {
    val segments = path.split(".")
    var elements: List<JsonElement?> = listOf(maDMP)

    for (segment in segments) {
        elements = elements.flatMap { element ->
            when (element) {
                is JsonObject -> {
                    if (segment.endsWith("[*]")) {
                        val key = segment.removeSuffix("[*]")
                        val array = element[key]?.jsonArrayOrNull()
                        array?.toList() ?: emptyList()
                    } else {
                        element[segment]?.let { listOf(it) } ?: emptyList()
                    }
                }
                is JsonArray -> {
                    element.flatMap { subElem ->
                        if (subElem is JsonObject && subElem[segment] != null) {
                            listOf(subElem[segment]!!)
                        } else emptyList()
                    }
                }
                else -> emptyList()
            }
        }
    }

    return elements

}

fun JsonElement.jsonArrayOrNull(): JsonArray? =
    this as? JsonArray

val JsonElement.jsonPrimitiveOrNull: JsonPrimitive?
    get() = this as? JsonPrimitive
