package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object LiteraryTextForSoundRecordingsLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/authorities/genreForms/gf2014026047" to 'a',
        "http://id.loc.gov/authorities/genreForms/gf2014026049" to 'b',
        "http://id.loc.gov/authorities/genreForms/gf2014026068" to 'c',
        "http://id.loc.gov/authorities/genreForms/gf2014026297" to 'd',
        "http://id.loc.gov/authorities/genreForms/gf2014026094" to 'e',
        "http://id.loc.gov/authorities/genreForms/gf2014026339" to 'f',
        "http://id.loc.gov/authorities/genreForms/gf2014026113" to 'g',
        "http://id.loc.gov/vocabulary/marcgt/his" to 'h',
        "http://id.loc.gov/authorities/genreForms/gf2014026114" to 'i',
        "http://id.loc.gov/vocabulary/marcgt/lan" to 'j',
        "http://id.loc.gov/authorities/genreForms/gf2014026110" to 'k',
        "http://id.loc.gov/authorities/genreForms/gf2011026363" to 'l',
        //"http://id.loc.gov/authorities/genreForms/gf2014026047" to 'm', TODO : this is duplicated.  LC conversion appears broken in this and doesn't display 'a' or 'm'.  Since this gf is Autobiography I'll use 'a' for now.
        "http://id.loc.gov/authorities/genreForms/gf2014026344" to 'o',
        "http://id.loc.gov/authorities/genreForms/gf2014026481" to 'p',
        "http://id.loc.gov/vocabulary/marcgt/reh" to 'r',
        "http://id.loc.gov/authorities/genreForms/gf2011026594" to 's',
        "http://id.loc.gov/authorities/genreForms/gf2014026115" to 't'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}