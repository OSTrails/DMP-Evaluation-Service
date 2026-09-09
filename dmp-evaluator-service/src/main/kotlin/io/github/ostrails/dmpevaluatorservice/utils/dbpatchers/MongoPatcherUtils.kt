package io.github.ostrails.dmpevaluatorservice.utils.dbpatchers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
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
    val kClass = T::class
    // Only properties backed by a constructor parameter that declares its own
    // default are safe to backfill - a required parameter (e.g. a non-null
    // `title: String` with no default) has no real "default" to fall back on,
    // even though createDefaultInstance() had to fill it with a placeholder
    // just to be able to construct the instance at all.
    val backfillableNames = kClass.primaryConstructor?.parameters
        ?.filter { it.isOptional }
        ?.mapNotNull { it.name }
        ?.toSet() ?: emptySet()
    var totalPatched = 0L

    for (property in kClass.memberProperties) {
        if (property.name !in backfillableNames) continue
        property.isAccessible = true
        val name = property.name
        val value = property.get(defaultInstance)

        if (value != null) {
            val query = Query(Criteria.where(name).exists(false))
            val update = Update().set(name, value)
            val result = mongoTemplate.updateMulti(query, update, T::class.java, collectionName)
            totalPatched += result.modifiedCount
        }
    }

    log.info("Patched $totalPatched field(s) across documents in '$collectionName'")
}

inline fun <reified T : Any> createDefaultInstance(): T? {
    return try {
        val constructor = T::class.primaryConstructor ?: return null
        // Required (non-optional) parameters have no default to reflect on -
        // give them a placeholder just so the constructor call succeeds.
        // patchMongoCollection() filters these back out before using any values.
        val args = constructor.parameters
            .filterNot { it.isOptional }
            .associateWith { placeholderFor(it) }
        constructor.callBy(args)
    } catch (e: Exception) {
        log.warn("Could not create instance of ${T::class.simpleName}: ${e.message}")
        null
    }
}

fun placeholderFor(param: KParameter): Any? = when {
    param.type.isMarkedNullable -> null
    param.type.classifier == String::class -> ""
    param.type.classifier == List::class -> emptyList<Any?>()
    param.type.classifier == Int::class -> 0
    param.type.classifier == Boolean::class -> false
    else -> null
}

