package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.MarcKeyUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.SingleIndicatorConfig
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField651Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            when(it) {
                is GeographicNameWithMarcKey -> {
                    val builder = MarcKeyUtils.parseMarcKey(it.marcKey.stringValue(), SingleIndicatorConfig(SingleIndicatorConfig.IndicatorPlacement.USE_DEFAULTS, ' ', '0')).apply {
                        tag = "651"
                        indicator2 = if (it.uri.isIRI) '0' else getIndicator2(it.uri, it.sourceId)
                    }

                    if (builder.indicator2 == '7') {
                        if (it.source == null && it.sourceId == null) {
                            addSubfieldIfExists(builder, '2', TextUtils.getCodeStringFromUrl(it.uri.stringValue()?.substringBeforeLast("/") ?: ""))
                        } else {
                            addSourceSubfieldIfExists(builder, it.source ?: it.sourceId)
                        }
                    }

                    if (it.uri.isIRI) {
                        addSubfieldIfExists(builder, '1', it.uri)
                    }

                    record.dataFields.add(builder.build())
                }
                is GeographicNameWithComponents -> {
                    val builder = DataFieldBuilder().apply {
                        tag = "651"
                        indicator2 = getIndicator2(it.uri, it.sourceId)
                    }

                    addSubfieldIfExists(builder, '3', it.appliesTo)

                    var wroteFirstGeographic = false
                    it.parts.forEach { part ->
                        when (part.type.stringValue()) {
                            "http://www.loc.gov/mads/rdf/v1#Geographic" -> {
                                if (wroteFirstGeographic) {
                                    'z'
                                } else {
                                    wroteFirstGeographic = true
                                    'a'
                                }
                            }
                            "http://www.loc.gov/mads/rdf/v1#GenreForm" -> 'v'
                            "http://www.loc.gov/mads/rdf/v1#Temporal" -> 'y'
                            "http://www.loc.gov/mads/rdf/v1#Topic" -> 'x'
                            else -> '9'
                        }.let { name -> addSubfieldIfExists(builder, name, part.label) }
                    }

                    if (builder.indicator2 == '7') {
                        if (it.source == null && it.sourceId == null) {
                            addSubfieldIfExists(builder, '2', TextUtils.getCodeStringFromUrl(it.uri.stringValue()?.substringBeforeLast("/") ?: ""))
                        } else {
                            addSourceSubfieldIfExists(builder, it.source ?: it.sourceId)
                        }
                    }

                    if (it.uri.isIRI) {
                        addSubfieldIfExists(builder, '1', it.uri)
                    }

                    record.dataFields.add(builder.build())                }
            }
        }
    }

    private fun getIndicator2(uri: Value?, sourceId: Value?): Char {
        return if (sourceId == null) {
            if (uri == null) {
                '4'
            } else {
                getSourceIndicatorFromString(uri.stringValue().substringBeforeLast("/"))
            }
        } else {
            getSourceIndicatorFromString(sourceId.stringValue())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<GeographicName> {
        val upperLevelValuesQueryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?subjectId ?marcKey ?appliesTo ?sourceId ?source
            WHERE {
                {
                    ?id             bf:subject                              ?subjectId .
                    ?subjectId      rdf:type                                ?subjectTypeId ;
                                    mads:componentList/rdf:first            ?itemId .
                    ?itemId         rdf:type                                mads:Geographic .
                    FILTER(?subjectTypeId IN (bf:Place, bf:Topic))
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
                UNION
                {
                    ?id             bf:subject      ?subjectId .
                    ?subjectId      rdf:type        bf:Place ;
                                    bflc:marcKey    ?marcKey .
                    FILTER(STRSTARTS(STR(?marcKey), "151"))
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
            }
        """.trimIndent()

        val upperLevelValuesQuery = conn.prepareTupleQuery(upperLevelValuesQueryString)
        upperLevelValuesQuery.setBinding("id", id)

        upperLevelValuesQuery.evaluate().use { result ->
            return result.map { subject ->
                if (subject.hasBinding("marcKey")) {
                    GeographicNameWithMarcKey(subject.getValue("subjectId"), subject.getValue("marcKey"), subject.getValue("sourceId"), subject.getValue("source"))
                } else {
                    val subjectId = subject.getValue("subjectId")

                    val typesAndLabelsQueryString = """
                    PREFIX bf: <$BIBFRAME_ONTOLOGY>
                    PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
                    PREFIX mads: <$LOC_MADS_RDF>
                    SELECT ?typeId ?text (COUNT(?intermediateNode)-1 as ?position)
                    WHERE {
                        ?id                 bf:subject                          ?subjectId .
                        ?subjectId          rdf:type                            ?subjectTypeId ;
                                            mads:componentList                  ?linkage .
                        ?linkage            rdf:rest*                           ?intermediateNode .
                        ?intermediateNode   rdf:rest*                           ?nodeId .
                        ?nodeId             rdf:first                           ?element .
                        ?element            rdf:type                            ?typeId ;
                                            rdfs:label|mads:authoritativeLabel  ?text .
                        FILTER(?subjectTypeId IN (bf:Place, bf:Topic))
                        FILTER(?typeId IN (<http://www.loc.gov/mads/rdf/v1#Topic>, <http://www.loc.gov/mads/rdf/v1#GenreForm>, <http://www.loc.gov/mads/rdf/v1#Temporal>, <http://www.loc.gov/mads/rdf/v1#Geographic>))                                        
                    }
                    GROUP BY ?nodeId ?typeId ?text
                    ORDER BY ?position
                """.trimIndent()

                    val typesAndLabelsQuery = conn.prepareTupleQuery(typesAndLabelsQueryString)
                    typesAndLabelsQuery.setBinding("id", id)
                    typesAndLabelsQuery.setBinding("subjectId", subjectId)

                    val typesAndLabels = typesAndLabelsQuery.evaluate().use { result ->
                        result.map { GeographicNameSubdivision(it.getValue("typeId"), it.getValue("text")) }.toList()
                    }

                    GeographicNameWithComponents(subjectId, subject.getValue("appliesTo"), subject.getValue("sourceId"), subject.getValue("source"), typesAndLabels)
                }
            }.toList()
        }
    }

    private sealed interface GeographicName {
        val uri: Value
        val sourceId: Value?
        val source: Value?
    }

    @JvmRecord
    private data class GeographicNameSubdivision(val type: Value, val label: Value)

    @JvmRecord
    private data class GeographicNameWithComponents(override val uri: Value, val appliesTo: Value?, override val sourceId: Value?, override val source: Value?, val parts: List<GeographicNameSubdivision>): GeographicName

    @JvmRecord
    private data class GeographicNameWithMarcKey(override val uri: Value, val marcKey: Value, override val sourceId: Value?, override val source: Value?): GeographicName
}