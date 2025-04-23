package io.github.ostrails.dmpevaluatorservice.utils.madmp2rdf

//import be.ugent.idlab.knows.functions.agent.Agent;
//import be.ugent.idlab.knows.functions.agent.AgentFactory;
import be.ugent.rml.Executor;
import be.ugent.rml.Utils;
import be.ugent.rml.records.RecordsFactory;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.store.QuadStoreFactory;
import be.ugent.rml.store.RDF4JStore;
import be.ugent.rml.term.NamedNode;

import java.io.*;

class TORDF () {

    suspend fun jsonToRDF(json: String) {
        try {
            File mappingFile = Utils.getFile("rmlmappings/rmlMappings.ttl");

            // Get the mapping string stream
            InputStream mappingStream = new FileInputStream(mappingFile);

            // Load the mapping in a QuadStore
            QuadStore rmlStore = QuadStoreFactory.read(mappingStream);

            // Set up the basepath for the records factory, i.e., the basepath for the (local file) data sources
            RecordsFactory factory = new RecordsFactory(mappingFile.getParent(), mappingFile.getParent());

            // Set up the functions used during the mapping
            //Agent functionAgent = AgentFactory.createFromFnO("fno/functions_idlab.ttl", "fno/functions_idlab_test_classes_java_mapping.ttl");

            // Set up the outputstore (needed when you want to output something else than nquads
            QuadStore outputStore = new RDF4JStore();

            // Create the Executor
            Executor executor = new Executor(rmlStore, factory, outputStore, Utils.getBaseDirectiveTurtleOrDefault(mappingStream, "http://purl.org/dmp#"), null);

            // Execute the mapping
            QuadStore result = executor.execute(null).get(new NamedNode("rmlmapper://default.store"));

            // Output the result
            outFile = new File("madmps_rdf/ex9-dmp-long.json.ttl");
            FileOutputStream fos = new FileOutputStream(outFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            BufferedWriter out = new BufferedWriter(osw);
            result.write(out, "turtle");
            out.flush();
            out.close();
            fos.close();

        } catch (Exception e) {
            fail("No exception was expected.");
        }
    
    }

}