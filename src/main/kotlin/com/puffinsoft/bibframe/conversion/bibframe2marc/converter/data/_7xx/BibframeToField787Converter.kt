package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField787Converter : AbstractBibframeToLinkingEntryConverter() {
    // TODO : mostly correct.  might have language based names (although spec doesn't show 880's) and $i might be a different path
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        convert(conn, workData, record, "787", '8', "http://id.loc.gov/vocabulary/relationship/relatedwork")
    }
}