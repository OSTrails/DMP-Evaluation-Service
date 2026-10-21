package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DataOwnershipRoleDeclaredTest {

    private fun evaluate(contributors: String) = run("dataOwnershipRoleDeclared", dmp(dmpFields = """"contributor": [$contributors]""")).result

    @Test
    fun `contributor with owner role passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"name": "Jane Doe", "role": ["Data Owner", "Data Steward"]}"""))
    }

    @Test
    fun `owner identified only by contributor id passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("""{"role": ["owner"], "contributor_id": {"identifier": "https://orcid.org/0000-0002-1825-0097", "type": "orcid"}}"""))
    }

    @Test
    fun `no owner role fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"name": "Jane Doe", "role": ["Data Steward"]}"""))
    }

    @Test
    fun `owner without name or id fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate("""{"role": ["owner"]}"""))
    }

    @Test
    fun `no contributors fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(""))
    }
}
