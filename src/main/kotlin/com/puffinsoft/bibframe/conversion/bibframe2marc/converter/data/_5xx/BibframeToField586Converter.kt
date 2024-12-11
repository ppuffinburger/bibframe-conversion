package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField586Converter : BibframeToMarcConverter {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "586" }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.note)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<AwardsNoteData> {
        val awardUri = lookupNoteTypeUri("award")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?note ?appliesTo ?institution ?institutionId
            WHERE {
                ?id         bf:note     ?noteId .
                ?noteId     rdf:type    bf:Note ;
                            rdf:type    <$awardUri> ;
                            rdfs:label  ?note .
                OPTIONAL {
                    ?noteId         bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { AwardsNoteData(it.getValue("note"), it.getValue("appliesTo")) }.toList()
        }
    }

    @JvmRecord
    private data class AwardsNoteData(val note: Value, val appliesTo: Value?)
}