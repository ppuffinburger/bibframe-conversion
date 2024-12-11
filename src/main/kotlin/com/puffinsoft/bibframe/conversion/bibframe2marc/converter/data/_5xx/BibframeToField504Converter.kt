package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField504Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        // Supplementary Content has been changed to a Note, keeping for now for older data
        querySupplementaryContent(conn, workData.workId).forEach {
            record.dataFields.add(buildDataField(it))
        }

        querySupplementaryContent(conn, workData.primaryInstanceId).forEach {
            record.dataFields.add(buildDataField(it))
        }

        queryNote(conn, workData.primaryInstanceId).forEach {
            record.dataFields.add(buildDataField(it))
        }
    }

    private fun buildDataField(data: BibliographyNoteData): DataField {
        val builder = DataFieldBuilder().apply { tag = "504" }

        addSubfieldIfExists(builder, 'a', data.note)
        addSubfieldIfExists(builder, 'b', data.count)

        return builder.build()
    }

    private fun queryNote(conn: RepositoryConnection, instanceId: Value): List<BibliographyNoteData> {
        val biblioUri = lookupNoteTypeUri("biblio")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?note ?count
            WHERE {
                ?id         bf:note                 ?noteId .
                ?noteId     rdf:type                bf:Note ;
                            rdfs:label              ?note .
                OPTIONAL {
                    ?noteId  bf:count    ?count .
                }
                FILTER(?noteId = <$biblioUri>)
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", instanceId)

        query.evaluate().use { result ->
            return result.map { BibliographyNoteData(it.getValue("note"), it.getValue("count")) }.toList()
        }
    }

    private fun querySupplementaryContent(conn: RepositoryConnection, id: Value): List<BibliographyNoteData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?note ?count
            WHERE {
                ?id         bf:supplementaryContent ?contentId .
                ?contentId  rdf:type                bf:SupplementaryContent ;
                            rdfs:label              ?note .
                OPTIONAL {
                    ?contentId  bf:count    ?count .
                }
                FILTER(ISBLANK(?contentId))
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { BibliographyNoteData(it.getValue("note"), it.getValue("count")) }.toList()
        }
    }

    @JvmRecord
    private data class BibliographyNoteData(val note: Value, val count: Value?)
}