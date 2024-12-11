package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField656Converter : BibframeToMarcConverter {
    // TODO : need examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        queryComponentList(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "656"
                indicator2 = '7'
            }

            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.term)

            it.components.forEach { part ->
                when (part.type.stringValue()) {
                    "http://www.loc.gov/mads/rdf/v1#Topic" -> 'x'
                    "http://www.loc.gov/mads/rdf/v1#GenreForm" -> 'v'
                    "http://www.loc.gov/mads/rdf/v1#Temporal" -> 'y'
                    "http://www.loc.gov/mads/rdf/v1#Geographic" -> 'z'
                    else -> '9'
                }.let { name -> addSubfieldIfExists(builder, name, part.label) }
            }

            if (it.source == null) {
                addSubfieldIfExists(builder, '2', TextUtils.getCodeStringFromUrl(it.sourceId))
            } else {
                addSourceSubfieldIfExists(builder, it.source)
            }

            record.dataFields.add(builder.build())
        }
    }

    private fun queryComponentList(conn: RepositoryConnection, id: Value): List<OccupationTermWithComponents> {
        val upperLevelValuesQueryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?term ?subjectId ?appliesTo ?sourceId ?source
            WHERE {
                ?id             bf:subject                          ?subjectId .
                ?subjectId      rdf:type                            bf:Topic ;
                                rdfs:label                          ?term ;
                                mads:componentList                  ?linkage .
                ?linkage        rdf:first                           ?nodeId .
                ?nodeId         rdf:type                            <http://www.loc.gov/mads/rdf/v1#Occupation> .
                OPTIONAL {
                    ?subjectId      bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
                OPTIONAL {
                    ?subjectId  bf:source   ?sourceId .
                    ?sourceId   rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
            }
        """.trimIndent()

        val upperLevelValuesQuery = conn.prepareTupleQuery(upperLevelValuesQueryString)
        upperLevelValuesQuery.setBinding("id", id)

        upperLevelValuesQuery.evaluate().use { result ->
            return result.map { subject ->
                val subjectId = subject.getValue("subjectId")

                val typesAndLabelsQueryString = """
                    PREFIX bf: <$BIBFRAME_ONTOLOGY>
                    PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
                    PREFIX mads: <$LOC_MADS_RDF>
                    SELECT ?typeId ?text (COUNT(?intermediateNode)-1 as ?position)
                    WHERE {
                        ?id                 bf:subject                          ?subjectId .
                        ?subjectId          rdf:type                            bf:Topic ;
                                            mads:componentList                  ?linkage .
                        ?linkage            rdf:rest*                           ?intermediateNode .
                        ?intermediateNode   rdf:rest*                           ?nodeId .
                        ?nodeId             rdf:first                           ?element .
                        ?element            rdf:type                            ?typeId ;
                                            rdfs:label|mads:authoritativeLabel  ?text .
                        FILTER(?typeId IN (<http://www.loc.gov/mads/rdf/v1#Topic>, <http://www.loc.gov/mads/rdf/v1#GenreForm>, <http://www.loc.gov/mads/rdf/v1#Temporal>, <http://www.loc.gov/mads/rdf/v1#Geographic>))                                        
                    }
                    GROUP BY ?nodeId ?typeId ?text
                    ORDER BY ?position
                """.trimIndent()

                val typesAndLabelsQuery = conn.prepareTupleQuery(typesAndLabelsQueryString)
                typesAndLabelsQuery.setBinding("id", id)
                typesAndLabelsQuery.setBinding("subjectId", subjectId)

                val typesAndLabels = typesAndLabelsQuery.evaluate().use { result ->
                    result.map { OccupationTermSubdivision(it.getValue("typeId"), it.getValue("text")) }.toList()
                }

                OccupationTermWithComponents(subject.getValue("term"), subjectId, subject.getValue("appliesTo"), subject.getValue("sourceId"), subject.getValue("source"), typesAndLabels)
            }.toList()
        }
    }

    @JvmRecord
    private data class OccupationTermSubdivision(val type: Value, val label: Value)

    @JvmRecord
    private data class OccupationTermWithComponents(val term: Value, val uri: Value, val appliesTo: Value?, val sourceId: Value?, val source: Value?, val components: List<OccupationTermSubdivision>)
}