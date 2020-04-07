package vip.untitled.bungeeguard.helpers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser

@Suppress("MemberVisibilityCanBePrivate")
open class TypedJSON(val json: JsonElement) {
    companion object {
        @Suppress("DEPRECATION")
        fun fromString(str: String): TypedJSON {
            return try {
                TypedJSON(JsonParser.parseString(str))
            } catch (err: NoSuchMethodError) {
                TypedJSON(JsonParser().parse(str))
            }
        }
    }

    fun assertPrimitive() {
        assertPrimitive() { "JSON primitive expected" }
    }

    fun assertPrimitive(lazyMessage: () -> Any) {
        assert(json.isJsonPrimitive, lazyMessage)
    }

    fun assertString() {
        assertString() { "String expected" }
    }

    fun assertString(lazyMessage: () -> Any) {
        assert(json.isJsonPrimitive && json.asJsonPrimitive.isString, lazyMessage)
    }

    fun assertNumber() {
        assertNumber() { "Number expected" }
    }

    fun assertNumber(lazyMessage: () -> Any) {
        assert(json.isJsonPrimitive && json.asJsonPrimitive.isNumber, lazyMessage)
    }

    fun assertBoolean() {
        assertBoolean() { "Boolean expected" }
    }

    fun assertBoolean(lazyMessage: () -> Any) {
        assert(json.isJsonPrimitive && json.asJsonPrimitive.isBoolean, lazyMessage)
    }

    fun assertNull() {
        assertNull() { "Null expected" }
    }

    fun assertNull(lazyMessage: () -> Any) {
        assert(json.isJsonNull, lazyMessage)
    }

    fun assertObject() {
        assertObject() { "JSON object expected" }
    }

    fun assertObject(lazyMessage: () -> Any) {
        assert(json.isJsonObject, lazyMessage)
    }

    fun assertArray() {
        assertArray() { "Array expected" }
    }

    fun assertArray(lazyMessage: () -> Any) {
        assert(json.isJsonArray, lazyMessage)
    }

    @Suppress("DuplicatedCode")
    fun getString(key: String): String? {
        if (!json.isJsonObject) return null
        val elem = json.asJsonObject.get(key) ?: return null
        if (elem.isJsonPrimitive) {
            val primitive = elem.asJsonPrimitive
            if (primitive.isString) {
                return primitive.asString
            } else {
                throw IllegalStateException("Invalid property type")
            }
        } else throw IllegalStateException("Invalid property type")
    }

    fun getArray(key: String): JsonArray? {
        if (!json.isJsonObject) return null
        val elem = json.asJsonObject.get(key) ?: return null
        return if (elem.isJsonArray) elem.asJsonArray
        else null
    }

    @Suppress("DuplicatedCode")
    fun getLong(key: String): Long? {
        if (!json.isJsonObject) return null
        val elem = json.asJsonObject.get(key) ?: return null
        if (elem.isJsonPrimitive) {
            val primitive = elem.asJsonPrimitive
            if (primitive.isNumber) {
                return primitive.asLong
            } else {
                throw IllegalStateException("Invalid property type")
            }
        } else throw IllegalStateException("Invalid property type")
    }
}