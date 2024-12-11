package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField752Converter : BibframeToMarcConverter {
    // TODO : need examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        queryComponentList(conn, workData.workId).forEach { data ->
            val builder = DataFieldBuilder().apply { tag = "752" }

            data.typesAndLabels.forEach {
                val subfieldName = when(it.type.stringValue()) {
                    "http://www.loc.gov/mads/rdf/v1#Country" -> 'a'
                    "http://www.loc.gov/mads/rdf/v1#State" -> 'b'
                    "http://www.loc.gov/mads/rdf/v1#County" -> 'c'
                    "http://www.loc.gov/mads/rdf/v1#City" -> 'd'
                    "http://www.loc.gov/mads/rdf/v1#CitySection" -> 'f'
                    "http://www.loc.gov/mads/rdf/v1#Region" -> 'g'
                    "http://www.loc.gov/mads/rdf/v1#ExtraterrestrialArea" -> 'h'
                    else -> '9'
                }

                addSubfieldIfExists(builder, subfieldName, it.label.stringValue())
            }

            addSubfieldIfExists(builder, 'e', data.roleLabel)
            addSubfieldIfExists(builder, '4', data.roleCode)
            addSourceSubfieldIfExists(builder, data.source ?: data.sourceId)

            record.dataFields.add(builder.build())
        }
    }

    private fun queryComponentList(conn: RepositoryConnection, id: Value): List<HierarchicalPlaceName> {
        val upperLevelValuesQueryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?placeId ?sourceId ?source ?roleLabel ?roleCode
            WHERE {
                ?id             bf:place                            ?placeId .
                ?placeId        rdf:type                            bf:Place ;
                                rdf:type                            mads:HierarchicalGeographic ;
                                mads:componentList                  ?linkage .
                OPTIONAL {
                    ?placeId    bf:source   ?sourceId .
                    ?sourceId   rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?placeId    bf:role     ?roleId .
                    ?roleId     rdf:type    bf:Role .
                    OPTIONAL {
                        ?roleId rdfs:label  ?roleLabel .
                    }
                    OPTIONAL {
                        ?roleId bf:code     ?roleCode .
                    }
                }
            }
        """.trimIndent()

        val upperLevelValuesQuery = conn.prepareTupleQuery(upperLevelValuesQueryString)
        upperLevelValuesQuery.setBinding("id", id)

        upperLevelValuesQuery.evaluate().use { result ->
            return result.map { subject ->
                val placeId = subject.getValue("placeId")

                val typesAndLabelsQueryString = """
                    PREFIX bf: <$BIBFRAME_ONTOLOGY>
                    PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
                    PREFIX mads: <$LOC_MADS_RDF>
                    SELECT ?typeId ?text (COUNT(?intermediateNode)-1 as ?position)
                    WHERE {
                        ?id                 bf:place                            ?placeId .
                        ?placeId            rdf:type                            bf:Place ;
                                            rdf:type                            mads:HierarchicalGeographic ;
                                            mads:componentList                  ?linkage .
                        ?linkage            rdf:rest*                           ?intermediateNode .
                        ?intermediateNode   rdf:rest*                           ?nodeId .
                        ?nodeId             rdf:first                           ?element .
                        ?element            rdf:type                            ?typeId ;
                                            rdfs:label|mads:authoritativeLabel  ?text .
                        FILTER(?typeId IN (<http://www.loc.gov/mads/rdf/v1#Country>, <http://www.loc.gov/mads/rdf/v1#State>, <http://www.loc.gov/mads/rdf/v1#County>, <http://www.loc.gov/mads/rdf/v1#City>, <http://www.loc.gov/mads/rdf/v1#CitySection>, <http://www.loc.gov/mads/rdf/v1#Region>, <http://www.loc.gov/mads/rdf/v1#ExtraterrestrialArea>))                                        
                    }
                    GROUP BY ?nodeId ?typeId ?text
                    ORDER BY ?position
                """.trimIndent()

                val typesAndLabelsQuery = conn.prepareTupleQuery(typesAndLabelsQueryString)
                typesAndLabelsQuery.setBinding("id", id)
                typesAndLabelsQuery.setBinding("placeId", placeId)

                val typesAndLabels = typesAndLabelsQuery.evaluate().use { result ->
                    result.map { HierarchicalPlaceNameData(it.getValue("typeId"), it.getValue("text")) }.toList()
                }

                HierarchicalPlaceName(placeId, subject.getValue("sourceId"), subject.getValue("source"), subject.getValue("roleLabel"), subject.getValue("roleCode"), typesAndLabels)
            }.toList()
        }
    }

    @JvmRecord
    private data class HierarchicalPlaceNameData(val type: Value, val label: Value)

    @JvmRecord
    private data class HierarchicalPlaceName(val placeId: Value, val sourceId: Value?, val source: Value?, val roleLabel: Value?, val roleCode: Value?, val typesAndLabels: List<HierarchicalPlaceNameData>)
}