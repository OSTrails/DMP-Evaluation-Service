package io.github.ostrails.dmpevaluatorservice.model.test

import com.fasterxml.jackson.annotation.JsonProperty

data class TestJsonLD(
    @JsonProperty("@context") val context: Map<String, String> = mapOf(
        "schema" to "http://schema.org/",
        "rdf" to "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        "ftr" to "https://w3id.org/ftr#",
        "sio" to "http://semanticscience.org/resource/",
        "xsd" to "http://www.w3.org/2001/XMLSchema#",
        "dcterms" to "http://purl.org/dc/terms/",
        "dcat" to "http://www.w3.org/ns/dcat#",
        "prov" to "http://www.w3.org/ns/prov#"
    ),
    @JsonProperty("@graph") val graph: List<Any>
)

// Main Test entity JSON-LD entry
data class FullTestLDEntry(
    @JsonProperty("@id") val id: String,
    @JsonProperty("@type") val type: List<String> = listOf("https://w3id.org/ftr#Test", "http://www.w3.org/ns/dcat#DataService"),
    @JsonProperty("dcterms:identifier") val identifier: IdWrapper,
    @JsonProperty("dcterms:title") val title: LangLiteral,
    @JsonProperty("dcterms:description") val description: LangLiteral,
    @JsonProperty("dcterms:license") val license: IdWrapper,
    @JsonProperty("dcat:endpointURL") val endpointURL: IdWrapper,
    @JsonProperty("dcat:endpointDescription") val endpointDescription: IdWrapper?,
    @JsonProperty("dcat:version") val version: LangLiteral,
    @JsonProperty("dcat:keyword") val keyword: List<LangLiteral>?,
    @JsonProperty("dcterms:type") val typeUri: IdWrapper?,
    @JsonProperty("dcat:theme") val theme: IdWrapper?,
    @JsonProperty("dqv:inDimension") val inDimension: IdWrapper?,
    @JsonProperty("ftr:supportedBy") val supportedBy: IdWrapper?,
    @JsonProperty("dpv:isApplicableFor") val isApplicableFor: IdWrapper?,
    @JsonProperty("dcterms:creator") val creator: IdWrapper?,
    @JsonProperty("dcat:contactPoint") val contactPoint: List<IdWrapper>?,
    @JsonProperty("sio:SIO_000233") val linkedMetric: IdWrapper?
)

// Wrapper for ID-based fields
data class IdWrapper(
    @JsonProperty("@id") val id: String
)

// Generic language literal
data class LangLiteral(
    @JsonProperty("@language") val language: String = "en",
    @JsonProperty("@value") val value: String
)

// Helper for xsd:typed values
data class TypedDate(
    @JsonProperty("@value") val value: String,
    @JsonProperty("@type") val type: String = "xsd:date"
)

data class TypedInt(
    @JsonProperty("@value") val value: String,
    @JsonProperty("@type") val type: String = "xsd:int"
)

