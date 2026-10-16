package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class CoverageGroupsRegistrationTest {

    private val evaluator = DCSCoverageEvaluator()

    @Test
    fun `every group function is registered in the coverage evaluator`() {
        val groups = listOf(DatasetProvenanceTests.functions, SecurityAccessTests.functions, RepositoryStorageTests.functions)
        groups.forEach { group ->
            group.forEach { (name, function) -> assertSame(function, evaluator.functionMap[name], "'$name' is not registered") }
        }
    }
}
