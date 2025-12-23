package com.example.stalcraft_companion_compose.api

import com.example.stalcraft_companion_compose.data.models.FormattedObject
import com.example.stalcraft_companion_compose.data.models.InfoBlock
import com.example.stalcraft_companion_compose.data.models.TranslationLines
import com.example.stalcraft_companion_compose.data.models.TranslationString
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class InfoBlockDeserializer : JsonDeserializer<InfoBlock> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): InfoBlock? {
        val jsonObject = json.asJsonObject
        return when (jsonObject.get("type")?.asString) {
            "text" -> InfoBlock.TextBlock(
                title = context.deserialize(jsonObject.get("title"), TranslationString::class.java),
                text = context.deserialize(jsonObject.get("text"), TranslationString::class.java)
            )
            "damage" -> InfoBlock.DamageBlock(
                startDamage = jsonObject.get("startDamage").asFloat,
                damageDecreaseStart = jsonObject.get("damageDecreaseStart").asFloat,
                endDamage = jsonObject.get("endDamage").asFloat,
                damageDecreaseEnd = jsonObject.get("damageDecreaseEnd").asFloat,
                maxDistance = jsonObject.get("maxDistance").asFloat
            )
            "range" -> InfoBlock.RangeBlock(
                name = context.deserialize(jsonObject.get("name"), TranslationString::class.java),
                min = jsonObject.get("min").asFloat,
                max = jsonObject.get("max").asFloat
            )
            "list" -> InfoBlock.ListBlock(
                title = context.deserialize(jsonObject.get("title"), TranslationString::class.java),
                elements = context.deserialize(jsonObject.get("elements"), object : TypeToken<List<InfoBlock>>() {}.type)
            )
            "numeric" -> InfoBlock.NumericBlock(
                name = context.deserialize(jsonObject.get("name"), TranslationString::class.java),
                value = jsonObject.get("value").asFloat,
                formatted = context.deserialize(jsonObject.get("formatted"), FormattedObject::class.java)
            )
            "key-value" -> InfoBlock.KeyValueBlock(
                key = context.deserialize(jsonObject.get("key"), TranslationString::class.java),
                value = context.deserialize(jsonObject.get("value"), TranslationString::class.java)
            )
            "usage" -> InfoBlock.UsageBlock(
                name = context.deserialize(jsonObject.get("name"), TranslationString::class.java)
            )
            "item" -> InfoBlock.ItemBlock(
                name = context.deserialize(jsonObject.get("name"), TranslationString::class.java)
            )
            null -> null
            else -> throw JsonParseException("Unknown type: ${jsonObject.get("type")} on JsonObject (InfoBlock): $jsonObject")
        }
    }
}

class TranslationStringDeserializer: JsonDeserializer<TranslationString> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): TranslationString? {
        val jsonObject = json.asJsonObject
        return when (jsonObject.get("type")?.asString) {
            "text" -> TranslationString.Text(
                text = jsonObject.get("text").asString
            )
            "translation" -> TranslationString.Translation(
                lines = context.deserialize(jsonObject.get("lines"), TranslationLines::class.java)
            )
            null -> null
            else -> throw JsonParseException("Unknown type: ${jsonObject.get("type")} on JsonObject (TranslationString): $jsonObject")
        }
    }
}