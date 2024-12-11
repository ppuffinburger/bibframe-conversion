package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object MapGenreFormLookup {
    private val MAP: Map<String, String> = mapOf(
        "http://id.loc.gov/authorities/genreForms/gf2011026058" to "ad ",
        "http://id.loc.gov/authorities/genreForms/gf2014026061" to "ag ",
        "http://id.loc.gov/authorities/genreForms/gf2011026387" to "aj ",
        "http://id.loc.gov/authorities/genreForms/gf2011026113" to "ak ",
        "http://id.loc.gov/authorities/genreForms/gf2017027245" to "aq ",
        "http://id.loc.gov/authorities/genreForms/gf2011026530" to "ar ",
        "http://id.loc.gov/authorities/genreForms/gf2011026295" to "as ",
        "http://id.loc.gov/authorities/genreForms/gf2018026045" to "ay "
    )

    fun lookup(value: Value): String? {
        return MAP[value.stringValue()]
    }
}