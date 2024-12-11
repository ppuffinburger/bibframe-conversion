package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField353Converter : BibframeToMarcConverter {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "353" }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.characteristicLabel)
            addSubfieldIfExists(builder, 'b', TextUtils.getCodeStringFromUrl(it.uri))
            addSubfieldIfExists(builder, '0', it.uri)
            addSourceSubfieldIfExists(builder, it.source)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<SupplementaryContentCharacteristicsData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?characteristicLabel ?characteristicId ?source ?sourceId ?appliesTo
            WHERE {
                ?id                 bf:supplementaryContent     ?characteristicId .
                ?characteristicId   rdf:type                    bf:SupplementaryContent ;
                                    rdfs:label                  ?characteristicLabel .
                FILTER(ISIRI(?characteristicId))
                OPTIONAL {
                    ?characteristicId   bf:source   ?sourceId .
                    ?sourceId           rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?characteristicId   bflc:appliesTo  ?appliesToId .
                    ?appliesToId        rdf:type        bflc:AppliesTo ;
                                        rdfs:label      ?appliesTo .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map {
                SupplementaryContentCharacteristicsData(
                    it.getValue("characteristicLabel"),
                    it.getValue("characteristicId"),
                    if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId"),
                    it.getValue("appliesTo")
                )
            }.toList()
        }
    }

    @JvmRecord
    private data class SupplementaryContentCharacteristicsData(val characteristicLabel: Value, val uri: Value, val source: Value?, val appliesTo: Value?)
}