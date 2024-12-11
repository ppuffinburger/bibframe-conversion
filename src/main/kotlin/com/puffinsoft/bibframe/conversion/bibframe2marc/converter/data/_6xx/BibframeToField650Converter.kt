package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.MarcKeyUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField650Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        queryComponentList(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "650"
                indicator2 = getIndicator2(it.uri, it.sourceId)
            }

            addSubfieldIfExists(builder, '3', it.appliesTo)

            var wroteFirstTopic = false
            it.parts.forEach { part ->
                when (part.type.stringValue()) {
                    "http://www.loc.gov/mads/rdf/v1#Topic" -> {
                        if (wroteFirstTopic) {
                            'x'
                        } else {
                            wroteFirstTopic = true
                            'a'
                        }
                    }
                    "http://www.loc.gov/mads/rdf/v1#GenreForm" -> 'v'
                    "http://www.loc.gov/mads/rdf/v1#Temporal" -> 'y'
                    "http://www.loc.gov/mads/rdf/v1#Geographic" -> 'z'
                    else -> '9'
                }.let { name -> addSubfieldIfExists(builder, name, part.label) }
            }

            if (builder.indicator2 == '7') {
                if (it.source == null) {
                    addSubfieldIfExists(builder, '2', TextUtils.getCodeStringFromUrl(it.uri.stringValue()?.substringBeforeLast("/") ?: ""))
                } else {
                    addSourceSubfieldIfExists(builder, it.source)
                }
            }

            if (it.uri.isIRI) {
                addSubfieldIfExists(builder, '0', it.uri)
            }

            record.dataFields.add(builder.build())
        }

        queryTopicalTerm(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "650"
                indicator2 = getIndicator2(it.uri, it.sourceId)
            }

            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.term)

            if (builder.indicator2 == '7') {
                if (it.source == null) {
                    addSubfieldIfExists(builder, '2', TextUtils.getCodeStringFromUrl(it.uri?.stringValue()?.substringBeforeLast("/") ?: ""))
                } else {
                    addSourceSubfieldIfExists(builder, it.source)
                }
            }

            addSubfieldIfExists(builder, '0', it.uri)

            record.dataFields.add(builder.build())
        }

        queryTopicalTermWithMarcKey(conn, workData.workId).forEach {
            val builder = MarcKeyUtils.parseMarcKey(it.marcKey.stringValue()).apply {
                tag = "650"
                indicator2 = getIndicator2(it.uri, it.sourceId)
            }

            if (builder.indicator2 == '7') {
                if (it.source == null) {
                    addSubfieldIfExists(builder, '2', TextUtils.getCodeStringFromUrl(it.uri?.stringValue()?.substringBeforeLast("/") ?: ""))
                } else {
                    addSourceSubfieldIfExists(builder, it.source)
                }
            }

            addSubfieldIfExists(builder, '0', it.uri)

            record.dataFields.add(builder.build())
        }
    }

    private fun getIndicator2(uri: Value?, sourceId: Value?): Char {
        return if (sourceId == null) {
            if (uri == null || uri.isBNode) {
                '4'
            } else {
                getSourceIndicatorFromString(uri.stringValue().substringBeforeLast("/"))
            }
        } else {
            getSourceIndicatorFromString(sourceId.stringValue())
        }
    }

    private fun queryTopicalTermWithMarcKey(conn: RepositoryConnection, id: Value): List<TopicalTermWithMarcKey> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?marcKey ?subjectId ?sourceId ?source
            WHERE {
                ?id         bf:subject      ?subjectId .
                ?subjectId  rdf:type        bf:Topic ;
                            bflc:marcKey    ?marcKey .
                OPTIONAL {
                    ?subjectId  bf:source   ?sourceId .
                    ?sourceId   rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                FILTER NOT EXISTS { ?subjectId  mads:componentList  [] }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { TopicalTermWithMarcKey(it.getValue("marcKey"), it.getValue("subjectId"), it.getValue("sourceId"), it.getValue("source")) }
        }
    }

    private fun queryTopicalTerm(conn: RepositoryConnection, id: Value): List<TopicalTermWithTerm> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?term ?authorityId ?subjectId ?appliesTo ?sourceId ?source
            WHERE {
                {
                    ?id             bf:subject                      ?subjectId .
                    ?subjectId      rdf:type                        bf:Topic ;
                                    rdf:type                        mads:Topic ;
                                    mads:isIdentifiedByAuthority    ?authorityId .
                    ?authorityId    rdf:type                        bf:Topic ;
                                    mads:authoritativeLabel         ?term .
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
                    ?id         bf:subject                  ?subjectId .
                    ?subjectId  rdf:type                    bf:Topic ;
                                rdf:type                    mads:Topic ;
                                mads:authoritativeLabel     ?term ;
                                mads:isMemberOfMADSScheme   ?any .
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
                    ?id             bf:subject                      ?subjectId .
                    ?subjectId      rdf:type                        bf:Topic ;
                                    rdf:type                        mads:Topic ;
                                    mads:authoritativeLabel         ?term .
                    FILTER NOT EXISTS {
                        ?subjectId  mads:isMemberOfMADSScheme|mads:isIdentifiedByAuthority|mads:componentList   []
                    }
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
                    ?id         bf:subject                          ?subjectId .
                    ?subjectId  rdf:type                            bf:Topic ;
                                mads:authoritativeLabel             ?term .
                    FILTER NOT EXISTS {
                        ?subjectId  rdf:type    mads:Topic .
                    }
                    FILTER NOT EXISTS {
                        ?subjectId   bflc:marcKey   ?marcKey .
                    }
                    FILTER NOT EXISTS {
                        ?subjectId  mads:componentList  []
                    }
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
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map {
                val uri = if (it.hasBinding("authorityId") && it.getValue("authorityId").isIRI) {
                    it.getValue("authorityId")
                } else {
                    if (it.hasBinding("subjectId") && it.getValue("subjectId").isIRI) {
                        it.getValue("subjectId")
                    } else {
                        null
                    }
                }
                TopicalTermWithTerm(it.getValue("term"), uri, it.getValue("appliesTo"), it.getValue("sourceId"), if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId"))
            }.toList()
        }
    }

    private fun queryComponentList(conn: RepositoryConnection, id: Value): List<TopicalTermWithComponents> {
        val upperLevelValuesQueryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT DISTINCT ?subjectId ?appliesTo ?sourceId ?source
            WHERE {
                ?id             bf:subject                              ?subjectId .
                ?subjectId      rdf:type                                bf:Topic ;
                                mads:componentList/rdf:first            ?itemId .
                ?itemId         rdf:type                                mads:Topic .
                FILTER NOT EXISTS {
                    ?subjectId      mads:componentList      ?linkage .
                    ?linkage        (rdf:rest*/rdf:first)*  ?nodeId .
                    ?nodeId         rdf:type                ?typeId .
                    FILTER(?typeId IN (<http://www.loc.gov/mads/rdf/v1#Title>, <http://id.loc.gov/ontologies/bibframe/Hub>))
                }
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
                    SELECT ?typeId ?text
                    WHERE {
                        {
                            SELECT DISTINCT ?typeId ?text (COUNT(?intermediateNode)-1 as ?position)
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
                        }
                    }
                """.trimIndent()

                val typesAndLabelsQuery = conn.prepareTupleQuery(typesAndLabelsQueryString)
                typesAndLabelsQuery.setBinding("id", id)
                typesAndLabelsQuery.setBinding("subjectId", subjectId)

                val typesAndTexts = typesAndLabelsQuery.evaluate().use { result ->
                    result.map {
                        TopicalTermSubdivision(it.getValue("typeId"), it.getValue("text")) }.toList()
                }

                TopicalTermWithComponents(subjectId, subject.getValue("appliesTo"), subject.getValue("sourceId"), subject.getValue("source"), typesAndTexts)
            }.toList()
        }
    }

    @JvmRecord
    private data class TopicalTermWithTerm(val term: Value, val uri: Value?, val appliesTo: Value?, val sourceId: Value?, val source: Value?)

    private sealed interface TopicalTerm {
        val uri: Value
        val sourceId: Value?
        val source: Value?
    }

    @JvmRecord
    private data class TopicalTermSubdivision(val type: Value, val label: Value)

    @JvmRecord
    private data class TopicalTermWithComponents(override val uri: Value, val appliesTo: Value?, override val sourceId: Value?, override val source: Value?, val parts: List<TopicalTermSubdivision>) : TopicalTerm

    @JvmRecord
    private data class TopicalTermWithMarcKey(val marcKey: Value, override val uri: Value, override val sourceId: Value?, override val source: Value?) : TopicalTerm
}