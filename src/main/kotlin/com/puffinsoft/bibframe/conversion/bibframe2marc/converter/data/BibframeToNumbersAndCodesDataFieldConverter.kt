package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx.*
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx.BibframeToField010Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx.BibframeToField015Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx.BibframeToField016Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx.BibframeToField017Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx.BibframeToField020Converter
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToNumbersAndCodesDataFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        BibframeToField010Converter(),
        // 013 Not supported in Bibframe
        BibframeToField015Converter(),
        BibframeToField016Converter(),
        BibframeToField017Converter(),
        // 018 Not supported in Bibframe
        BibframeToField020Converter(),
        BibframeToField022Converter(),
        BibframeToField023Converter(),
        BibframeToField024Converter(),
        BibframeToField025Converter(),
        BibframeToField026Converter(),
        BibframeToField027Converter(),
        BibframeToField028Converter(),
        BibframeToField030Converter(),
        // 031 Not supported in Bibframe
        BibframeToField032Converter(),
        BibframeToField033Converter(),
        BibframeToField034Converter(),
        BibframeToField035Converter(),
        BibframeToField036Converter(),
        BibframeToField037Converter(),
        BibframeToField038Converter(),
        BibframeToField040Converter(),
        BibframeToField041Converter(),
        BibframeToField042Converter(),
        BibframeToField043Converter(),
        // 044 Not supported in Bibframe
        BibframeToField045Converter(),
        BibframeToField046Converter(),
        // 047 Not supported in Bibframe
        BibframeToField048Converter(),
        BibframeToField050Converter(),
        BibframeToField051Converter(),
        BibframeToField052Converter(),
        BibframeToField055Converter(),
        BibframeToField060Converter(),
        // 061 Not supported in Bibframe
        // 066 Not supported in Bibframe
        BibframeToField070Converter(),
        // 071 Not supported in Bibframe
        BibframeToField072Converter(),
        BibframeToField074Converter(),
        BibframeToField080Converter(),
        BibframeToField082Converter(),
        // 083 Not supported in Bibframe
        BibframeToField084Converter(),
        // 085 Not supported in Bibframe
        BibframeToField086Converter(),
        BibframeToField088Converter()
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}