package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._1xx.BibframeToField100Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._1xx.BibframeToField110Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._1xx.BibframeToField111Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._1xx.BibframeToField130Converter
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToMainEntryFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        BibframeToField100Converter(),
        BibframeToField110Converter(),
        BibframeToField111Converter(),
        BibframeToField130Converter()
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}