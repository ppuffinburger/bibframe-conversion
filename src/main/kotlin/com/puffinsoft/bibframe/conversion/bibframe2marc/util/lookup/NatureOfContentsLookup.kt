package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object NatureOfContentsLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/vocabulary/msupplcont/bibliography" to 'b',
        "http://id.loc.gov/vocabulary/msupplcont/discography" to 'k',
        "http://id.loc.gov/vocabulary/msupplcont/film" to 'q',
        "http://id.loc.gov/authorities/genreForms/gf2014026057" to 'c',
        "http://id.loc.gov/authorities/genreForms/gf2014026086" to 'd',
        "http://id.loc.gov/authorities/genreForms/gf2014026092" to 'e',
        "http://id.loc.gov/authorities/genreForms/gf2014026109" to 'f',
//        "http://id.loc.gov/authorities/genreForms/gf2011026351" to 'g',   Code has this commented out
//        "http://id.loc.gov/authorities/genreForms/gf2014026049" to 'h',   Code has this commented out
        "http://id.loc.gov/authorities/genreForms/gf2014026112" to 'i',
        "http://id.loc.gov/authorities/genreForms/gf2011026438" to 'j',
        "http://id.loc.gov/authorities/genreForms/gf2011026351" to 'l',
        "http://id.loc.gov/authorities/genreForms/gf2014026039" to 'm',
        "http://id.loc.gov/authorities/genreForms/gf2014026087" to 'r',
        "http://id.loc.gov/authorities/genreForms/gf2014026181" to 's',
        "http://id.loc.gov/authorities/genreForms/gf2015026093" to 't',
        "http://id.loc.gov/authorities/genreForms/gf2014026208" to 'y',
        "http://id.loc.gov/authorities/genreForms/gf2011026707" to 'z',
        "http://id.loc.gov/authorities/genreForms/gf2014026055" to '5',
        "http://id.loc.gov/authorities/genreForms/gf2014026266" to '6'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}