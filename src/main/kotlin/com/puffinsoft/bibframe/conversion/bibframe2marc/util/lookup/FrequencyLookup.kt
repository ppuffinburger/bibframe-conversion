package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object FrequencyLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/vocabulary/frequencies/ann" to 'a',
        "http://id.loc.gov/vocabulary/frequencies/bmn" to 'b',
        "http://id.loc.gov/vocabulary/frequencies/swk" to 'c',
        "http://id.loc.gov/vocabulary/frequencies/dyl" to 'd',
        "http://id.loc.gov/vocabulary/frequencies/bwk" to 'e',
        "http://id.loc.gov/vocabulary/frequencies/san" to 'f',
        "http://id.loc.gov/vocabulary/frequencies/bin" to 'g',
        "http://id.loc.gov/vocabulary/frequencies/ten" to 'h',
        "http://id.loc.gov/vocabulary/frequencies/ttw" to 'i',
        "http://id.loc.gov/vocabulary/frequencies/ttm" to 'j',
        "http://id.loc.gov/vocabulary/frequencies/con" to 'k',
        "http://id.loc.gov/vocabulary/frequencies/mon" to 'm',
        "http://id.loc.gov/vocabulary/frequencies/qrt" to 'q',
        "http://id.loc.gov/vocabulary/frequencies/smn" to 's',
        "http://id.loc.gov/vocabulary/frequencies/tty" to 't'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}