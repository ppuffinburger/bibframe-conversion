package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.*
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField346Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply { tag = "346" }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.videoFormat)
            addSubfieldIfExists(builder, 'b', it.broadcastStandard)
            addSubfieldIfExists(builder, '0', it.uri)
            addSourceSubfieldIfExists(builder, it.source)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<VideoCharacteristicsData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?videoCharacteristicId ?videoFormat ?broadcastStandard ?source ?sourceId ?appliesTo
            WHERE {
                {
                    SELECT ?videoCharacteristicId (COALESCE(?authLabel, ?vfLabel) AS ?videoFormat)
                    WHERE {
                        ?id                     bf:videoCharacteristic      ?videoCharacteristicId .
                        ?videoCharacteristicId  rdf:type                    bf:VideoFormat .
                        OPTIONAL {
                            GRAPH <$VIDEO_FORMAT_URI> {
                                ?videoCharacteristicId  mads:authoritativeLabel ?authLabel .
                            }
                        }
                        OPTIONAL {
                            ?videoCharacteristicId  rdfs:label  ?vfLabel .
                        }
                    }
                }
                UNION
                {
                    SELECT ?videoCharacteristicId (COALESCE(?authLabel, ?bsLabel) AS ?broadcastStandard)
                    WHERE {
                        ?id                     bf:videoCharacteristic      ?videoCharacteristicId .
                        ?videoCharacteristicId  rdf:type                    bf:BroadcastStandard .
                        OPTIONAL {
                            GRAPH <$BROADCAST_STANDARD_URI> {
                                ?videoCharacteristicId  mads:authoritativeLabel ?authLabel .
                            }
                        }
                        OPTIONAL {
                            ?videoCharacteristicId  rdfs:label  ?bsLabel .
                        }
                    }
                }
                OPTIONAL {
                    ?videoCharacteristicId  bf:source   ?sourceId .
                    ?sourceId               rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?videoCharacteristicId  bflc:appliesTo  ?appliesToId .
                    ?appliesToId            rdf:type        bflc:AppliesTo ;
                                            rdfs:label      ?appliesTo .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map {
                VideoCharacteristicsData(
                    it.getValue("videoCharacteristicId"),
                    it.getValue("videoFormat"),
                    it.getValue("broadcastStandard"),
                    it.getValue("source") ?: it.getValue("sourceId"),
                    it.getValue("appliesTo")
                )
            }.toList()
        }
    }

    @JvmRecord
    private data class VideoCharacteristicsData( val uri: Value,val videoFormat: Value?, val broadcastStandard: Value?, val source: Value?, val appliesTo: Value?)
}