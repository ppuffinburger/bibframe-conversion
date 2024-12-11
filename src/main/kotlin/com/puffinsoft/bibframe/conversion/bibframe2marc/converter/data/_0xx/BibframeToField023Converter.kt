package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField023Converter : AbstractBibframeToIssnConverter() {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        addIssns(query(conn, workData.workId, true), record, 23)

        for (instanceId in workData.getAllInstanceIds()) {
            addIssns(query(conn, instanceId, true), record, 23)
        }
    }
}