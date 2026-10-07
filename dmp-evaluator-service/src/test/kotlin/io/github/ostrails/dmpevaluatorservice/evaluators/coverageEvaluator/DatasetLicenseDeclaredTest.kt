package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DatasetLicenseDeclaredTest {

    private val licensed = """{"title": "licensed", "license": [{"license_ref": "https://creativecommons.org/licenses/by/4.0/", "start_date": "2026-01-01"}]}"""
    private val unlicensed = """{"title": "unlicensed"}"""

    private fun result(vararg datasets: String) = run("datasetLicenseDeclared", dmp(*datasets)).result

    @Test
    fun `dataset with one licensed distribution passes`() {
        assertEquals(ResultTestEnum.PASS, result("""{"title": "a", "distribution": [$unlicensed, $licensed]}"""))
    }

    @Test
    fun `dataset whose distributions have no license fails`() {
        assertEquals(ResultTestEnum.FAIL, result("""{"title": "a", "distribution": [$licensed]}""", """{"title": "b", "distribution": [$unlicensed]}"""))
    }

    @Test
    fun `blank license_ref does not count`() {
        assertEquals(ResultTestEnum.FAIL, result("""{"title": "a", "distribution": [{"title": "d", "license": [{"license_ref": " "}]}]}"""))
    }

    @Test
    fun `dataset without distributions fails`() {
        assertEquals(ResultTestEnum.FAIL, result("""{"title": "a"}"""))
    }

    @Test
    fun `no datasets fails`() {
        assertEquals(ResultTestEnum.FAIL, result())
    }
}
