package io.github.ostrails.dmpevaluatorservice.model

enum class ResultTestEnum {
    PASS,
    FAIL,
    ERROR,

    @Deprecated("Misspelled — use INDETERMINATE. Kept so existing Mongo documents with this value still deserialize; see utils/dbpatchers for the migration that rewrites them.")
    INDERTERMINATED,

    INDETERMINATE
}