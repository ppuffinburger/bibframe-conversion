package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.PresentationFormatLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField345Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        queryAspectRatio(conn, workData.workId).forEach { record.dataFields.add(buildAspectRatioDataField(it)) }
        queryAspectRatio(conn, workData.primaryInstanceId).forEach { record.dataFields.add(buildAspectRatioDataField(it)) }

        queryProjectionCharacteristic(conn, workData.primaryInstanceId).forEach {
            val builder: DataFieldBuilder = DataFieldBuilder().apply { tag = "345" }
            addSubfieldIfExists(builder, '3', it.appliesTo)

            if (it.projectionCharacteristic != null) {
                addSubfieldIfExists(builder, 'a', it.projectionCharacteristic)
            } else {
                addSubfieldIfExists(builder, 'a', it.presentationFormat)
            }

            addSubfieldIfExists(builder, 'b', it.projectionSpeed)
            addSourceSubfieldIfExists(builder, it.source)

            record.dataFields.add(builder.build())
        }
    }

    private fun buildAspectRatioDataField(data: AspectRatioData): DataField {
        val builder = DataFieldBuilder().apply { tag = "345" }

        addSubfieldIfExists(builder, '3', data.appliesTo)
        addSubfieldIfExists(builder, 'c', data.aspectRatioValue)
        addSubfieldIfExists(builder, 'd', data.designator)
        addSourceSubfieldIfExists(builder, data.source)
        addSubfieldIfExists(builder, '0', data.uri)

        return builder.build()
    }

    private fun queryAspectRatio(conn: RepositoryConnection, id: Value): List<AspectRatioData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?aspectRatioId ?aspectRatioValue ?designator ?source ?sourceId ?appliesTo
            WHERE {
                ?id             bf:aspectRatio  ?aspectRatioId .
                ?aspectRatioId  rdf:type        bf:AspectRatio .
                OPTIONAL {
                    ?aspectRatioId  rdfs:label  ?designator .
                }
                OPTIONAL {
                    ?aspectRatioId  rdf:value   ?aspectRatioValue .
                }
                OPTIONAL {
                    ?aspectRatioId  bf:source   ?sourceId .
                    ?sourceId       rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?aspectRatioId  bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result
                .filter { it.hasBinding("aspectRatioValue") || it.hasBinding("designator") }
                .map { AspectRatioData(it.getValue("aspectRatioId"), it.getValue("aspectRatioValue"), it.getValue("designator"), it.getValue("source") ?: it.getValue("sourceId"), it.getValue("appliesTo")) }.toList()
        }
    }

    private fun queryProjectionCharacteristic(conn: RepositoryConnection, id: Value): List<ProjectionCharacteristicData> {
        val cinemiracleUri = lookupPresentationFormatUri("cinem")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?presentationFormat ?projectionCharacteristic ?projectionSpeed ?source ?sourceId ?appliesTo
            WHERE {
                OPTIONAL {
                    ?id                         bf:projectionCharacteristic ?presentationFormatId .
                    ?presentationFormatId       rdf:type                    bf:PresentationFormat ;
                                                rdfs:label                  ?presentationFormat .
                }
                OPTIONAL {
                    ?id                                 bf:projectionCharacteristic <$cinemiracleUri> .
                    <$cinemiracleUri>  rdf:type                    bf:ProjectionCharacteristic ;
                                                        rdfs:label                  ?projectionCharacteristic .
                }
                OPTIONAL {
                    ?id                         bf:projectionCharacteristic ?projectionSpeedId .
                    ?projectionSpeedId          rdf:type                    bf:ProjectionSpeed ;
                    rdfs:label                  ?projectionSpeed .
                }
                OPTIONAL {
                    ?soundContentId bf:source   ?sourceId .
                    ?sourceId       rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?soundContentId bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("presentationFormat") || it.hasBinding("projectionCharacteristic") }
                .map {
                    ProjectionCharacteristicData(
                        it.getValue("presentationFormat"),
                        it.getValue("projectionCharacteristic"),
                        it.getValue("projectionSpeed"),
                        if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId"),
                        it.getValue("appliesTo")
                    )
                }.toList()
        }
    }

    @JvmRecord
    private data class AspectRatioData(val uri: Value, val aspectRatioValue: Value?, val designator: Value?, val source: Value?, val appliesTo: Value?)

    @JvmRecord
    private data class ProjectionCharacteristicData(val presentationFormat: Value?, val projectionCharacteristic: Value?, val projectionSpeed: Value?, val source: Value?, val appliesTo: Value?)
}