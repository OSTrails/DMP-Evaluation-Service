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
import java.io.File
import java.io.FileInputStream
import java.io.OutputStreamWriter
import java.io.BufferedWriter

class ToRDF {

    suspend fun jsonToRDF(json: String) {
        try {
            // Define the output directory
            val outputDir = File("target/output")
            outputDir.mkdirs()

            // Save the json String to a file in the output directory
            val madmp_file = File(outputDir, "madmp_file.json")
            madmp_file.writeText(json)
            val madmp_file_path = madmp_file.absolutePath
            println("File created at: ${madmp_file.absolutePath}")

            val resource = this::class.java.classLoader.getResource("rmlmappings/rmlMappings.ttl")
            val mappingFile = File(resource?.toURI() ?: error("Resource not found"))
            val mappingContent = mappingFile.readText().replace("{{madmp_json_path}}", madmp_file_path)
            
            // Create a file with the updated path to the madmp JSON file in the output directory
            val updatedMappingFile = File(outputDir, "updated_rmlMappings.ttl")
            updatedMappingFile.writeText(mappingContent)
            val mappingStream = FileInputStream(updatedMappingFile)

            // Load the mapping into a QuadStore
            val rmlStore: QuadStore = QuadStoreFactory.read(mappingStream)

            // Records factory for data sources
            val factory = RecordsFactory(mappingFile.parent, mappingFile.parent)

            // Output store
            val outputStore = RDF4JStore()

            //Functions
            //val functionAgent = AgentFactory.createFromFnO("fno/functions_idlab.ttl", "fno/functions_idlab_test_classes_java_mapping.ttl");

            // Create the Executor
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
                println("RDF output file will be saved at: ${out_file.absolutePath}")
                out_file.parentFile.mkdirs()

                // Extract RDF4J model and set namespaces
                val modelField = RDF4JStore::class.java.getDeclaredField("model")
                modelField.isAccessible = true
                val model = modelField.get(it) as org.eclipse.rdf4j.model.Model

                // Set namespaces
                model.setNamespace("dcat", "http://www.w3.org/ns/dcat#")
                model.setNamespace("dmp", "http://purl.org/dmp#")
                model.setNamespace("foaf", "http://xmlns.com/foaf/0.1/")
                model.setNamespace("xsd", "http://www.w3.org/2001/XMLSchema#")
                model.setNamespace("dcso", "https://w3id.org/dcso/ns/core#")
                model.setNamespace("dcterms", "http://purl.org/dc/terms/")

                // Write the model using RDF4J's Rio API (not it.write)
                BufferedWriter(OutputStreamWriter(out_file.outputStream())).use { writer ->
                    org.eclipse.rdf4j.rio.Rio.write(model, writer, org.eclipse.rdf4j.rio.RDFFormat.TURTLE)
                }
            } ?: println("No RDF output was generated. 'result' was null.")

        } catch (e: Exception) {
            println("An error occurred during RDF generation: ${e.message}")
            e.printStackTrace()
        }
    }
}
