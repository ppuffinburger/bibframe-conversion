package com.puffinsoft.bibframe.conversion.bibframe2marc.converter

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control.BibframeToField001Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control.BibframeToField005Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control.BibframeToField007Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control.BibframeToField008Converter
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToControlFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        BibframeToField001Converter(),
        BibframeToField005Converter(),
        BibframeToField007Converter(),
        BibframeToField008Converter()
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}