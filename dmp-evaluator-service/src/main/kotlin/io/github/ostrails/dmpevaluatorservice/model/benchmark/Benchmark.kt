package io.github.ostrails.dmpevaluatorservice.model.benchmark

import com.fasterxml.jackson.annotation.JsonProperty


data class BenchmarkJsonLD(
    @JsonProperty("@context")
    val context: Map<String, String> = Companion.defaultContext,

    @JsonProperty("@graph")
    val graph: List<BenchmarkGraphEntry>
){
    companion object {
        val defaultContext = mapOf(
            "ftr" to "https://www.w3id.org/ftr#",
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


data class BenchmarkGraphEntry(
    @JsonProperty("@id")
    val id: String,

    @JsonProperty("@type")
    val type: String = "ftr:Benchmark",

    @JsonProperty("dcterms:title")
    val title: String,

    @JsonProperty("dcterms:description")
    val description: String,

    @JsonProperty("dcat:version")
    val version: String,

    @JsonProperty("ftr:status")
    val status: String? = null,

    @JsonProperty("rdfs:label")
    val label: String? = null,

    @JsonProperty("vivo:abbreviation")
    val abbreviation: String? = null,

    @JsonProperty("dcat:landingPage")
    val landingPage: IdWrapper? = null,

    @JsonProperty("dcat:keyword")
    val keyword: List<String>? = null,

    @JsonProperty("ftr:associatedMetric")
    val associatedMetric: List<IdWrapper>? = null,

    @JsonProperty("ftr:hasAlgorithm")
    val hasAlgorithm: List<IdWrapper>? = null,

    @JsonProperty("dcterms:creator")
    val creators: List<IdWrapper>? = null
)

data class IdWrapper(
    @JsonProperty("@id")
    val id: String
)

