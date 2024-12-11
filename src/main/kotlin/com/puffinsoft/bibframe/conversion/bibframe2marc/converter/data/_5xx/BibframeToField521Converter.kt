package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField521Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "521"
                indicator1 = getIndicator1(it)
            }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.note)
            record.dataFields.add(builder.build())
        }
    }

    private fun getIndicator1(data: IntendedAudienceNoteData): Char {
        return when (data.note.stringValue()) {
            "reading grade level" -> '0'
            "interest age level" -> '1'
            "interest grade level" -> '2'
            else -> ' '
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<IntendedAudienceNoteData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?note ?appliesTo
            WHERE {
                ?id     bf:intendedAudience ?iaId .
                ?iaId   rdf:type            bf:IntendedAudience ;
                        rdfs:label          ?note .
                OPTIONAL {
                    ?iaId           bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
                FILTER(ISBLANK(?iaId))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { IntendedAudienceNoteData(it.getValue("note"), it.getValue("appliesTo")) }.toList()
        }
    }

    @JvmRecord
    private data class IntendedAudienceNoteData(val note: Value, val appliesTo: Value?)
}