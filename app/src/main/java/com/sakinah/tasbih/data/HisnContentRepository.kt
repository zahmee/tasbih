package com.sakinah.tasbih.data

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import java.io.InputStreamReader

class HisnContentRepository(private val context: Context) {
    fun load(languageTag: String = "ar"): HisnCatalog {
        val language = languageTag.substringBefore('-').ifBlank { "ar" }
        val requestedAsset = "hisn_${language}.json"
        var loadedLanguage = language
        val input = runCatching { context.assets.open(requestedAsset) }
            .getOrElse {
                loadedLanguage = "ar"
                context.assets.open(ArabicAsset)
            }

        return input.use { stream ->
            JsonReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                parseCatalog(reader, languageTag = loadedLanguage)
            }
        }
    }

    private fun parseCatalog(reader: JsonReader, languageTag: String): HisnCatalog {
        val collections = mutableListOf<DhikrCollection>()
        reader.beginObject()
        var order = 0

        while (reader.hasNext()) {
            order += 1
            val title = reader.nextName()
            val collectionId = "hisn_${order.toString().padStart(3, '0')}"
            var audioUrl: String? = null
            val entries = mutableListOf<DhikrEntry>()

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "Audio" -> audioUrl = reader.nextNullableString()
                    "Adhkar" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            var text = ""
                            var repetitions = 1
                            var reference = ""
                            reader.beginObject()
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "Text" -> text = reader.nextNullableString().orEmpty()
                                    "Count" -> repetitions = reader.nextInt().coerceAtLeast(1)
                                    "Reference" -> reference = reader.nextNullableString().orEmpty()
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()

                            entries += DhikrEntry(
                                id = "${collectionId}_${(entries.size + 1).toString().padStart(2, '0')}",
                                collectionId = collectionId,
                                text = text.trim(),
                                repetitions = repetitions,
                                reference = reference.trim(),
                            )
                        }
                        reader.endArray()
                    }

                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            collections += DhikrCollection(
                id = collectionId,
                order = order,
                title = title.trim(),
                audioUrl = audioUrl,
                entries = entries,
            )
        }
        reader.endObject()

        return HisnCatalog(languageTag = languageTag, collections = collections)
    }

    private fun JsonReader.nextNullableString(): String? = if (peek() == JsonToken.NULL) {
        nextNull()
        null
    } else {
        nextString()
    }

    private companion object {
        const val ArabicAsset = "hisn_ar.json"
    }
}
