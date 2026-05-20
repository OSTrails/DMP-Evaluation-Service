package io.github.ostrails.dmpevaluatorservice.model.testResult

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.ostrails.dmpevaluatorservice.model.metric.IdWrapper

data class TestResultSetJsonLD(
    @JsonProperty("@context")
    val context: Map<String, String> = defaultContext,

    @JsonProperty("@id") val id: String,
    @JsonProperty("@type") val type: String = "ftr:TestResultSet",
    @JsonProperty("dcterms:identifier") val identifier: IdWrapper,
    @JsonProperty("dcterms:title") val title: LangLiteral,
    @JsonProperty("ftr:assessmentTarget") val assessmentTarget: IdWrapper,
    @JsonProperty("prov:hadMember") val hadMember: List<IdWrapper>,
    @JsonProperty("prov:generatedAtTime") val generatedAtTime: TypedLiteral,
    @JsonProperty("dcterms:license") val license: IdWrapper? = null
)
