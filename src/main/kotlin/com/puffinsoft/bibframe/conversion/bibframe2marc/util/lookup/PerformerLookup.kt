package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException

internal object PerformerLookup {
    private val PERFORMER_KEY_TO_CODE: Map<PerformerKey, String>

    init {
        val mapper = ObjectMapper()
        try {
            PERFORMER_KEY_TO_CODE = mapper.readValue(javaClass.getResourceAsStream("/performers.json"), object : TypeReference<List<Performer>>() {}).associateBy({ PerformerKey(it.type, it.label ?: "") }, { it.code }).toMap()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun lookup(type: String, label: String): String? {
        return PERFORMER_KEY_TO_CODE[PerformerKey(type, label)]
    }

    @JvmRecord
    private data class PerformerKey(val type: String, val label: String) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PerformerKey

            if (type != other.type) return false
            if (label != other.label) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + label.hashCode()
            return result
        }
    }

    @JvmRecord
    private data class Performer(val code: String, val type: String, val label: String?)
}