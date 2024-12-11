package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx.*
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToNoteFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        BibframeToField500Converter(),
        BibframeToField501Converter(),
        BibframeToField502Converter(),
        BibframeToField504Converter(),
        BibframeToField505Converter(),
        BibframeToField506Converter(),
        // 507 Not supported in Bibframe
        BibframeToField508Converter(),  // Specs doesn't have, Code does and is just like 511, except doesn't start with 'Cast:'.
        BibframeToField510Converter(),
        BibframeToField511Converter(),
        BibframeToField513Converter(),
        // 514 Not supported in Bibframe
        BibframeToField515Converter(),
        BibframeToField516Converter(),
        BibframeToField518Converter(),
        BibframeToField520Converter(),
        BibframeToField521Converter(),
        BibframeToField522Converter(),
        BibframeToField524Converter(),
        // 525 Not supported in Bibframe
        // 526 Not supported in Bibframe
        BibframeToField530Converter(),
        BibframeToField532Converter(),
        BibframeToField533Converter(),
        BibframeToField534Converter(),
        // 535 Not supported in Bibframe
        BibframeToField536Converter(),
        BibframeToField538Converter(),
        // 540 Not supported in Bibframe
        BibframeToField541Converter(),
        // 542 Not supported in Bibframe
        // 544 Not supported in Bibframe
        BibframeToField545Converter(),
        BibframeToField546Converter(),
        // 547 Not supported in Bibframe
        BibframeToField550Converter(),
        // 552 Not supported in Bibframe
        BibframeToField555Converter(),
        // 556 Not supported in Bibframe
        BibframeToField561Converter(),
        // 562 Not supported in Bibframe
        BibframeToField563Converter(),
        // 565 Not supported in Bibframe
        // 567 Not supported in Bibframe
        // 580 Not supported in Bibframe
        BibframeToField581Converter(),
        BibframeToField583Converter(),
        // 584 Not supported in Bibframe
        BibframeToField585Converter(),
        BibframeToField586Converter(),
        BibframeToField588Converter()
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}