package io.github.ostrails.dmpevaluatorservice.model.testResult


import com.fasterxml.jackson.annotation.JsonProperty
import io.github.ostrails.dmpevaluatorservice.model.metric.IdWrapper

data class TestResultJsonLD(
    @JsonProperty("@context")
    val context: Map<String, String> = defaultContext,

    @JsonProperty("@graph")
    val graph: List<Any> // mixed types: TestResult, Activity, etc.
)

val defaultContext = mapOf(
    "xsd" to "http://www.w3.org/2001/XMLSchema#",
    "prov" to "http://www.w3.org/ns/prov#",
    "dct" to "http://purl.org/dc/terms/",
    "dcat" to "http://www.w3.org/ns/dcat#",
    "ftr" to "https://w3id.org/ftr#",
    "sio" to "http://semanticscience.org/resource/",
    "schema" to "http://schema.org/"
)

data class TestExecutionActivity(
    @JsonProperty("@id") val id: String,
    @JsonProperty("@type") val type: String = "ftr:TestExecutionActivity",
    @JsonProperty("prov:wasAssociatedWith") val associatedWith: IdWrapper,
    @JsonProperty("prov:generated") val generated: IdWrapper,
    @JsonProperty("prov:used") val used: IdWrapper
)

data class TestResultGraph(
    @JsonProperty("@id") val id: String,
    @JsonProperty("@type") val type: String = "ftr:TestResult",
    @JsonProperty("dct:identifier") val identifier: IdWrapper,
    @JsonProperty("dct:title") val title: LangLiteral,
    @JsonProperty("dct:description") val description: LangLiteral,
    @JsonProperty("dct:license") val license: IdWrapper,
    @JsonProperty("prov:value") val resultValue: LangLiteral,
    @JsonProperty("ftr:summary") val summary: LangLiteral,
    @JsonProperty("prov:generatedAtTime") val generatedAt: TypedLiteral,
    @JsonProperty("ftr:log") val log: LangLiteral,
    @JsonProperty("ftr:completion") val completion: TypedLiteral,
    @JsonProperty("ftr:outputFromTest") val outputFromTest: IdWrapper,
    @JsonProperty("prov:wasDerivedFrom") val wasDerivedFrom: IdWrapper
)

data class TestDefinition(
    @JsonProperty("@id") val id: String,
    @JsonProperty("@type") val type: List<String>,
    @JsonProperty("dct:identifier") val identifier: String,
    @JsonProperty("dct:title") val title: LangLiteral,
    @JsonProperty("dct:description") val description: LangLiteral,
    @JsonProperty("dcat:endpointDescription") val endpointDescription: IdWrapper,
    @JsonProperty("dcat:endpointURL") val endpointURL: IdWrapper,
    @JsonProperty("dcat:version") val version: LangLiteral,
    @JsonProperty("dct:license") val license: IdWrapper,
    @JsonProperty("sio:SIO_000233") val reference: IdWrapper
)

data class Entity(
    @JsonProperty("@id") val id: String,
    @JsonProperty("@type") val type: String = "prov:Entity"
)

data class LangLiteral(
    @JsonProperty("@language") val language: String = "en",
    @JsonProperty("@value") val value: String
)

data class TypedLiteral(
    @JsonProperty("@type") val type: String,
    @JsonProperty("@value") val value: String
)