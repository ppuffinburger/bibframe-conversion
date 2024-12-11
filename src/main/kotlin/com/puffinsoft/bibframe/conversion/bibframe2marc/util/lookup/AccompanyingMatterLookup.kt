package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object AccompanyingMatterLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/vocabularly/msupplecont/discography" to 'a',
        "http://id.loc.gov/vocabularly/msupplecont/bibliography" to 'b',
        "http://id.loc.gov/vocabularly/msupplecont/thematicindex" to 'c',
        "http://id.loc.gov/vocabularly/msupplecont/libretto" to 'd',
        "http://id.loc.gov/vocabularly/msupplecont/creatorbio" to 'e',
        "http://id.loc.gov/vocabularly/msupplecont/performerhistory" to 'f',
        "http://id.loc.gov/vocabularly/msupplecont/techinstruments" to 'g',
        "http://id.loc.gov/vocabularly/msupplecont/techmusic" to 'h',
        "http://id.loc.gov/vocabularly/msupplecont/historicalinfo" to 'i',
        "http://id.loc.gov/vocabularly/msupplecont/ethnologicinfo" to 'k',
        "http://id.loc.gov/vocabularly/msupplecont/instructmaterial" to 'r',
        "http://id.loc.gov/vocabularly/msupplecont/music" to 's'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}