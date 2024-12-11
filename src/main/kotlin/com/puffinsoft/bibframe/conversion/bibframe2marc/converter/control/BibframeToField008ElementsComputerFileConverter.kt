package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.TypeOfComputerFileLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal object BibframeToField008ElementsComputerFileConverter : AbstractBibframeToField008ElementsConverter() {
    override fun convert(conn: RepositoryConnection, workData: WorkData): String {
        with(StringBuilder()) {
            append("||||")
            append(getTargetAudience(conn, workData.workId))
            append(getFormOfItem(conn, workData))
            append("||")
            append(getTypeOfComputerFile(workData.workGenreForms))
            append('|')
            append(getGovernmentPublication(conn, workData.workId))
            append("||||||")

            return toString()
        }
    }

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        TODO("Not implemented")
    }

    private fun getTypeOfComputerFile(genreForms: Set<Value>): Char {
        return genreForms.firstNotNullOfOrNull { TypeOfComputerFileLookup.lookup(it) } ?: '|'
    }
}