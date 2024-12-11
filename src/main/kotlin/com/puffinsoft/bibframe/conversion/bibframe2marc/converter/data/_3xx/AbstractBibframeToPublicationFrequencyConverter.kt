package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.StatusCodeLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal abstract class AbstractBibframeToPublicationFrequencyConverter : BibframeToMarcConverter {
    protected fun convert(conn: RepositoryConnection, id: Value, record: BibliographicRecord, queryForFormer: Boolean) {
        val formerUri = lookupStatusCodeUri("former")

        val additionalFilter = if (queryForFormer) {
            "?statusId = <$formerUri>"
        } else {
            "?statusId != <$formerUri>"
        }

        query(conn, id, additionalFilter).forEach {
            val builder = DataFieldBuilder().apply { tag = if (queryForFormer) "321" else "310" }

            addSubfieldIfExists(builder, 'a', it.frequency)
            addSubfieldIfExists(builder, 'b', it.date)
            addSourceSubfieldIfExists(builder, it.source)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value, additionalFilter: String): List<PublicationFrequencyData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?frequency ?date ?source ?sourceId
            WHERE {
                ?id             bf:frequency    ?frequencyId .
                ?frequencyId    rdf:type        bf:Frequency ;
                                rdfs:label      ?frequency ;
                                bf:status       ?statusId .
                ?statusId       rdf:type        bf:Status .
                OPTIONAL {
                    ?frequencyId    bf:date     ?date .
                }
                OPTIONAL {
                    ?frequencyId    bf:source   ?sourceId .
                    ?sourceId       rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code     ?source .
                    }
                }
                FILTER(ISBLANK(?frequencyId) && $additionalFilter)
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { PublicationFrequencyData(it.getValue("frequency"), it.getValue("date"), if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId")) }.toList()
        }
    }

    @JvmRecord
    private data class PublicationFrequencyData(val frequency: Value, val date: Value?, val source: Value?)
}