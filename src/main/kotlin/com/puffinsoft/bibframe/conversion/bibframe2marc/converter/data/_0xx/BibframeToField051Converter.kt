package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField051Converter : BibframeToMarcConverter {
    // TODO : need examples of items with ClassificationLcc
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        workData.itemIds.forEach {
            query(conn, it).forEach { data ->
                val builder = DataFieldBuilder().apply { this.tag = "051" }

                addSubfieldIfExists(builder, 'a', data.classificationPortion)
                addSubfieldIfExists(builder, 'b', data.itemPortion)
                addSubfieldIfExists(builder, 'c', data.note)

                record.dataFields.add(builder.build())
            }
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<Classification> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?classificationPortion ?itemPortion ?note
            WHERE {
                ?id                 bf:classification           ?classificationId .
                ?classificationId   rdf:type                    bf:ClassificationLcc ;
                                    bf:classificationPortion    ?classificationPortion .
                OPTIONAL {
                    ?classificationId bf:itemPortion ?itemPortion .
                }
                OPTIONAL {
                    ?classificationId   bf:note     ?noteId .
                    ?noteId             rdf:type    bf:Note ;
                                        rdfs:label  ?note .
                }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { Classification(it.getValue("classificationPortion"), it.getValue("itemPortion"), it.getValue("note")) }.toList()
        }
    }

    @JvmRecord
    private data class Classification(val classificationPortion: Value, val itemPortion: Value?, val note: Value?)
}