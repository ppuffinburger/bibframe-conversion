package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object TypeOfVisualMaterialLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/authorities/genreForms/gf2017027218" to 'a',
        "http://id.loc.gov/vocabulary/marcgt/kit" to 'b',
        "http://id.loc.gov/vocabulary/marcgt/arr" to 'c',
        "http://id.loc.gov/vocabulary/marcgt/dio" to 'd',
        "http://id.loc.gov/vocabulary/marcgt/fls" to 'f',
        "http://id.loc.gov/authorities/genreForms/gf2014026158" to 'g',
        "http://id.loc.gov/authorities/genreForms/gf2017027251" to 'i',
        "http://id.loc.gov/vocabulary/marcgt/gra" to 'k',
        "http://id.loc.gov/vocabulary/marcgt/ted" to 'l',
        "http://id.loc.gov/authorities/genreForms/gf2011026406" to 'm',
        "http://id.loc.gov/vocabulary/marcgt/cha" to 'n',
        "http://id.loc.gov/vocabulary/marcgt/fla" to 'o',
        "http://id.loc.gov/vocabulary/marcgt/mic" to 'p',
        "http://id.loc.gov/authorities/genreForms/gf2017027245" to 'q',
        "http://id.loc.gov/vocabulary/marcgt/rea" to 'r',
        "http://id.loc.gov/vocabulary/marcgt/sli" to 's',
        "http://id.loc.gov/vocabulary/marcgt/tra" to 't',
        "http://id.loc.gov/authorities/genreForms/gf2011026723" to 'v',
        "http://id.loc.gov/vocabulary/marcgt/toy" to 'w'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}