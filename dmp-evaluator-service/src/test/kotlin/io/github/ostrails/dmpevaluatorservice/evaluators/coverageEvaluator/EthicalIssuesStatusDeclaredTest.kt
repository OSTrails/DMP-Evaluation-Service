package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EthicalIssuesStatusDeclaredTest {

    private fun evaluate(value: String?) =
        run("ethicalIssuesStatusDeclared", dmp(dmpFields = value?.let { """"ethical_issues_exist": "$it"""" })).result

    @Test
    fun `yes or no passes`() {
        assertEquals(ResultTestEnum.PASS, evaluate("yes"))
        assertEquals(ResultTestEnum.PASS, evaluate("no"))
    }

    @Test
    fun `unknown is indeterminate`() {
        assertEquals(ResultTestEnum.INDETERMINATE, evaluate("unknown"))
    }

    @Test
    fun `missing or invalid value fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(null))
        assertEquals(ResultTestEnum.FAIL, evaluate("maybe"))
    }

    @Test
    fun `missing dmp object fails`() {
        assertEquals(ResultTestEnum.FAIL, run("ethicalIssuesStatusDeclared", Json.parseToJsonElement("{}").jsonObject).result)
    }
}
