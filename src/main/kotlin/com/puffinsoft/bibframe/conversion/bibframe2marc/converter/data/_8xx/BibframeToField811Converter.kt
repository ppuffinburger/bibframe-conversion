package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._8xx

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField811Converter : AbstractBibframeToSeriesAddedEntryConverter() {
    private val agentTypes = listOf("http://id.loc.gov/ontologies/bibframe/Meeting")

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        convert(conn, workData.workId, record, "811", agentTypes)
    }
}