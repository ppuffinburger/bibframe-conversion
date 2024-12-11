package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx.*
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToEditionImprintEtcFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        BibframeToField250Converter(),
        // 251 Not supported in Bibframe
        // 254 Not supported in Bibframe
        BibframeToField255Converter(),
        BibframeToField256Converter(),
        BibframeToField257Converter(),
        // 258 Not supported in Bibframe
        // 260 Not supported in Bibframe
        BibframeToField263Converter(),
        BibframeToField264Converter()
        // 270 Not supported in Bibframe
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}