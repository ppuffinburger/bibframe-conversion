package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object FormatOfMusicLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/vocabulary/mmusicformat/score" to 'a',
        "http://id.loc.gov/vocabulary/mmusicformat/studyscore" to 'b',
        "http://id.loc.gov/vocabulary/mmusicformat/pianoscore" to 'c',
        //"http://id.loc.gov/vocabulary/mmusicformat/vocalscore" to 'd', TODO : The rules doubled this, but the LC conversion also ignores 'd'.
        "http://id.loc.gov/vocabulary/mmusicformat/pianopart" to 'e',
        //"http://id.loc.gov/vocabulary/mmusicformat/conscore" to 'g', TODO : The rules doubled this, but the LC conversion also ignores 'g'.
        "http://id.loc.gov/vocabulary/mmusicformat/chscore" to 'h',
        "http://id.loc.gov/vocabulary/mmusicformat/conscore" to 'i',
        "http://id.loc.gov/vocabulary/mmusicformat/perfconpt" to 'j',
        "http://id.loc.gov/vocabulary/mmusicformat/vocalscore" to 'k'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}