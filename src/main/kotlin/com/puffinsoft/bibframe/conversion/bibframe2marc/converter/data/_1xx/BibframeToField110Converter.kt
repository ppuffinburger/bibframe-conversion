package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._1xx

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField110Converter : AbstractBibframeToMainEntryConverter() {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        convert(conn, workData, record, "Organization")
    }
}