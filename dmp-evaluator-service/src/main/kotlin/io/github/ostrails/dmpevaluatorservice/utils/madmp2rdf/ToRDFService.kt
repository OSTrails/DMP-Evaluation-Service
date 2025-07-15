package io.github.ostrails.dmpevaluatorservice.utils.madmp2rdf

import be.ugent.idlab.knows.functions.agent.Agent
import be.ugent.idlab.knows.functions.agent.AgentFactory
import be.ugent.rml.Executor
import be.ugent.rml.Utils
import be.ugent.rml.records.RecordsFactory
import be.ugent.rml.store.QuadStore
import be.ugent.rml.store.QuadStoreFactory
import be.ugent.rml.store.RDF4JStore
import be.ugent.rml.term.NamedNode
import io.github.ostrails.dmpevaluatorservice.exceptionHandler.ResourceNotFoundException
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.OutputStreamWriter
import java.io.BufferedWriter

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray

@Service
class ToRDFService {

    suspend fun jsonToRDF(json: String) {
        try {
            // Define the output directory
            val outputDir = File("target/output")
            outputDir.mkdirs()

            // Create a file in the output directory to store the madmp JSON
            val madmp_file = File(outputDir, "madmp_file.json")

            // Generate random IDs for all distributions (dmp.dataset[*].distribution[*]) and inject into JSON
            val jsonObject = kotlinx.serialization.json.Json.parseToJsonElement(json).jsonObject.toMutableMap()
            val dmp = jsonObject["dmp"]?.jsonObject?.toMutableMap()

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

            // Generate random IDs for all fundings (dmp.project[*].funding[*]) and inject into JSON
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

            if (dmp != null) {
                jsonObject["dmp"] = kotlinx.serialization.json.JsonObject(dmp)
            }
            val updatedJson = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                kotlinx.serialization.json.JsonObject(jsonObject)
            )
            madmp_file.writeText(updatedJson)

            val madmp_file_path = madmp_file.absolutePath
            println("File created at: ${madmp_file.absolutePath}")

            // Load the RML mappings file and update it with the madmp JSON file path
            val resource = this::class.java.classLoader.getResource("rmlmappings/rmlMappings.ttl")
            val mappingFile = File(resource?.toURI() ?: error("Resource not found"))
            val mappingContent = mappingFile.readText().replace("{{madmp_json_path}}", madmp_file_path)
            
            // Write the updated rml mappings to the output directory
            val updatedMappingFile = File(outputDir, "updated_rmlMappings.ttl")
            updatedMappingFile.writeText(mappingContent)
            val mappingStream = FileInputStream(updatedMappingFile)

            // Load the mapping into a QuadStore. The mappings file is pointing to the madmp JSON file.
            val rmlStore: QuadStore = QuadStoreFactory.read(mappingStream)

            // Records factory for data sources
            val factory = RecordsFactory(mappingFile.parent, mappingFile.parent)

            // Output store
            val outputStore = RDF4JStore()

            //Functions
            //val functionAgent = AgentFactory.createFromFnO("fno/functions_idlab.ttl", "fno/functions_idlab_test_classes_java_mapping.ttl");

            // Create the RML Mappings Executor
            val executor = Executor(
            rmlStore,
            factory,
            outputStore,
            Utils.getBaseDirectiveTurtleOrDefault(mappingStream, "http://purl.org/dmp#"),
            null,
            )

            val result = executor.execute(null).get(NamedNode("rmlmapper://default.store"))

            result?.let {
                val out_file = File(outputDir, "madmp.ttl")
                println("Save output file at: ${out_file.absolutePath}")
                out_file.parentFile.mkdirs()

                // Extract RDF4J model and set namespaces
                val modelField = RDF4JStore::class.java.getDeclaredField("model")
                modelField.isAccessible = true
                val model = modelField.get(it) as org.eclipse.rdf4j.model.Model

                // Load the Ontology file and append it to the out_file
                val tBoxAndABox = org.eclipse.rdf4j.model.impl.LinkedHashModel()
                tBoxAndABox.addAll(model)  // Add the MaDMP model


                val ontologyResource = this::class.java.classLoader.getResource("rmlmappings/dcso-4.0.0.ttl")
                ontologyResource?.let { resource ->
                    val ontologyFile = File(resource.toURI())
                    if (ontologyFile.exists()) {
                        val ontologyModel = org.eclipse.rdf4j.rio.Rio.parse(
                            ontologyFile.inputStream(),
                            ontologyFile.toURI().toString(),
                            org.eclipse.rdf4j.rio.RDFFormat.TURTLE
                        )

                        // Add ontology triples
                        tBoxAndABox.addAll(ontologyModel) 
                        println("Ontology file appended to the model.")

                        // Copy namespaces from ontology model
                        ontologyModel.namespaces.forEach { ns ->
                            tBoxAndABox.setNamespace(ns.prefix, ns.name)
                        }
                        println("Namespaces from ontology file copied to the model.")
                        
                        println("Ontology file appended to the output file.")
                    } else {
                        println("Ontology file not found at: ${ontologyFile.absolutePath}")
                    }
                } ?: println("Ontology resource not found in classpath.")   

                // Append ontology triples to the output file
                BufferedWriter(OutputStreamWriter(out_file.outputStream(), Charsets.UTF_8)).use { writer ->
                    org.eclipse.rdf4j.rio.Rio.write(tBoxAndABox, writer, org.eclipse.rdf4j.rio.RDFFormat.TURTLE)
                }

            } ?: println("No RDF output was generated. 'result' was null.")

        } catch (e: Exception) {
            println("An error occurred during RDF generation: ${e.message}")
            e.printStackTrace()
        }
    }
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

# Unsolved issues
hasProjectId and hasProjectIdType do not exist in the dcso.4.0.0.ttl core ontology.

no licenseName datatype property in the dcso.4.0.0.ttl core ontology.

the range of the datatype property dcterms:format is a string but a list of strings is given.

Some IDs like project_id are datatype properties and other IDs like contributor_id is an objectProperty

There is a dcat:Distribution and dcso:Distribution in the dcso.4.0.0.ttl core ontology.

(Distribution has no ID)

Can really multiple licenses be assigned to a dataset?

# TODOs
Add reasoner to infere additional triples from the OWL/RDFs annotations in the dcso-4.0.0.ttl ontology.



*/