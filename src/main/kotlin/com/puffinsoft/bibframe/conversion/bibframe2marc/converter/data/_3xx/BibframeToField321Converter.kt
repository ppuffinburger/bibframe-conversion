package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField321Converter : AbstractBibframeToPublicationFrequencyConverter() {
    // TODO : Code is wrong.  Spec says frequencies whose object is a BNODE, but code grabs all.  LC data also uses status and not note to say former/current.  Will code to LC for now.
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        convert(conn, workData.primaryInstanceId, record, true)
    }
}