package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField256Converter : BibframeToMarcConverter {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId)?.let {
            val builder = DataFieldBuilder().apply { tag = "256" }
            addSubfieldIfExists(builder, 'a', it)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): Value? {
        val typeOfComputerDataNoteUri = lookupNoteTypeUri("computer")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?characteristics
            WHERE {
                ?id     bf:note     ?noteId .
                ?noteId rdf:type    bf:Note ;
                        rdfs:label  ?characteristics .
                OPTIONAL {
                    ?noteId rdf:type    ?noteIdType .
                }
                OPTIONAL {
                    ?noteId bf:noteType ?noteType .
                }
                FILTER(?noteIdType = <$typeOfComputerDataNoteUri> || ?noteType = "computer file characteristics")
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { it.getValue("characteristics") }.firstOrNull()
        }
    }
}