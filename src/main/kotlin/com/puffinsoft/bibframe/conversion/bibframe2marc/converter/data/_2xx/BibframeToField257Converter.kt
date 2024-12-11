package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField257Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply { tag = "257" }

            addSubfieldIfExists(builder, 'a', it.place)
            addSourceSubfieldIfExists(builder, it.source)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<CountryOfProducingEntityData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?place ?source ?sourceId
            WHERE {
                ?id         bf:originPlace  ?placeId .
                ?placeId    rdf:type        bf:Place ;
                            rdfs:label      ?place .
                OPTIONAL {
                    ?placeId    bf:source   ?sourceId .
                    ?sourceId   rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { CountryOfProducingEntityData(it.getValue("place"), if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId")) }.toList()
        }
    }

    @JvmRecord
    private data class CountryOfProducingEntityData(val place: Value, val source: Value?)
}