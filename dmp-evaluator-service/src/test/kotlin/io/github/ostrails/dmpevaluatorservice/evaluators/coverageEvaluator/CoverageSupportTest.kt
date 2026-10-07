package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.testRecord
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CoverageSupportTest {

    private fun evaluate(subjects: List<Subject>, check: (JsonObject) -> SubjectCheck) =
        evaluateSubjects(dmp(), "report-id", testRecord("f"), "gen", subjects, "dataset", "requirement",
            ResultTestEnum.FAIL, "Nothing to check.", check)

    private fun subjects(vararg titles: String) = datasetSubjects(dmp(*titles.map { """{"title": "$it"}""" }.toTypedArray()))

    private fun byTitle(vararg outcomes: Pair<String, SubjectCheck>): (JsonObject) -> SubjectCheck =
        { json -> outcomes.toMap().getValue(json.text("title")!!) }

    @Test
    fun `problem beats undetermined and ok`() {
        val result = evaluate(subjects("a", "b", "c"), byTitle(
            "a" to SubjectCheck.Ok("ok"), "b" to SubjectCheck.Undetermined("?"), "c" to SubjectCheck.Problem("bad")))
        assertEquals(ResultTestEnum.FAIL, result.result)
        assertEquals(listOf("dataset[1]", "dataset[2]"), result.affectedElements)
    }

    @Test
    fun `undetermined beats ok`() {
        val result = evaluate(subjects("a", "b"), byTitle("a" to SubjectCheck.Ok("ok"), "b" to SubjectCheck.Undetermined("?")))
        assertEquals(ResultTestEnum.INDETERMINATE, result.result)
    }

    @Test
    fun `all ok passes`() {
        val result = evaluate(subjects("a", "b"), byTitle("a" to SubjectCheck.Ok("ok"), "b" to SubjectCheck.Ok("ok")))
        assertEquals(ResultTestEnum.PASS, result.result)
        assertEquals(null, result.affectedElements)
    }

    @Test
    fun `no subjects uses the empty result`() {
        val result = evaluate(emptyList()) { SubjectCheck.Ok("ok") }
        assertEquals(ResultTestEnum.FAIL, result.result)
        assertEquals("Nothing to check.", result.guidance?.summary)
    }

    @Test
    fun `reused scope keeps reused datasets and marks unclassified ones undetermined`() {
        val maDMP = dmp("""{"title": "new", "is_reused": false}""", """{"title": "old", "is_reused": true}""", """{"title": "unknown"}""")
        val reused = datasetSubjects(maDMP, DatasetScope.REUSED)
        assertEquals(listOf("dataset[1]", "dataset[2]"), reused.map { it.path })
        assertEquals(null, reused[0].preset)
        assertEquals(SubjectCheck.Undetermined(CANNOT_CLASSIFY_REUSE), reused[1].preset)
        assertEquals(listOf("dataset[0]", "dataset[2]"), datasetSubjects(maDMP, DatasetScope.NEW).map { it.path })
    }

    @Test
    fun `dataset without distributions becomes a problem subject`() {
        val distributions = distributionSubjects(datasetSubjects(dmp(
            """{"title": "a", "distribution": [{"title": "d1"}, {"title": "d2"}]}""", """{"title": "b"}""")))
        assertEquals(listOf("dataset[0].distribution[0]", "dataset[0].distribution[1]", "dataset[1]"), distributions.map { it.path })
        assertEquals(SubjectCheck.Problem("No distributions declared."), distributions[2].preset)
    }
}
