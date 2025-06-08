// app/src/main/java/com/virtualrealm/virtualrealmmusicplayer/util/RuntimeTypeAdapterFactory.kt
package com.virtualrealm.virtualrealmmusicplayer.util

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

/**
 * Adapts types based on a runtime-specified type field in the JSON object.
 */
class RuntimeTypeAdapterFactory<T> private constructor(
    private val baseType: Class<*>,
    private val typeFieldName: String,
    private val maintainType: Boolean
) : TypeAdapterFactory {
    private val labelToSubtype = LinkedHashMap<String, Class<*>>()
    private val subtypeToLabel = LinkedHashMap<Class<*>, String>()

    companion object {
        fun <T> of(baseType: Class<T>, typeFieldName: String, maintainType: Boolean): RuntimeTypeAdapterFactory<T> {
            return RuntimeTypeAdapterFactory(baseType, typeFieldName, maintainType)
        }

        fun <T> of(baseType: Class<T>, typeFieldName: String): RuntimeTypeAdapterFactory<T> {
            return RuntimeTypeAdapterFactory(baseType, typeFieldName, false)
        }

        fun <T> of(baseType: Class<T>): RuntimeTypeAdapterFactory<T> {
            return RuntimeTypeAdapterFactory(baseType, "type", false)
        }
    }

    fun registerSubtype(subtype: Class<out T>, label: String): RuntimeTypeAdapterFactory<T> {
        if (subtypeToLabel.containsKey(subtype) || labelToSubtype.containsKey(label)) {
            throw IllegalArgumentException("types and labels must be unique")
        }
        labelToSubtype[label] = subtype
        subtypeToLabel[subtype] = label
        return this
    }

    override fun <R : Any> create(gson: Gson, type: TypeToken<R>): TypeAdapter<R>? {
        if (type.rawType != baseType) {
            return null
        }

        val labelToDelegate = LinkedHashMap<String, TypeAdapter<*>>()
        val subtypeToDelegate = LinkedHashMap<Class<*>, TypeAdapter<*>>()
        for ((key, value) in labelToSubtype.entries) {
            val delegate = gson.getDelegateAdapter(this, TypeToken.get(value))
            labelToDelegate[key] = delegate
            subtypeToDelegate[value] = delegate
        }

        return object : TypeAdapter<R>() {
            override fun read(reader: JsonReader): R {
                val jsonElement = gson.getAdapter(JsonElement::class.java).read(reader)
                val labelJsonElement = jsonElement.asJsonObject.remove(typeFieldName)
                    ?: throw JsonParseException("cannot deserialize ${baseType} because it does not define a field named ${typeFieldName}")
                val label = labelJsonElement.asString
                val delegate = labelToDelegate[label] as TypeAdapter<R>?
                    ?: throw JsonParseException("cannot deserialize ${baseType} subtype named ${label}; did you forget to register a subtype?")
                return delegate.fromJsonTree(jsonElement)
            }

            override fun write(writer: JsonWriter, value: R) {
                val srcType = value::class.java
                val label = subtypeToLabel[srcType]
                val delegate = subtypeToDelegate[srcType] as TypeAdapter<R>?
                    ?: throw JsonParseException("cannot serialize ${srcType.name}; did you forget to register a subtype?")
                val jsonObject = delegate.toJsonTree(value).asJsonObject

                if (maintainType) {
                    val `object` = JsonObject()
                    for ((key, value1) in jsonObject.entrySet()) {
                        `object`.add(key, value1)
                    }
                    `object`.add(typeFieldName, JsonPrimitive(label))
                    gson.getAdapter(JsonObject::class.java).write(writer, `object`)
                } else {
                    jsonObject.add(typeFieldName, JsonPrimitive(label!!))
                    gson.getAdapter(JsonObject::class.java).write(writer, jsonObject)
                }
            }
        }.nullSafe()
    }
}