package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._4xx.BibframeToField490Converter
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToSeriesStatementFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        BibframeToField490Converter()
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}