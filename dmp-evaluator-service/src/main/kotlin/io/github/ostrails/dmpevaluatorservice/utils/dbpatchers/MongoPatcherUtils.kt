package io.github.ostrails.dmpevaluatorservice.utils.dbpatchers
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

inline fun <reified T : Any> patchMongoCollection(
    mongoTemplate: MongoTemplate,
    collectionName: String
) {
    val defaultInstance = createDefaultInstance<T>()
    if (defaultInstance == null) {
        println("❌ Could not create default instance for ${T::class.simpleName}")
        return
    }

    println("✅ Default instance created: $defaultInstance")
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
    println("🔧 Patched ${result.modifiedCount} documents in '$collectionName'")
}

inline fun <reified T : Any> createDefaultInstance(): T? {
    return try {
        val constructor = T::class.constructors.firstOrNull {
            it.parameters.all { p -> p.isOptional || p.type.isMarkedNullable }
        }
        constructor?.callBy(emptyMap())
    } catch (e: Exception) {
        println("⚠️ Could not create instance of ${T::class.simpleName}: ${e.message}")
        null
    }
}

