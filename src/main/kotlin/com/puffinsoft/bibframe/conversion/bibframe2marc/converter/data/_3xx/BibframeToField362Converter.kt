package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField362Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        queryIssuesData(conn, workData.primaryInstanceId)?.let {
            val builder = DataFieldBuilder().apply {
                tag = "362"
                indicator1 = '0'
            }

            val data = with(StringBuilder()) {
                if (it.firstIssue != null) {
                    append(it.firstIssue.stringValue())
                }

                append('-')

                if (it.lastIssue != null) {
                    append(it.lastIssue.stringValue())
                }

                toString()
            }

            addSubfieldIfExists(builder, 'a', data)

            record.dataFields.add(builder.build())
        }

        queryNoteByType(conn, workData.primaryInstanceId, "number").forEach {
            record.dataFields.add(DataField("362", '1', subfields = mutableListOf(Subfield('a', it.stringValue()))))
        }
    }

    private fun queryIssuesData(conn: RepositoryConnection, id: Value): IssuesData? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?firstIssue ?lastIssue
            WHERE {
                OPTIONAL {
                    ?id bf:firstIssue   ?firstIssue .
                }
                OPTIONAL {
                    ?id bf:lastIssue    ?lastIssue .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("firstIssue") || it.hasBinding("lastIssue") }
                .map { IssuesData(it.getValue("firstIssue"), it.getValue("lastIssue")) }
                .firstOrNull()
        }
    }

    @JvmRecord
    private data class IssuesData(val firstIssue: Value?, val lastIssue: Value?)
}