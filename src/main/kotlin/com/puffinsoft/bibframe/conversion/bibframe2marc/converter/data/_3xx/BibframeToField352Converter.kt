package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField352Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply { tag = "352" }
            addSubfieldIfExists(builder, 'a', it.dataTypeLabel)
            addSubfieldIfExists(builder, 'b', it.objectTypeLabel)
            addSubfieldIfExists(builder, 'c', it.objectCount)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<CartographicDigitalCharacteristicData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?dataTypeLabel ?objectTypeLabel ?objectCount
            WHERE {
                ?id     bf:digitalCharacteristic    ?dcId .
                OPTIONAL {
                    ?dcId   rdf:type    bf:CartographicDataType ;
                            rdfs:label  ?dataTypeLabel .
                }
                OPTIONAL {
                    ?dcId   rdf:type    bf:CartographicObjectType ;
                            rdfs:label  ?objectTypeLabel .
                }
                OPTIONAL {
                    ?dcId   rdf:type    bf:ObjectCount ;
                            bf:count    ?objectCount .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("dataTypeLabel") || it.hasBinding("objectTypeLabel") || it.hasBinding("objectCount") }
                .map { CartographicDigitalCharacteristicData(it.getValue("dataTypeLabel"), it.getValue("objectTypeLabel"), it.getValue("objectCount")) }
                .toList()
        }
    }

    @JvmRecord
    private data class CartographicDigitalCharacteristicData(val dataTypeLabel: Value?, val objectTypeLabel: Value?, val objectCount: Value?)
}