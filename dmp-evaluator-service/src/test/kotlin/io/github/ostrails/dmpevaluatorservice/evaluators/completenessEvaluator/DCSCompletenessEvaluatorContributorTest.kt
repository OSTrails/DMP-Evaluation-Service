package io.github.ostrails.dmpevaluatorservice.evaluators.completenessEvaluator

import io.github.ostrails.dmpevaluatorservice.database.model.TestRecord
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DCSCompletenessEvaluatorContributorTest {

    private val evaluator = DCSCompletenessEvaluator()

    private val testRecord = TestRecord(
        id = "test-id",
        title = "Roles in RDM Defined",
        description = "Verifies that contributor roles related to data management are clearly defined in the DMP.",
        license = "https://opensource.org/license/mit",
        version = "0.0.1",
        metricImplemented = null,
        evaluator = "DCSCompletenessEvaluator",
        functionEvaluator = "contributorValuesPresent",
    )

    private fun contributor(roleJson: String?) =
        """{"name": "Jane Doe", "contributor_id": {"identifier": "https://orcid.org/0000-0000-0000-0000", "type": "orcid"}""" +
            (if (roleJson != null) """, "role": $roleJson}""" else "}")

    private fun evaluateContributors(contributorsJson: String) =
        evaluator.contributorValuesPresent(
            Json.parseToJsonElement("""{"dmp": {"contributor": $contributorsJson}}""").jsonObject,
            "report-id",
            testRecord
        )

    @Test
    fun `all contributors with roles pass`() {
        val result = evaluateContributors("""[${contributor("""["Data Steward"]""")}, ${contributor("""["Data Manager", "PI"]""")}]""")
        assertEquals(ResultTestEnum.PASS, result.result)
        assertEquals(null, result.affectedElements)
    }

    @Test
    fun `contributor without role fails even when others have roles`() {
        val result = evaluateContributors("""[${contributor("""["Data Steward"]""")}, ${contributor(null)}]""")
        assertEquals(ResultTestEnum.FAIL, result.result)
        assertEquals(listOf("contributor[1]"), result.affectedElements)
    }

    @Test
    fun `empty role list fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluateContributors("[${contributor("[]")}]").result)
    }

    @Test
    fun `blank role entry fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluateContributors("""[${contributor("""["Data Steward", " "]""")}]""").result)
    }

    @Test
    fun `role given as a string instead of a list fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluateContributors("""[${contributor("\"Data Steward\"")}]""").result)
    }

    @Test
    fun `no contributors fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluateContributors("[]").result)
    }

    @Test
    fun `non-object contributor entry fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluateContributors("""[${contributor("""["Data Steward"]""")}, "not an object"]""").result)
    }
}
