package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DistributionLicensePresentTest {

    private val licensed = """{"title": "licensed", "license": [{"license_ref": "https://creativecommons.org/licenses/by/4.0/", "start_date": "2026-01-01"}]}"""
    private val unlicensed = """{"title": "unlicensed"}"""

    private fun evaluate(vararg datasets: String) = run("distributionLicensePresent", dmp(*datasets))

    @Test
    fun `all distributions licensed passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"title": "a", "distribution": [$licensed, $licensed]}""").result)
    }

    @Test
    fun `one unlicensed distribution fails and is reported`() {
        val result = evaluate("""{"title": "a", "distribution": [$licensed, $unlicensed]}""")
        assertEquals(ResultTestEnum.FAIL, result.result)
        assertEquals(listOf("dataset[0].distribution[1]"), result.affectedElements)
    }

    @Test
    fun `dataset without distributions fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"title": "a"}""").result)
    }

    @Test
    fun `no datasets fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate().result)
    }
}
