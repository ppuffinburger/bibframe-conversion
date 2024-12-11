package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object TargetAudienceLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/vocabulary/maudience/pre" to 'a',
        "http://id.loc.gov/vocabulary/maudience/pri" to 'b',
        "http://id.loc.gov/vocabulary/maudience/pad" to 'c',
        "http://id.loc.gov/vocabulary/maudience/ado" to 'd',
        "http://id.loc.gov/vocabulary/maudience/adu" to 'e',
        "http://id.loc.gov/vocabulary/maudience/spe" to 'f',
        "http://id.loc.gov/vocabulary/maudience/gen" to 'g',
        "http://id.loc.gov/vocabulary/maudience/juv" to 'j'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}