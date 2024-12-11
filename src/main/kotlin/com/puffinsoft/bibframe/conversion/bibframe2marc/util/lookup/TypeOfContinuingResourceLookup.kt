package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object TypeOfContinuingResourceLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/vocabulary/mserialpubtype/database" to 'd',
        "http://id.loc.gov/vocabulary/mserialpubtype/mag" to 'g',
        "http://id.loc.gov/vocabulary/mserialpubtype/blog" to 'h',
        "http://id.loc.gov/vocabulary/mserialpubtype/journal" to 'j',
        "http://id.loc.gov/vocabulary/mserialpubtype/looseleaf" to 'l',
        "http://id.loc.gov/vocabulary/mserialpubtype/monoseries" to 'm',
        "http://id.loc.gov/vocabulary/mserialpubtype/newspaper" to 'n',
        "http://id.loc.gov/vocabulary/mserialpubtype/periodical" to 'p',
        "http://id.loc.gov/vocabulary/mserialpubtype/repo" to 'r',
        "http://id.loc.gov/vocabulary/mserialpubtype/newsletter" to 's',
        "http://id.loc.gov/vocabulary/mserialpubtype/directory" to 't',
        "http://id.loc.gov/vocabulary/mserialpubtype/web" to 'w'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}