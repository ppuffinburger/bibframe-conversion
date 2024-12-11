package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField777Converter : AbstractBibframeToLinkingEntryConverter() {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        convert(conn, workData, record, "777", '8', "http://id.loc.gov/vocabulary/relationship/issuedwith")
    }
}