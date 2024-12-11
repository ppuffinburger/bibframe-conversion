package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object GovernmentPublicationLookup {
    private val MAP: Map<String, Char> = mapOf(
        "http://id.loc.gov/vocabulary/mgovtpubtype/a" to 'a',
        "http://id.loc.gov/vocabulary/mgovtpubtype/c" to 'c',
        "http://id.loc.gov/vocabulary/mgovtpubtype/f" to 'f',
        "http://id.loc.gov/vocabulary/mgovtpubtype/i" to 'i',
        "http://id.loc.gov/vocabulary/mgovtpubtype/l" to 'l',
        "http://id.loc.gov/vocabulary/mgovtpubtype/m" to 'm',
        "http://id.loc.gov/vocabulary/mgovtpubtype/g" to 'o',
        "http://id.loc.gov/vocabulary/mgovtpubtype/s" to 's'
    )

    fun lookup(value: Value): Char? {
        return MAP[value.stringValue()]
    }
}