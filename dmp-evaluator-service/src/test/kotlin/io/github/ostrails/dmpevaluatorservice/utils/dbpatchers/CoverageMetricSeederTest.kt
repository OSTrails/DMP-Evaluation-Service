package io.github.ostrails.dmpevaluatorservice.utils.dbpatchers

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.DCSCoverageEvaluator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoverageMetricSeederTest {

    private val specs = CoverageMetricSeeder.specs

    @Test
    fun `every coverage function is seeded exactly once`() {
        assertEquals(DCSCoverageEvaluator().functionMap.keys.sorted(), specs.map { it.function }.sorted())
    }

    @Test
    fun `ids are unique object ids`() {
        val ids = specs.flatMap { listOf(it.metricId, it.testId) }
        assertEquals(ids.size, ids.toSet().size, "duplicate ids")
        ids.forEach { assertTrue(it.matches(Regex("[0-9a-f]{24}")), "'$it' is not a 24-character hex ObjectId") }
    }

    @Test
    fun `abbreviations are unique`() {
        val abbreviations = specs.map { it.abbreviation }
        assertEquals(abbreviations.size, abbreviations.toSet().size, "duplicate abbreviations")
    }
}
