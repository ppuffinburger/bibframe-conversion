package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object ProjectedGraphicCarrierLookup {
    private val MAP: Map<String, String> = mapOf(
        "http://id.loc.gov/vocabulary/carriers/gc" to "gc ",
        "http://id.loc.gov/vocabulary/carriers/gd" to "gd ",
        "http://id.loc.gov/vocabulary/carriers/gf" to "gf ",
        "http://id.loc.gov/vocabulary/carriers/mo" to "go ",
        "http://id.loc.gov/vocabulary/carriers/gs" to "gs ",
        "http://id.loc.gov/vocabulary/carriers/gt" to "gt "
    )

    fun lookup(value: Value): String? {
        return MAP[value.stringValue()]
    }
}