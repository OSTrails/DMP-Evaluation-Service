package io.github.ostrails.dmpevaluatorservice.utils

import io.github.ostrails.dmpevaluatorservice.database.model.Evaluation
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum

import be.ugent.rml.Executor
import be.ugent.rml.Utils
import be.ugent.rml.records.RecordsFactory
import be.ugent.rml.store.QuadStoreFactory
import be.ugent.rml.store.RDF4JStore
import be.ugent.rml.term.NamedNode
import kotlinx.serialization.json.*
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.impl.LinkedHashModel
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer
import org.eclipse.rdf4j.sail.memory.MemoryStore
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.model.vocabulary.SHACL
import org.springframework.stereotype.Service
import org.eclipse.rdf4j.sail.shacl.ShaclSail

import java.io.*
import java.util.*



fun jsonToRDF(json: String): String {
    try {
        val outputDir = File("target/output").apply { mkdirs() }
        val madmpFile = File(outputDir, "madmp_file.json")

        val jsonObject = Json.parseToJsonElement(json).jsonObject.toMutableMap()
        val dmp = jsonObject["dmp"]?.jsonObject?.toMutableMap()

        // Generate random IDs for all distribution (dmp.dataset[*].distribution[*]) and inject into jsonObject
        val datasets = dmp?.get("dataset")?.jsonArray?.map { it.jsonObject.toMutableMap() }?.toMutableList()
        if (datasets != null) {
            for (dataset in datasets) {
                val distributions = dataset["distribution"]?.jsonArray?.map { it.jsonObject.toMutableMap() }?.toMutableList()
                if (distributions != null) {
                    for (distribution in distributions) {
                        distribution["generated_id"] = kotlinx.serialization.json.JsonPrimitive(java.util.UUID.randomUUID().toString())
                    }
                    dataset["distribution"] = kotlinx.serialization.json.JsonArray(distributions.map { kotlinx.serialization.json.JsonObject(it) })
                }
            }
            dmp["dataset"] = kotlinx.serialization.json.JsonArray(datasets.map { kotlinx.serialization.json.JsonObject(it) })
        }

        // Generate random IDs for all fundings (dmp.project[*].funding[*]) and inject into jsonObject
        val projects = dmp?.get("project")?.jsonArray?.map { it.jsonObject.toMutableMap() }?.toMutableList()
        if (projects != null) {
            for (project in projects) {
                val fundings = project["funding"]?.jsonArray?.map { it.jsonObject.toMutableMap() }?.toMutableList()
                if (fundings != null) {
                    for (funding in fundings) {
                        funding["generated_id"] = kotlinx.serialization.json.JsonPrimitive(java.util.UUID.randomUUID().toString())
                    }
                    project["funding"] = kotlinx.serialization.json.JsonArray(fundings.map { kotlinx.serialization.json.JsonObject(it) })
                }
            }
            dmp["project"] = kotlinx.serialization.json.JsonArray(projects.map { kotlinx.serialization.json.JsonObject(it) })
        }

        // Update the jsonObject with the modified dmp structure and write it to the madmp_file
        if (dmp != null) {
            jsonObject["dmp"] = kotlinx.serialization.json.JsonObject(dmp)
        }
        val updatedJson = kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.JsonObject(jsonObject)
        )
        madmpFile.writeText(updatedJson)
        println("File created at: ${madmpFile.absolutePath}")

        val classLoader = object {}.javaClass.classLoader
        val mappingResource = classLoader.getResource("rmlmappings/rmlMappings.ttl")?: error("RML mapping resource not found")
        val mappingFile = File(mappingResource.toURI())
        val mappingContent = mappingFile.readText().replace("{{madmp_json_path}}", madmpFile.absolutePath)
        val updatedMappingFile = File(outputDir, "updated_rmlMappings.ttl").apply {
            writeText(mappingContent)
        }

        val mappingStream = FileInputStream(updatedMappingFile)
        val rmlStore = QuadStoreFactory.read(mappingStream)
        val recordsFactory = RecordsFactory(mappingFile.parent, mappingFile.parent)
        val outputStore = RDF4JStore()

        val executor = Executor(
            rmlStore,
            recordsFactory,
            outputStore,
            Utils.getBaseDirectiveTurtleOrDefault(mappingStream, "https://w3id.org/dcso/ns/core#"),
            null
        )

        val result = executor.execute(null)[NamedNode("rmlmapper://default.store")]
        if (result == null) {
            println("No RDF output generated.")
            return "No RDF output generated."
        }

        println("Saving output to: \${outFile.absolutePath}")

        val modelField = RDF4JStore::class.java.getDeclaredField("model").apply { isAccessible = true }
        val model = modelField.get(result) as Model

        val tBoxAndABox = LinkedHashModel().apply { addAll(model) }
        var ontologyNamespaces: Set<org.eclipse.rdf4j.model.Namespace>? = null

        
        val resourceUrl = Thread.currentThread().contextClassLoader
            .getResource("rmlmappings/dcso-4.0.1.ttl")

        resourceUrl?.let { url ->
            url.openStream().use { inputStream ->
                val ontologyModel = Rio.parse(inputStream, url.toString(), RDFFormat.TURTLE)
                tBoxAndABox.addAll(ontologyModel)
                ontologyNamespaces = ontologyModel.namespaces
                println("Ontology file added.")
            }
        } ?: println("Ontology resource not found.")

        val repo = SailRepository(SchemaCachingRDFSInferencer(MemoryStore())).apply { init() }
        repo.connection.use { conn: RepositoryConnection ->
            conn.add(tBoxAndABox)
            val inferredModel = LinkedHashModel().apply {
                conn.getStatements(null, null, null).forEach { add(it.subject, it.predicate, it.`object`, it.context) }
                ontologyNamespaces?.forEach { setNamespace(it.prefix, it.name) }
                setNamespace("dcso", "https://w3id.org/dcso/ns/core#")
                setNamespace("dcso-inst", "https://w3id.org/dcso/ns/core/")
            }
            val stringWriter = StringWriter()
            Rio.write(inferredModel, stringWriter, RDFFormat.TURTLE)
            return stringWriter.toString()
            }
        } catch (e: Exception) {
        println("Error during RDF generation: ${e.message}")
        e.printStackTrace()
        return "Error during RDF generation: ${e.message}"
    }
}

