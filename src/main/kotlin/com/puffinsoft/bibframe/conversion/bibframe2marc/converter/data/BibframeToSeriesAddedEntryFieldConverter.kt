package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._8xx.BibframeToField800Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._8xx.BibframeToField810Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._8xx.BibframeToField811Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._8xx.BibframeToField830Converter
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToSeriesAddedEntryFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        BibframeToField800Converter(),
        BibframeToField810Converter(),
        BibframeToField811Converter(),
        BibframeToField830Converter()
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}