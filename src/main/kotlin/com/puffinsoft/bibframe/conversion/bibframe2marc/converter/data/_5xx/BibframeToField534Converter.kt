package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField534Converter : BibframeToMarcConverter {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply { tag = "534" }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.note)
            addSubfieldIfExists(builder, 'x', it.issn)
            addSubfieldIfExists(builder, 'z', it.isbn)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<OriginalVersionNoteData> {
        val originalVersionUri = lookupNoteTypeUri("orig")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?note ?appliesTo ?issn ?isbn
            WHERE {
                ?id         bf:note     ?noteId .
                ?noteId     rdf:type    bf:Note ;
                            rdf:type    <$originalVersionUri> ;
                            rdfs:label  ?note .
                OPTIONAL {
                    ?noteId         bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
                OPTIONAL {
                    ?noteId bf:identifiedBy ?issnId .
                    ?issnId rdf:type        bf:Issn ;
                            rdf:value       ?issn .
                }
                OPTIONAL {
                    ?noteId bf:identifiedBy ?isbnId .
                    ?isbnId rdf:type        bf:Isbn ;
                            rdf:value       ?isbn .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { OriginalVersionNoteData(it.getValue("note"), it.getValue("appliesTo"), it.getValue("issn"), it.getValue("isbn")) }.toList()
        }
    }

    @JvmRecord
    private data class OriginalVersionNoteData(val note: Value, val appliesTo: Value?, val issn: Value?, val isbn: Value?)
}