fun validateCompletenessWithSHACL(maDMP: String): Evaluation? {
    val baseURI = "https://w3id.org/validation/ns/core#"

    // Parse the maDMP Turtle content into a Model
    val maDMPModel: Model = Rio.parse(maDMP.byteInputStream(), baseURI, RDFFormat.TURTLE)

    var evaluationReport: Evaluation?

    val shapesDir = File("src/main/resources/shapes")

    val shapeFile = shapesDir.listFiles { file -> file.name == "dcs-completeness.ttl" }?.firstOrNull()
        ?: throw IllegalStateException("Shape file dcs-completeness.ttl not found in shapes directory")
    
    // Parse shape model
    val shapeModel = Rio.parse(shapeFile.inputStream(), baseURI, RDFFormat.TURTLE)

    // Register the base prefix ":" for prettier output (if not already present)
    shapeModel.setNamespace("", baseURI)
    
    // Extract all shapes (NodeShape or PropertyShape)
    val shapeSubjects = shapeModel
        .filter(null, RDF.TYPE, null)
        .filter { it.`object` == SHACL.NODE_SHAPE || it.`object` == SHACL.PROPERTY_SHAPE }
        .map { stmt ->
            val subj = stmt.subject
            when {
                subj.isIRI -> shapeModel.namespaces.firstOrNull { ns -> subj.stringValue().startsWith(ns.name) }?.let { ns ->
                    val localName = subj.stringValue().removePrefix(ns.name)
                    if (ns.prefix.isEmpty()) ":$localName" else "${ns.prefix}:$localName"
                } ?: "<${subj.stringValue()}>"
                else -> subj.stringValue()
            }
        }
        .distinct()

    // Create a comma-separated list for the detailed report
    val shapeListString = shapeSubjects.joinToString(", ")

    // Create a SHACL-enabled repository
    val shaclSail = ShaclSail(MemoryStore())
    val repo = SailRepository(shaclSail)
    repo.init()

    // Load shapes into the SHACL Sail
    repo.connection.use { conn ->
        conn.begin()
        conn.add(shapeModel)
        conn.commit()

        // Validate the maDMPModel against the loaded shapes
        try {
            conn.begin()
            conn.add(maDMPModel)
            conn.commit()

            evaluationReport =
                Evaluation(
                    evaluationId = UUID.randomUUID().toString(),
                    title = "SHACL Validation with shapes from the file: ${shapeFile.name}",
                    result = ResultTestEnum.PASS,
                    details = "Validation based on the file ${shapeFile.name} which has following shapes: ${shapeListString}",
                    reportId = null,
                )
            
        } catch (e: Exception) {
            evaluationReport =
                Evaluation(
                    evaluationId = UUID.randomUUID().toString(),
                    title = "SHACL Validation for ${shapeFile.name}",
                    result = ResultTestEnum.FAIL,
                    details = "Validation failed: ${e.message}",
                    reportId = null,
                )
            
        }
    }

    repo.shutDown()

    return evaluationReport
}


