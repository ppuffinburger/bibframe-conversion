package com.puffinsoft.bibframe.conversion.bibframe2marc.converter

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data.*
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToDataFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        BibframeToNumbersAndCodesDataFieldConverter(),                  // 010-088
        BibframeToMainEntryFieldConverter(),                            // 100-130
        BibframeToTitleAndTitleRelatedFieldConverter(),                 // 210-247
        BibframeToEditionImprintEtcFieldConverter(),                    // 250-270
        BibframeToPhysicalDescriptionEtcFieldConverter(),               // 300-388
        BibframeToSeriesStatementFieldConverter(),                      // 490
        BibframeToNoteFieldConverter(),                                 // 500-588
        BibframeToSubjectAccessFieldConverter(),                        // 600-688
        BibframeToAddedEntryFieldConverter(),                           // 700-758
        BibframeToLinkingEntryAndDescriptionFieldConverter(),           // 760-788
        BibframeToSeriesAddedEntryFieldConverter(),                     // 800-830
        BibframeToHoldingsLocationAlternateGraphicsEtcFieldConverter()  // 850-887
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}