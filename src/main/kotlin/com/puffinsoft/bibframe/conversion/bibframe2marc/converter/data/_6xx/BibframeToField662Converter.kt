package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField662Converter : BibframeToMarcConverter {
    // TODO : need examples.  Spec doesn't say it's a component list, but code does.  Makes sense as a component list, so doing that for now
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        queryComponentList(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "662" }

            it.parts.forEach { part ->
                when (part.type.stringValue()) {
                    "http://www.loc.gov/mads/rdf/v1#Country" -> 'a'
                    "http://www.loc.gov/mads/rdf/v1#State" -> 'b'
                    "http://www.loc.gov/mads/rdf/v1#County" -> 'c'
                    "http://www.loc.gov/mads/rdf/v1#City" -> 'd'
                    "http://www.loc.gov/mads/rdf/v1#CitySection" -> 'f'
                    "http://www.loc.gov/mads/rdf/v1#Region" -> 'g'
                    "http://www.loc.gov/mads/rdf/v1#ExtraterrestrialArea" -> 'h'
                    else -> '9'
                }.let { name -> addSubfieldIfExists(builder, name, part.label) }
            }

            if (it.uri.isIRI) {
                addSubfieldIfExists(builder, '1', it.uri)
            }

            addSourceSubfieldIfExists(builder, it.source ?: it.sourceId)

            record.dataFields.add(builder.build())
        }
    }

    private fun queryComponentList(conn: RepositoryConnection, id: Value): List<HierarchicalPlaceNameWithComponents> {
        val upperLevelValuesQueryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?subjectId ?sourceId ?source
            WHERE {
                SELECT ?subjectId ?sourceId ?source
                WHERE {
                    {
                        ?id             bf:subject                          ?subjectId .
                        ?subjectId      rdf:type                            bf:Place ;
                                        rdf:type                            mads:HierarchicalGeographic ;
                    }
                    OPTIONAL {
                        ?subjectId  bf:source   ?sourceId .
                        ?sourceId   rdf:type    bf:Source .
                        OPTIONAL {
                            ?sourceId   bf:code ?source .
                        }
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
                    PREFIX mads: <$LOC_MADS_RDF>
                    SELECT ?typeId ?text (COUNT(?intermediateNode)-1 as ?position)
                    WHERE {
                        ?id                 bf:subject                          ?subjectId .
                        ?subjectId          rdf:type                            bf:Place ;
                                            rdf:type                            mads:HierarchicalGeographic ;
                                            mads:componentList                  ?linkage .
                        ?linkage            rdf:rest*                           ?intermediateNode .
                        ?intermediateNode   rdf:rest*                           ?nodeId .
                        ?nodeId             rdf:first                           ?element .
                        ?element            rdf:type                            ?typeId ;
                                            rdfs:label|mads:authoritativeLabel  ?text .
                    }
                    GROUP BY ?nodeId ?typeId ?text
                    ORDER BY ?position
                """.trimIndent()

                val typesAndLabelsQuery = conn.prepareTupleQuery(typesAndLabelsQueryString)
                typesAndLabelsQuery.setBinding("id", id)
                typesAndLabelsQuery.setBinding("subjectId", subjectId)

                val typesAndLabels = typesAndLabelsQuery.evaluate().use { result ->
                    result.map { PlaceData(it.getValue("typeId"), it.getValue("text")) }.toList()
                }

                HierarchicalPlaceNameWithComponents(
                    subjectId,
                    subject.getValue("sourceId"),
                    subject.getValue("source"),
                    typesAndLabels
                )
            }.toList()
        }
    }

    @JvmRecord
    private data class PlaceData(val type: Value, val label: Value)

    @JvmRecord
    private data class HierarchicalPlaceNameWithComponents(val uri: Value, val sourceId: Value?, val source: Value?, val parts: List<PlaceData>)
}