package io.github.ostrails.dmpevaluatorservice.model.metric

import com.fasterxml.jackson.annotation.JsonProperty

data class MetricJsonLD(

    @JsonProperty("@context")
    val context: Map<String, String> = defaultContext,

    @JsonProperty("@graph")
    val graph: List<MetricGraphEntry>

){
    companion object {
        val defaultContext = mapOf(
            "ftr" to "https://w3id.org/ftr#",
            "doap" to "http://usefulinc.com/ns/doap#",
            "xsd" to "http://www.w3.org/2001/XMLSchema#",
            "dqv" to "http://www.w3.org/ns/dqv#",
            "dcterms" to "http://purl.org/dc/terms/",
            "rdfs" to "http://www.w3.org/2000/01/rdf-schema#",
            "vcard" to "http://www.w3.org/2006/vcard/ns#",
            "vivo" to "http://vivoweb.org/ontology/core#",
            "dcat" to "http://www.w3.org/ns/dcat#",
            "foaf" to "http://xmlns.com/foaf/0.1/"
        )
    }
}

data class MetricGraphEntry(
    @JsonProperty("@id")
    val id: String,

    @JsonProperty("@type")
    val type: String = "dqv:Metric",

    @JsonProperty("dcterms:title")
    val title: String,

    @JsonProperty("dcterms:description")
    val description: String,

    @JsonProperty("dcat:version")
    val version: String,

    @JsonProperty("rdfs:label")
    val label: String? = null,

    @JsonProperty("vivo:abbreviation")
    val abbreviation: String? = null,

    @JsonProperty("dcat:landingPage")
    val landingPage: IdWrapper? = null,

    @JsonProperty("dcat:keyword")
    val keyword: List<String>? = null,

    @JsonProperty("ftr:hasTest")
    val hasTest: List<IdWrapper>? = null,

    @JsonProperty("ftr:hasBenchmark")
    val hasBenchmark: List<IdWrapper>? = null,

    @JsonProperty("ftr:isApplicableFor")
    val isApplicableFor: IdWrapper? = null,

    @JsonProperty("ftr:supportedBy")
    val supportedBy: IdWrapper? = null
)

data class IdWrapper(
    @JsonProperty("@id")
    val id: String
)