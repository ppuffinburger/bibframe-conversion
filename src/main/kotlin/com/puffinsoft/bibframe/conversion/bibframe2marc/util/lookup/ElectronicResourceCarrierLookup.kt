package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object ElectronicResourceCarrierLookup {
    private val MAP: Map<String, String> = mapOf(
        "http://id.loc.gov/vocabulary/carriers/ca" to "ca ",
        "http://id.loc.gov/vocabulary/carriers/cb" to "cb ",
        "http://id.loc.gov/vocabulary/carriers/cd" to "co ",
        "http://id.loc.gov/vocabulary/carriers/ce" to "cj ",
        "http://id.loc.gov/vocabulary/carriers/cf" to "cf ",
        "http://id.loc.gov/vocabulary/carriers/ch" to "ch ",
        "http://id.loc.gov/vocabulary/carriers/ck" to "ck ",
        "http://id.loc.gov/vocabulary/carriers/cr" to "cr ",
        "http://id.loc.gov/vocabulary/carriers/cz" to "cz "
    )

    fun lookup(value: Value): String? {
        return MAP[value.stringValue()]
    }
}