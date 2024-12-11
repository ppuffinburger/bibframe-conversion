package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object MicroformCarrierLookup {
    private val MAP: Map<String, String> = mapOf(
        "http://id.loc.gov/vocabulary/carriers/ha" to "ha ",
        "http://id.loc.gov/vocabulary/carriers/hb" to "hb ",
        "http://id.loc.gov/vocabulary/carriers/hc" to "hc ",
        "http://id.loc.gov/vocabulary/carriers/hd" to "hd ",
        "http://id.loc.gov/vocabulary/carriers/he" to "he ",
        "http://id.loc.gov/vocabulary/carriers/hf" to "hf ",
        "http://id.loc.gov/vocabulary/carriers/hg" to "hg ",
        "http://id.loc.gov/vocabulary/carriers/hh" to "hh ",
        "http://id.loc.gov/vocabulary/carriers/hj" to "hj "
    )

    fun lookup(value: Value): String? {
        return MAP[value.stringValue()]
    }
}