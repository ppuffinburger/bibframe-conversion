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

internal class BibframeToField338Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        workData.getAllInstanceIds().forEach { instanceId ->
            query(conn, instanceId).forEach {
                val builder = DataFieldBuilder().apply { tag = "338" }
                addSubfieldIfExists(builder, '3', it.appliesTo)
                addSubfieldIfExists(builder, 'a', it.carrierType)
                addSubfieldIfExists(builder, 'b', TextUtils.getCodeStringFromUrl(it.uri))
                addSubfieldIfExists(builder, '0', it.uri)
                addSourceSubfieldIfExists(builder, it.source)
                record.dataFields.add(builder.build())
            }
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<CarrierType> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT DISTINCT ?carrierType ?carrierTypeId ?source ?sourceId ?appliesTo ?mainTitle
            WHERE {
                ?id             bf:carrier  ?carrierTypeId .
                ?carrierTypeId  rdf:type    bf:Carrier ;
                                rdfs:label  ?carrierType .
                OPTIONAL {
                    ?carrierTypeId  bf:source   ?sourceId .
                    ?sourceId       rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?carrierTypeId  bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
                OPTIONAL {
                    ?id         rdf:type        bflc:SecondaryInstance ;
                                bf:title        ?titleId .
                    ?titleId    rdf:type        bf:Title ;
                                bf:mainTitle    ?mainTitle .
                }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { CarrierType(it.getValue("carrierType"), it.getValue("carrierTypeId"), if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId"), it.getValue("appliesTo") ?: it.getValue("mainTitle")) }.toList()
        }
    }

    @JvmRecord
    private data class CarrierType(val carrierType: Value, val uri: Value, val source: Value?, val appliesTo: Value?)
}