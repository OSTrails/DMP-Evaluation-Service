package io.github.ostrails.dmpevaluatorservice.utils

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.springframework.http.codec.multipart.FilePart

suspend fun fileToJsonObject(file: FilePart): JsonObject {
    val content = file.content()
        .map { dataBuffer -> dataBuffer.toByteBuffer().array().decodeToString() }
        .reduce { acc, text -> acc + text }
        .awaitFirst()
    return Json.parseToJsonElement(content).jsonObject
}

