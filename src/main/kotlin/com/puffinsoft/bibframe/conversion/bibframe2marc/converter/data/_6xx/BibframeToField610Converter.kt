package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField610Converter : AbstractBibframeToSubjectAddedEntryConverter() {
    private val madsNameTypes = listOf("http://www.loc.gov/mads/rdf/v1#CorporateName")
    private val bibframeNameTypes = listOf("http://id.loc.gov/ontologies/bibframe/Organization")

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        convert(conn, workData.workId, record, "610", madsNameTypes, bibframeNameTypes)
    }
}