/*

# How to build
Go to the rmlmapper-java directory and execute:
```
mvn install -DskipTests=true
```

# How to test
In the root project directory `dmp-evaluator-serivce` run:
```
mvn test -Dtest=io.github.ostrails.dmpevaluatorservice.ToRDFServiceTest
```

# Status:
dmp:
* class assignment: mapped
* properties:
    * description: mapped
    * title: mapped
    * language: mapped
    * created: mapped
    * modified: mapped
    * dmp_id: mapped
        * identifier: mapped
        * doi: mapped
    * contact: mapped
        * name: mapped
        * mbox: mapped
        * contactId: mapped
            * identifier: mapped
            * type: mapped
    * contributor: mapped
        * name: mapped
        * mbox: mapped
        * contributor_id: mapped
            * identifier: mapped
            * type: mapped
        * role:
    * ethical_issues_exist: mapped
    * ethical_issues_report: mapped
    * ethical_issues_description: mapped
    * project: mapped
        * title: mapped
        * description: mapped
        * start: mapped
        * end: mapped
        * project_id: mapped (not in ontology)
        * project_id_type (not in ontology)
        * funding: mapped
            * funder_name: mapped
            * funder_id: mapped
                * identifier: mapped
                * type: mapped
            * grant_id: mapped
                * identifier: mapped
                * type: mapped
    * dataset: mapped
        * dataset_id: mapped
            * identifier: mapped    
            * type: mapped
        * title: mapped
        * personal_data: mapped
        * sensitive_data: mapped
        * type: mapped
        * description: mapped
        * distribution: mapped
            * title: mapped
            * format: mapped
            * byte_size: mapped
            * data_access: mapped
            * license: mapped
                * license_name: mapped (not in ontology)
                * license_ref: mapped
                * start_date: mapped
            * host:
                * title: mapped
                * url: mapped
                * host_id_type: mapped
                * description: mapped
                * supports_versioning: mapped
                * storage_type: mapped
                * pid_system: mapped
            * available_until: mapped

# Improvements
generate md5 hashsums for instance IRIs

mapped host_id_type to dcso:identifierType

ex9-dmp-long.json has the same dataset identifiers for every dataset. I changed 
the handle IDs so that they can be distinguished. it is important for the ID 
generation.

# Unsolved issues in the dcso.4.0.0.ttl core ontology
hasProjectId and hasProjectIdType do not exist

no licenseName datatype property

the range of the datatype property dcterms:format is a string but a list of strings is given.

Some IDs like project_id are datatype properties and other IDs like contributor_id is an objectProperty

There is a dcat:Distribution and dcso:Distribution in the dcso.4.0.0.ttl core ontology.

(Distribution has no ID)

Can really multiple licenses be assigned to a dataset?

*/