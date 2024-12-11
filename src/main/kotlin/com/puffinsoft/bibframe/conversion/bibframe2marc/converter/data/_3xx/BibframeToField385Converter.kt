package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.INTENDED_AUDIENCE_URI
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField385Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "385" }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.audienceTerm)
            addSubfieldIfExists(builder, 'b', TextUtils.getCodeStringFromUrl(it.audienceCodeUri))
            addSubfieldIfExists(builder, 'm', it.demographicGroupTerm)
            addSubfieldIfExists(builder, 'n', TextUtils.getCodeStringFromUrl(it.demographicGroupUri))
            addSubfieldIfExists(builder, '0', it.audienceCodeUri)
            addSourceSubfieldIfExists(builder, it.source)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<AudienceCharacteristicsData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT DISTINCT (COALESCE(?authLabel, ?mfLabel) AS ?audienceTerm) ?audienceCodeUri ?demographicGroupTerm ?demographicGroupUri ?source ?sourceId ?appliesTo
            WHERE {
                ?id                 bf:intendedAudience ?audienceCodeUri .
                ?audienceCodeUri    rdf:type            bf:IntendedAudience .
                OPTIONAL {
                    GRAPH <$INTENDED_AUDIENCE_URI> {
                        ?audienceCodeUri    mads:authoritativeLabel ?authLabel .
                    }
                }
                OPTIONAL {
                    ?audienceCodeUri    rdfs:label  ?mfLabel .
                }
                OPTIONAL {
                    ?audienceCodeUri        bflc:demographicGroup   ?demographicGroupUri .
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
                AudienceCharacteristicsData(
                    it.getValue("audienceTerm"),
                    it.getValue("audienceCodeUri"),
                    it.getValue("demographicGroupTerm"),
                    it.getValue("demographicGroupUri"),
                    if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId"),
                    it.getValue("appliesTo")
                )
            }.toList()
        }
    }

    @JvmRecord
    private data class AudienceCharacteristicsData(val audienceTerm: Value, val audienceCodeUri: Value, val demographicGroupTerm: Value?, val demographicGroupUri: Value?, val source: Value?, val appliesTo: Value?)
}