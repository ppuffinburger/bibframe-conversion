package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.base.AbstractLiteral

internal object LanguageScriptLookup {
    private val MAP: Map<String, String> = mapOf(
        "arab" to "(3",
        "cyrl" to "(N",
        "grek" to "(S",
        "hang" to "$1",
        "hani" to "$1",
        "hebr" to "(2",
        "jpan" to "$1",
        "latn" to "(B"
    )

    fun lookup(value: Value): String? {
        val lookup = (value as AbstractLiteral).language.map { it.substringAfter("-").lowercase() }.orElse("")
        return MAP[lookup]
    }
}