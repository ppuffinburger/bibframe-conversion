package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object VideoRecordingCarrierLookup {
    private val MAP: Map<String, String> = mapOf(
        "http://id.loc.gov/vocabulary/carriers/vc" to "vc ",
        "http://id.loc.gov/vocabulary/carriers/vd" to "vd ",
        "http://id.loc.gov/vocabulary/carriers/vf" to "vf ",
        "http://id.loc.gov/vocabulary/carriers/vr" to "vr "
    )

    fun lookup(value: Value): String? {
        return MAP[value.stringValue()]
    }
}