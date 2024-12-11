package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx.*
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx.BibframeToField600Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx.BibframeToField610Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx.BibframeToField611Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx.BibframeToField630Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx.BibframeToField648Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx.BibframeToField650Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx.BibframeToField651Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx.BibframeToField653Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx.BibframeToField655Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx.BibframeToField656Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx.BibframeToField662Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx.*
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx.BibframeToField700Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx.BibframeToField710Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx.BibframeToField711Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx.BibframeToField730Converter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx.BibframeToField758Converter
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToAddedEntryFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        BibframeToField700Converter(),
        BibframeToField710Converter(),
        BibframeToField711Converter(),
        BibframeToField720Converter(),
        BibframeToField730Converter(),
        BibframeToField740Converter(),
        // 751 Not supported in Bibframe
        BibframeToField752Converter(),
        BibframeToField753Converter(),
        // 754 Not supported in Bibframe
        BibframeToField758Converter()
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}