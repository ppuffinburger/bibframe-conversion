package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx.*
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToSubjectAccessFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        BibframeToField600Converter(),
        BibframeToField610Converter(),
        BibframeToField611Converter(),
        BibframeToField630Converter(),
        // 647 Not supported in Bibframe
        BibframeToField648Converter(),
        BibframeToField650Converter(),
        BibframeToField651Converter(),
        BibframeToField653Converter(),
        // 654 Not supported in Bibframe
        BibframeToField655Converter(),
        BibframeToField656Converter(),
        // 657 Not supported in Bibframe
        // 658 Not supported in Bibframe
        BibframeToField662Converter()
        // 688 Not supported in Bibframe
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}