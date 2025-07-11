package io.github.ostrails.dmpevaluatorservice.model.metric

import com.fasterxml.jackson.annotation.JsonProperty

data class MetricJsonLD(

    @JsonProperty("@context")
    val context: Map<String, String> = defaultContext,

    @JsonProperty("@graph")
    val graph: List<Any>

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
    val type: Any = "dqv:Metric", // String or List<String>

    @JsonProperty("dcterms:title")
    val title: LangLiteral,

    @JsonProperty("dcterms:description")
    val description: LangLiteral,

    @JsonProperty("dcat:version")
    val version: String,

    @JsonProperty("rdfs:label")
    val label: String? = null,

    @JsonProperty("vivo:abbreviation")
    val abbreviation: String? = null,

    @JsonProperty("dcat:landingPage")
    val landingPage: IdWrapper? = null,

    @JsonProperty("dcat:keyword")
    val keyword: List<LangLiteral>? = null,

    @JsonProperty("ftr:hasTest")
    val hasTest: List<IdWrapper>,

    @JsonProperty("ftr:hasBenchmark")
    val hasBenchmark: List<IdWrapper> ,

    @JsonProperty("ftr:isApplicableFor")
    val isApplicableFor: IdWrapper? = null,

    @JsonProperty("ftr:supportedBy")
    val supportedBy: IdWrapper? = null,

    @JsonProperty("dcat:contactPoint")
    val contactPoint: IdWrapper? = null,

    @JsonProperty("dcterms:creator")
    val creator: List<IdWrapper>? = null,

    @JsonProperty("dcterms:publisher")
    val publisher: IdWrapper? = null,

    @JsonProperty("dqv:inDimension")
    val inDimension: IdWrapper? = null,

    @JsonProperty("dcterms:license")
    val license: IdWrapper? = null,

    @JsonProperty("http://semanticscience.org/resource/SIO_000233")
    val sio233: IdWrapper? = null
)

data class IdWrapper(
    @JsonProperty("@id")
    val id: String
)

data class LangLiteral(
    @JsonProperty("@language") val language: String,
    @JsonProperty("@value") val value: String
)

data class BenchmarkMini(
    @JsonProperty("@id") val id: String,
    @JsonProperty("@type") val type:String =  "ftr:Benchmark",
    val title: String,
    val description: String
)

data class Creator(
    @JsonProperty("@id") val id: String,
    val name: String,
    val email: String
)

data class Organization(val id: String)

data class FAIRPrinciple(
    @JsonProperty("@id")val id: String,
    val label: String,
    val abbreviation: String,
    val description: String
)

