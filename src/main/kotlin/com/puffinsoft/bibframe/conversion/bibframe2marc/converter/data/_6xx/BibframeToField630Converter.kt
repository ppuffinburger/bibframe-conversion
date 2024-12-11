package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.MarcKeyUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.SingleIndicatorConfig
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.LanguageLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField630Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        if (!containsContribution(conn, workData.workId)) {
            queryComponentList(conn, workData.workId).forEach {
                when (it) {
                    is UniformTitleWithMarcKey -> {
                        val builder = MarcKeyUtils.parseMarcKey(it.marcKey.stringValue(), SingleIndicatorConfig(SingleIndicatorConfig.IndicatorPlacement.USE_DEFAULTS, '0', '0')).apply {
                            tag = "630"
                            indicator2 = getIndicator2(it, '0')
                        }

                        if (builder.indicator2 == '7') {
                            addSubfieldIfExists(builder, '2', TextUtils.getCodeStringFromUrl(it.uri, true))
                            addSubfieldIfExists(builder, '0', it.uri)
                        }

                        record.dataFields.add(builder.build())
                    }
                    is UniformTitleWithComponents -> {
                        val builder = DataFieldBuilder().apply {
                            tag = "630"
                            indicator1 = it.nonSortNum?.stringValue()?.get(0) ?: '0'
                            indicator2 = getIndicator2(it)
                        }

                        addSubfieldIfExists(builder, 'a', it.title)
                        addSubfieldIfExists(builder, 'd', it.legalDate)
                        addSubfieldIfExists(builder, 'n', it.partNumber)
                        addSubfieldIfExists(builder, 'p', it.partName)
                        addSubfieldIfExists(builder, 'm', it.musicMedium)
                        addSubfieldIfExists(builder, 'r', it.musicKey)
                        addSubfieldIfExists(builder, 'l', it.language?.stringValue() ?: LanguageLookup.lookup(TextUtils.getCodeStringFromUrl(it.languageId))?.label)

                        if (it.arrangement) {
                            addSubfieldIfExists(builder, 'o', "arranged")
                        }

                        addSubfieldIfExists(builder, 's', it.version)
                        addSubfieldIfExists(builder, 'f', it.originDate)

                        it.components.forEach { part ->
                            when (part.type.stringValue()) {
                                "http://www.loc.gov/mads/rdf/v1#Topic" -> 'x'
                                "http://www.loc.gov/mads/rdf/v1#GenreForm" -> 'v'
                                "http://www.loc.gov/mads/rdf/v1#Temporal" -> 'y'
                                "http://www.loc.gov/mads/rdf/v1#Geographic" -> 'z'
                                else -> '9'
                            }.let { name -> addSubfieldIfExists(builder, name, part.label) }
                        }

                        if (builder.indicator2 == '7') {
                            if (it.source == null) {
                                addSubfieldIfExists(builder, '2', TextUtils.getCodeStringFromUrl(it.uri, true))
                            } else {
                                addSourceSubfieldIfExists(builder, it.source)
                            }
                        }

                        if (it.uri.isIRI) {
                            addSubfieldIfExists(builder, '0', it.uri)
                        }

                        if (it.role == null) {
                            addSubfieldIfExists(builder, '4', TextUtils.getCodeStringFromUrl(it.roleId))
                        } else {
                            addSubfieldIfExists(builder, '4', it.role)
                        }

                        record.dataFields.add(builder.build())
                    }
                }
            }
        } else {
            querySimpleSubjectsWithMarcKey(conn, workData.workId).forEach {
                val builder = MarcKeyUtils.parseMarcKey(it.marcKey.stringValue(), SingleIndicatorConfig(SingleIndicatorConfig.IndicatorPlacement.USE_DEFAULTS, '0', '0')).apply {
                    tag = "630"
                    indicator2 = getIndicator2(it, '0')
                }

                if (builder.indicator2 == '7') {
                    addSubfieldIfExists(builder, '2', TextUtils.getCodeStringFromUrl(it.uri, true))
                }

                addSubfieldIfExists(builder, '0', it.uri)

                record.dataFields.add(builder.build())
            }
        }
    }

    private fun containsContribution(conn: RepositoryConnection, id: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK
            WHERE {
                {
                    ?id             bf:expressionOf ?expressionOfId .
                    ?expressionOfId rdf:type        bf:Hub ;
                                    bf:contribution ?contributionId .
                    ?contributionId rdf:type        bf:Contribution .
                }
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("id", id)

        return query.evaluate()
    }

    private fun getIndicator2(uniformTitle: UniformTitle, defaultIndicator: Char = '4'): Char {
        return if (uniformTitle.sourceId == null) {
            if (uniformTitle.hasValidUri()) {
                getSourceIndicatorFromString(uniformTitle.uri.stringValue().substringBeforeLast("/"))
            } else {
                defaultIndicator
            }
        } else {
            getSourceIndicatorFromString(uniformTitle.sourceId!!.stringValue())
        }
    }

    private fun queryComponentList(conn: RepositoryConnection, id: Value): List<UniformTitle> {
        val upperLevelValuesQueryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?subjectId ?marcKey ?title ?partNumber ?partName ?nonSortNum ?legalDate ?musicMedium ?musicKey ?languageId ?language ?arrangement ?version ?originDate ?sourceId ?source ?roleId ?role
            WHERE {
                {
                    SELECT ?subjectId ?title ?partNumber ?partName ?nonSortNum ?legalDate ?musicMedium ?musicKey ?languageId ?language ?arrangement ?version ?originDate ?sourceId ?source ?roleId ?role
                    WHERE {
                        {
                            ?id             bf:subject                          ?subjectId .
                            ?subjectId      rdf:type                            bf:Topic ;
                                            rdf:type                            mads:ComplexSubject ;
                                            mads:componentList                  ?linkage .
                            ?linkage        rdf:first                           ?nodeId .
                            ?nodeId         rdf:type                            bf:Hub ;
                                            bf:title                            ?titleId .
                            ?titleId        rdf:type                            bf:Title ;
                                            bf:mainTitle                        ?title .
                            OPTIONAL {
                                ?titleId    bf:partNumber   ?partNumber .
                            }
                            OPTIONAL {
                                ?titleId    bf:partName     ?partName .
                            }
                            FILTER NOT EXISTS {
                                ?nodeId     bflc:marcKey    ?marcKey .
                            }
                        }
                        OPTIONAL {
                            ?subjectId      bflc:nonSortNum ?nonSortNum .
                        }
                        OPTIONAL {
                            ?subjectId      bf:legalDate    ?legalDate .
                        }
                        OPTIONAL {
                            ?subjectId      bf:musicMedium  ?musicMediumId .
                            ?musicMediumId  rdf:type        bf:MusicMedium ;
                                            rdfs:label      ?musicMedium .
                        }
                        OPTIONAL {
                            ?subjectId      bf:musicKey     ?musicKey .
                        }
                        OPTIONAL {
                            ?subjectId      bf:language     ?languageId .
                            ?languageId     rdf:type        bf:Language ;
                            OPTIONAL {
                                ?languageId rdfs:label      ?language .
                            }
                        }
                        OPTIONAL {
                            ?subjectId      rdf:type        ?arrangement .
                            FILTER(?arrangement = bf:Arrangement)
                        }
                        OPTIONAL {
                            ?subjectId      bf:version      ?version .
                        }
                        OPTIONAL {
                            ?subjectId      bf:originDate   ?originDate .
                        }
                        OPTIONAL {
                            ?subjectId  bf:source   ?sourceId .
                            ?sourceId   rdf:type    bf:Source .
                            OPTIONAL {
                                ?sourceId   bf:code ?source .
                            }
                        }
                        OPTIONAL {
                            ?subjectId  bf:role     ?roleId .
                            ?roleId     rdf:type    bf:Role .
                            OPTIONAL {
                                ?roleId   bf:code ?role .
                            }
                        }
                    }
                }
                UNION
                {
                    SELECT DISTINCT ?subjectId ?marcKey ?sourceId ?source
                    WHERE {
                        {
                            ?id             bf:subject                              ?subjectId .
                            ?subjectId      rdf:type                                bf:Topic ;
                                            rdf:type                                mads:ComplexSubject ;
                                            mads:componentList/rdf:rest*/rdf:first  ?itemId .
                            ?itemId         bflc:marcKey                            ?marcKey .
                            FILTER(STRSTARTS(STR(?marcKey), "630"))
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
                            ?id         bf:subject      ?subjectId .
                            ?subjectId  rdf:type        bf:Topic ;
                                        bflc:marcKey    ?marcKey .
                            FILTER(STRSTARTS(STR(?marcKey), "130"))
                            OPTIONAL {
                                ?subjectId  bf:source   ?sourceId .
                                ?sourceId   rdf:type    bf:Source .
                                OPTIONAL {
                                    ?sourceId   bf:code ?source .
                                }
                            }
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
                    UniformTitleWithMarcKey(
                        subject.getValue("marcKey"),
                        subject.getValue("subjectId"),
                        subject.getValue("sourceId"),
                        subject.getValue("source")
                    )
                } else {
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

                    val typesAndTexts = typesAndLabelsQuery.evaluate().use { result ->
                        result.map { UniformTitleSubdivision(it.getValue("typeId"), it.getValue("text")) }.toList()
                    }

                    UniformTitleWithComponents(
                        subjectId,
                        subject.getValue("title"),
                        subject.getValue("partNumber"),
                        subject.getValue("partName"),
                        subject.getValue("nonSortNum"),
                        subject.getValue("legalDate"),
                        subject.getValue("musicMedium"),
                        subject.getValue("musicKey"),
                        subject.getValue("languageId"),
                        subject.getValue("language"),
                        subject.hasBinding("arrangement"),
                        subject.getValue("version"),
                        subject.getValue("originDate"),
                        subject.getValue("sourceId"),
                        subject.getValue("source"),
                        subject.getValue("roleId"),
                        subject.getValue("role"),
                        typesAndTexts
                    )
                }
            }.toList()
        }
    }

    private fun querySimpleSubjectsWithMarcKey(conn: RepositoryConnection, id: Value): List<UniformTitleWithMarcKey> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?subjectId ?marcKey ?sourceId ?source
            WHERE {
                ?id         bf:subject      ?subjectId .
                ?subjectId  rdf:type        bf:Topic ;
                            bflc:marcKey    ?marcKey .
                FILTER(STRSTARTS(STR(?marcKey), "130"))
                OPTIONAL {
                    ?subjectId  bf:source   ?sourceId .
                    ?sourceId   rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { subject ->
                UniformTitleWithMarcKey(
                    subject.getValue("marcKey"),
                    subject.getValue("subjectId"),
                    subject.getValue("sourceId"),
                    subject.getValue("source")
                )
            }.toList()
        }
    }

    private sealed interface UniformTitle {
        val uri: Value
        val sourceId: Value?
        val source: Value?

        fun hasValidUri(): Boolean {
            return uri.isIRI && !uri.stringValue().startsWith("http://id.loc.gov/resources/hubs/")
        }
    }

    @JvmRecord
    private data class UniformTitleSubdivision(val type: Value, val label: Value)

    @JvmRecord
    private data class UniformTitleWithComponents(
        override val uri: Value,
        val title: Value,
        val partNumber: Value?,
        val partName: Value?,
        val nonSortNum: Value?,
        val legalDate: Value?,
        val musicMedium: Value?,
        val musicKey: Value?,
        val languageId: Value?,
        val language: Value?,
        val arrangement: Boolean,
        val version: Value?,
        val originDate: Value?,
        override val sourceId: Value?,
        override val source: Value?,
        val roleId: Value?,
        val role: Value?,
        val components: List<UniformTitleSubdivision>
    ) : UniformTitle

    @JvmRecord
    private data class UniformTitleWithMarcKey(val marcKey: Value, override val uri: Value, override val sourceId: Value?, override val source: Value?) : UniformTitle
}