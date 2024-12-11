package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object TypeOfComputerFileLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/vocabulary/marcgt/num" to 'a',
        "http://id.loc.gov/vocabulary/marcgt/com" to 'b',
        "http://id.loc.gov/vocabulary/marcgt/rep" to 'c',
        "http://id.loc.gov/vocabulary/marcgt/doc" to 'd',
        "http://id.loc.gov/vocabulary/marcgt/bda" to 'e',
        "http://id.loc.gov/vocabulary/marcgt/fon" to 'f',
        "http://id.loc.gov/vocabulary/marcgt/gam" to 'g',
        "http://id.loc.gov/vocabulary/marcgt/sou" to 'h',
        "http://id.loc.gov/vocabulary/marcgt/inm" to 'i',
        "http://id.loc.gov/vocabulary/marcgt/ons" to 'j'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}