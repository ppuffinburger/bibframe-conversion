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

internal class BibframeToField386Converter : BibframeToMarcConverter {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "386" }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.creatorTerm)
            addSubfieldIfExists(builder, 'b', TextUtils.getCodeStringFromUrl(it.creatorCodeUri))
            addSubfieldIfExists(builder, 'm', it.demographicGroupTerm)
            addSubfieldIfExists(builder, 'n', TextUtils.getCodeStringFromUrl(it.demographicGroupUri))
            addSubfieldIfExists(builder, '0', it.creatorCodeUri)
            addSourceSubfieldIfExists(builder, it.source)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<CreatorCharacteristicsData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?creatorTerm ?creatorCodeUri ?demographicGroupTerm ?demographicGroupUri ?source ?sourceId ?appliesTo
            WHERE {
                ?id                 bflc:creatorCharacteristic  ?creatorCodeUri .
                ?creatorCodeUri     rdf:type                    bflc:CreatorCharacteristic ;
                                    rdfs:label                  ?creatorTerm .
                OPTIONAL {
                    ?creatorCodeUri         bflc:demographicGroup   ?demographicGroupUri .
                    ?demographicGroupUri    rdf:type                bflc:DemographicGroup ;
                                            rdfs:label              ?demographicGroupTerm .
                }
                OPTIONAL {
                    ?audienceCodeUri    bf:source   ?sourceId .
                    ?sourceId           rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?audienceCodeUri    bflc:appliesTo  ?appliesToId .
                    ?appliesToId        rdf:type        bflc:AppliesTo ;
                                        rdfs:label      ?appliesTo .
                }
                FILTER(!ISBLANK(?audienceCodeUri))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map {
                CreatorCharacteristicsData(
                    it.getValue("creatorTerm"),
                    it.getValue("creatorCodeUri"),
                    it.getValue("demographicGroupTerm"),
                    it.getValue("demographicGroupUri"),
                    if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId"),
                    it.getValue("appliesTo")
                )
            }.toList()
        }
    }

    @JvmRecord
    private data class CreatorCharacteristicsData(val creatorTerm: Value, val creatorCodeUri: Value, val demographicGroupTerm: Value?, val demographicGroupUri: Value?, val source: Value?, val appliesTo: Value?)
}