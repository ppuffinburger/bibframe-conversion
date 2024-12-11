package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField581Converter : BibframeToMarcConverter {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "581"
                indicator1 = '8'
            }
            addSubfieldIfExists(builder, 'a', it.note)
            addSubfieldIfExists(builder, 'z', it.isbn)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<PublicationsAboutDescribedMaterialsNoteData> {
        val relatedUri = lookupNoteTypeUri("related")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?note ?isbn
            WHERE {
                ?id         bf:note     ?noteId .
                ?noteId     rdf:type    bf:Note ;
                            rdf:type    <$relatedUri> ;
                            rdfs:label  ?note .
                OPTIONAL {
                    ?noteId bf:identifiedBy ?isbnId .
                    ?isbnId rdf:type        bf:Isbn ;
                            rdf:value       ?isbn .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { PublicationsAboutDescribedMaterialsNoteData(it.getValue("note"), it.getValue("isbn")) }.toList()
        }
    }

    @JvmRecord
    private data class PublicationsAboutDescribedMaterialsNoteData(val note: Value, val isbn: Value?)
}