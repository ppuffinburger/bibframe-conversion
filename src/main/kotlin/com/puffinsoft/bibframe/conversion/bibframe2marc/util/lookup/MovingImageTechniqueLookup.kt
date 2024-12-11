package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object MovingImageTechniqueLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/vocabulary/mtechnique/anim" to 'a',
        "http://id.loc.gov/vocabulary/mtechnique/animlive" to 'c',
        "http://id.loc.gov/vocabulary/mtechnique/live" to 'l',
        "http://id.loc.gov/vocabulary/mtechnique/other" to 'z'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}