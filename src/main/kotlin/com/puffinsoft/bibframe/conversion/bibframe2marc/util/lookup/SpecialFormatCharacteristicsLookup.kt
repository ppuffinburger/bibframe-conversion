package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object SpecialFormatCharacteristicsLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/authorities/genreForms/gf2011026385" to 'e',
        "http://id.loc.gov/authorities/genreForms/gf2014026151" to 'j',
        "http://id.loc.gov/authorities/genreForms/gf2014026055" to 'k',
        "http://id.loc.gov/authorities/genreForms/gf2014026158" to 'l',
        "http://id.loc.gov/authorities/genreForms/gf2011026728" to 'o',
        "http://id.loc.gov/authorities/genreForms/gf2017027252" to 'p',
        "http://id.loc.gov/authorities/genreForms/gf2011026373" to 'r'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}