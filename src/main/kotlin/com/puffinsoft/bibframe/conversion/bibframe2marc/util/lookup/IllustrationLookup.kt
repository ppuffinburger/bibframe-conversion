package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object IllustrationLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/vocabularly/millus/ill" to 'a',
        "http://id.loc.gov/vocabularly/millus/map" to 'b',
        "http://id.loc.gov/vocabularly/millus/por" to 'c',
        "http://id.loc.gov/vocabularly/millus/chr" to 'd',
        "http://id.loc.gov/vocabularly/millus/pln" to 'e',
        "http://id.loc.gov/vocabularly/millus/plt" to 'f',
        "http://id.loc.gov/vocabularly/millus/mus" to 'g',
        "http://id.loc.gov/vocabularly/millus/fac" to 'h',
        "http://id.loc.gov/vocabularly/millus/coa" to 'i',
        "http://id.loc.gov/vocabularly/millus/gnt" to 'j',
        "http://id.loc.gov/vocabularly/millus/for" to 'k',
        "http://id.loc.gov/vocabularly/millus/sam" to 'l',
        "http://id.loc.gov/vocabularly/millus/pho" to 'm',
        "http://id.loc.gov/vocabularly/millus/pht" to 'o',
        "http://id.loc.gov/vocabularly/millus/ilm" to 'p'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}