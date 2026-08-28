package io.github.ostrails.dmpevaluatorservice.utils.dbpatchers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@PublishedApi
internal val log: Logger = LoggerFactory.getLogger("MongoPatcherUtils")

// One-time migration for the ResultTestEnum typo fix (issue #20): rewrites any
// persisted evaluation still storing the misspelled "INDERTERMINATED" enum value
// to the corrected "INDETERMINATE". Safe to run on every startup - it's a no-op
// once no documents match.
fun migrateIndeterminatedResultTypo(mongoTemplate: MongoTemplate) {
    val query = Query(Criteria.where("result").`is`("INDERTERMINATED"))
    val update = Update.update("result", "INDETERMINATE")
    val result = mongoTemplate.updateMulti(query, update, "evaluations")
    if (result.modifiedCount > 0) {
        log.info("Migrated ${result.modifiedCount} evaluation(s) from 'INDERTERMINATED' to 'INDETERMINATE'")
    }
}

inline fun <reified T : Any> patchMongoCollection(
    mongoTemplate: MongoTemplate,
    collectionName: String
) {
    val defaultInstance = createDefaultInstance<T>()
    if (defaultInstance == null) {
        log.error("Could not create default instance for ${T::class.simpleName}")
        return
    }

    log.info("Default instance created: $defaultInstance")
    val updates = Update()
    val kClass = T::class

    for (property in kClass.memberProperties) {
        property.isAccessible = true
        val name = property.name
        val value = property.get(defaultInstance)

        if (value != null) {
            updates.setOnInsert(name, value)
        }
    }

    val result = mongoTemplate.updateMulti(Query(), updates, T::class.java, collectionName)
    log.info("Patched ${result.modifiedCount} documents in '$collectionName'")
}

inline fun <reified T : Any> createDefaultInstance(): T? {
    return try {
        val constructor = T::class.constructors.firstOrNull {
            it.parameters.all { p -> p.isOptional || p.type.isMarkedNullable }
        }
        constructor?.callBy(emptyMap())
    } catch (e: Exception) {
        log.warn("Could not create instance of ${T::class.simpleName}: ${e.message}")
        null
    }
}

