package io.github.ostrails.dmpevaluatorservice

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ToRDFServiceTest {

    @Test
    fun ex9LongToRDFTest() {
        // This test should load a JSON maDMP from src/main/kotlin/io/github/ostrails/dmpevaluatorservice/utils/madmp2rdf/madmps_json/ex9-dmp-long.json
        // It should send it via a POST request to http://localhost:8080/assess/mappingRDF 
        // it should send it as a multipart/form-data with the key "maDMP"
        // The response should be a 200 OK with the RDF representation of the maDMP
        // It should basically execute curl -X POST http://localhost:8080/assess/mappingRDF -F 'maDMP=@src/main/kotlin/io/github/ostrails/dmpevaluatorservice/utils/madmp2rdf/madmps_json/ex9-dmp-long.json;type=application/json'


    }

}
