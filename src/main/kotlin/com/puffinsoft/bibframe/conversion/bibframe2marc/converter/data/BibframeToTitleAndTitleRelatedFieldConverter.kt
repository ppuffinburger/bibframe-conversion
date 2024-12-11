package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx.*
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToTitleAndTitleRelatedFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        BibframeToField210Converter(),
        BibframeToField222Converter(),
        BibframeToField240Converter(),
        BibframeToField242Converter(),
        BibframeToField243Converter(),
        BibframeToField245Converter(),
        BibframeToField246Converter(),
        BibframeToField247Converter()
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}