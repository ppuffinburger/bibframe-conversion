package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.eclipse.rdf4j.model.Value

internal object NonProjectedGraphicGenreFormLookup {
    private val MAP: Map<String, String> = mapOf(
        "http://id.loc.gov/authorities/genreForms/gf2017027227" to "kc ",
        "http://id.loc.gov/authorities/genreForms/gf2017027231" to "kd ",
        "http://id.loc.gov/authorities/genreForms/gf2017027246" to "ke ",
        "http://id.loc.gov/vocabulary/graphicMaterials/tgm007730" to "kf ",
        "http://id.loc.gov/authorities/genreForms/gf2019026026" to "kg ",
        "http://id.loc.gov/vocabulary/graphicMaterials/tgm007718" to "kh ",
        "http://id.loc.gov/authorities/genreForms/gf2017027251" to "ki ",
        "http://id.loc.gov/authorities/genreForms/gf2017027255" to "kj ",
        "http://id.loc.gov/authorities/genreForms/gf2014026152" to "kk ",
        "http://id.loc.gov/vocabulary/graphicMaterials/tgm009250" to "kl ",
        "http://id.loc.gov/authorities/genreForms/gf2016026011" to "kn ",
        "http://id.loc.gov/authorities/genreForms/gf2014026151" to "kp ",
        "http://id.loc.gov/authorities/genreForms/gf2017027249" to "kv "
    )

    fun lookup(value: Value): String? {
        return MAP[value.stringValue()]
    }
}