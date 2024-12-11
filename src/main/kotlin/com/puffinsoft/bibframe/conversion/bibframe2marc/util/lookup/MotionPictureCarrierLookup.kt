package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object MotionPictureCarrierLookup {
    private val MAP: Map<String, String> = mapOf(
        getUri("mc") to "mc ",
        getUri("mf") to "mf ",
        getUri("mr") to "mr ",
        getUri("mz") to "mz "
    )

    fun lookup(value: Value): String? {
        return MAP[value.stringValue()]
    }

    private fun getUri(code: String): String {
        return CarriersLookup.lookup(code)?.uri ?: throw RuntimeException("Could not find '$code' in carriers")
    }
}