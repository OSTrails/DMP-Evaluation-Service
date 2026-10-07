package io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator

import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.dmp
import io.github.ostrails.dmpevaluatorservice.evaluators.coverageEvaluator.CoverageTestSupport.run
import io.github.ostrails.dmpevaluatorservice.model.ResultTestEnum
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ContributorPidsDeclaredTest {

    private fun contributor(idType: String?, affiliation: String? = null): String {
        val id = if (idType != null) """, "contributor_id": {"identifier": "0000-0003-0644-4174", "type": "$idType"}""" else ""
        val affiliations = if (affiliation != null) """, "affiliation": [$affiliation]""" else ""
        return """{"name": "Jane Doe", "role": ["DataManager"]$id$affiliations}"""
    }

    private fun evaluate(vararg contributors: String) =
        run("contributorPidsDeclared", dmp(dmpFields = """"contributor": [${contributors.joinToString()}]""")).result

    @Test
    fun `orcid contributors pass, with or without affiliations`() {
        val withRor = """{"name": "TU Wien", "affiliation_id": {"identifier": "04d836q62", "type": "ror"}}"""
        assertEquals(ResultTestEnum.PASS, evaluate(contributor("orcid"), contributor("ORCID", withRor), contributor("isni")))
    }

    @Test
    fun `missing or non-persistent contributor id fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(contributor(null)))
        assertEquals(ResultTestEnum.FAIL, evaluate(contributor("openid")))
    }

    @Test
    fun `affiliation without organisation pid fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate(contributor("orcid", """{"name": "TU Wien"}""")))
        assertEquals(ResultTestEnum.FAIL, evaluate(contributor("orcid", """{"name": "TU Wien", "affiliation_id": {"identifier": "x", "type": "other"}}""")))
    }

    @Test
    fun `no contributors fails`() {
        assertEquals(ResultTestEnum.FAIL, evaluate())
    }
}
