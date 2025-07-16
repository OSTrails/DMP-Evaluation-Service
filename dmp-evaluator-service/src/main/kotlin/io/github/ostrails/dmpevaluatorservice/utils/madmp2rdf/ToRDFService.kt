package io.github.ostrails.dmpevaluatorservice.utils.madmp2rdf

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
import org.springframework.stereotype.Service
import java.io.*
import java.util.*

@Service
class ToRDFService {

    suspend fun jsonToRDF(json: String) {
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

            val mappingResource = this::class.java.classLoader.getResource("rmlmappings/rmlMappings.ttl")
                ?: error("RML mapping resource not found")
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
                return
            }

            val outFile = File(outputDir, "madmp.ttl")
            println("Saving output to: \${outFile.absolutePath}")

            val modelField = RDF4JStore::class.java.getDeclaredField("model").apply { isAccessible = true }
            val model = modelField.get(result) as Model

            val tBoxAndABox = LinkedHashModel().apply { addAll(model) }
            var ontologyNamespaces: Set<org.eclipse.rdf4j.model.Namespace>? = null

            this::class.java.classLoader.getResource("rmlmappings/dcso-4.0.0.ttl")?.let {
                val ontologyFile = File(it.toURI())
                if (ontologyFile.exists()) {
                    val ontologyModel = Rio.parse(ontologyFile.inputStream(), ontologyFile.toURI().toString(), RDFFormat.TURTLE)
                    tBoxAndABox.addAll(ontologyModel)
                    ontologyNamespaces = ontologyModel.namespaces
                    println("Ontology file added.")
                } else println("Ontology file not found at: \${ontologyFile.absolutePath}")
            } ?: println("Ontology resource not found.")

            val repo = SailRepository(SchemaCachingRDFSInferencer(MemoryStore())).apply { init() }
            repo.connection.use { conn: RepositoryConnection ->
                conn.add(tBoxAndABox)
                val inferredModel = LinkedHashModel().apply {
                    conn.getStatements(null, null, null).forEach { add(it.subject, it.predicate, it.`object`, it.context) }
                    ontologyNamespaces?.forEach { setNamespace(it.prefix, it.name) }
                    setNamespace("dcso-ont", "https://w3id.org/dcso/ns/core#")
                    setNamespace("dcso", "https://w3id.org/dcso/ns/core/")
                }
                BufferedWriter(OutputStreamWriter(outFile.outputStream(), Charsets.UTF_8)).use {
                    Rio.write(inferredModel, it, RDFFormat.TURTLE)
                }
            }
        } catch (e: Exception) {
            println("Error during RDF generation: \${e.message}")
            e.printStackTrace()
        }
    }
}