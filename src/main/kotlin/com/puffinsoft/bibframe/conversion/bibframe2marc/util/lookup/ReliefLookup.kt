package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object ReliefLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/vocabulary/mrelief/cont" to 'a',
        "http://id.loc.gov/vocabulary/mrelief/shad" to 'b',
        "http://id.loc.gov/vocabulary/mrelief/grad" to 'c',
        "http://id.loc.gov/vocabulary/mrelief/hach" to 'd',
        "http://id.loc.gov/vocabulary/mrelief/bath" to 'e',
        "http://id.loc.gov/vocabulary/mrelief/form" to 'f',
        "http://id.loc.gov/vocabulary/mrelief/spot" to 'g',
        "http://id.loc.gov/vocabulary/mrelief/pict" to 'i',
        "http://id.loc.gov/vocabulary/mrelief/land" to 'j',
        "http://id.loc.gov/vocabulary/mrelief/isol" to 'k',
        "http://id.loc.gov/vocabulary/mrelief/rock" to 'm'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}