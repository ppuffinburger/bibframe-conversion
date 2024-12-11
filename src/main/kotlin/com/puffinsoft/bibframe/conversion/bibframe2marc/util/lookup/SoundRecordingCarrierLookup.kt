package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object SoundRecordingCarrierLookup {
    private val MAP: Map<String, String> = mapOf(
        "http://id.loc.gov/vocabulary/carriers/sd" to "sd ",
        "http://id.loc.gov/vocabulary/carriers/se" to "se ",
        "http://id.loc.gov/vocabulary/carriers/sg" to "sg ",
        "http://id.loc.gov/vocabulary/carriers/si" to "si ",
        "http://id.loc.gov/vocabulary/carriers/sq" to "sq ",
        "http://id.loc.gov/vocabulary/carriers/cr" to "sr ",
        "http://id.loc.gov/vocabulary/carriers/ss" to "ss ",
        "http://id.loc.gov/vocabulary/carriers/st" to "st ",
        "http://id.loc.gov/vocabulary/carriers/sw" to "sw "
    )

    fun lookup(value: Value): String? {
        return MAP[value.stringValue()]
    }
}