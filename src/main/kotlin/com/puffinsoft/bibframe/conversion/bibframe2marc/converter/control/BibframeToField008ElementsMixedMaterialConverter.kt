package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal object BibframeToField008ElementsMixedMaterialConverter : AbstractBibframeToField008ElementsConverter() {
    override fun convert(conn: RepositoryConnection, workData: WorkData): String {
        with(StringBuilder()) {
            append("     ")
            append(getFormOfItem(conn, workData))
            append("           ")

            return toString()
        }
    }

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        TODO("Not implemented")
    }
}