package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField505Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "505"
                indicator1 = '0'
            }
            addSubfieldIfExists(builder, 'a', it.note)
            addSubfieldIfExists(builder, 'u', it.electronicLocator)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<TableOfContentsNoteData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?note ?electronicLocator
            WHERE {
                ?id     bf:tableOfContents  ?tocId .
                ?tocId  rdf:type            bf:TableOfContents ;
                        rdfs:label          ?note .
                OPTIONAL {
                    ?tocId  bf:electronicLocator    ?electronicLocator .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { TableOfContentsNoteData(it.getValue("note"), it.getValue("electronicLocator")) }.toList()
        }
    }

    @JvmRecord
    private data class TableOfContentsNoteData(val note: Value, val electronicLocator: Value?)
}