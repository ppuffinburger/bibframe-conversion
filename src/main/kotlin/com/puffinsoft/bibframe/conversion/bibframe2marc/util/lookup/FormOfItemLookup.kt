package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object FormOfItemLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/vocabulary/carriers/hd" to 'a',
        "http://id.loc.gov/vocabulary/carriers/he" to 'b',
        "http://id.loc.gov/vocabulary/carriers/hg" to 'c',
        "http://id.loc.gov/vocabulary/carriers/cr" to 'o',
        "http://id.loc.gov/vocabulary/carriers/cz" to 's'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}