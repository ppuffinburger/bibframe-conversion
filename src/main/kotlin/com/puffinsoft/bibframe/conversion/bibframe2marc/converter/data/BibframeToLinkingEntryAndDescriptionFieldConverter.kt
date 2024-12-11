package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx.*
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx.BibframeToField760Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx.BibframeToField762Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx.BibframeToField765Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx.BibframeToField767Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx.BibframeToField773Converter
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToLinkingEntryAndDescriptionFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        BibframeToField760Converter(),
        BibframeToField762Converter(),
        BibframeToField765Converter(),
        BibframeToField767Converter(),
        BibframeToField770Converter(),
        BibframeToField772Converter(),
        BibframeToField773Converter(),
        BibframeToField774Converter(),
        BibframeToField775Converter(),
        BibframeToField776Converter(),
        BibframeToField777Converter(),
        BibframeToField780Converter(),
        BibframeToField785Converter(),
        BibframeToField786Converter(),
        BibframeToField787Converter()
        // 788 Not supported in Bibframe
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}