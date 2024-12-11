package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx

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

internal class BibframeToField730Converter : BibframeToMarcConverter {
    // TODO : not sure about most of the subfields.  Not enough examples to show where they are in the graph.  Role is also a $4 when code puts in the relationshipId.  Also has Item fields.
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            when(it) {
                is UniformTitleWithMarcKey -> {
                    val builder = MarcKeyUtils.parseMarcKey(it.marcKey.stringValue(), SingleIndicatorConfig(SingleIndicatorConfig.IndicatorPlacement.USE_DEFAULTS, '0')).apply { tag = "730" }

                    addSubfieldIfExists(builder, '1', it.uri)

                    record.dataFields.add(builder.build())
                }
                is UniformTitleWithComponents -> {
                    val builder = DataFieldBuilder().apply {
                        tag = "730"
                        indicator1 = it.nonSortNum?.stringValue()?.get(0) ?: '0'
                        indicator2 = '2'
                    }

                    addSubfieldIfExists(builder, 'i', it.relationship)
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

                    addSourceSubfieldIfExists(builder, it.source ?: it.sourceId)

                    if (it.uri.isIRI) {
                        addSubfieldIfExists(builder, '0', it.uri)
                    }

                    if (it.role == null) {
                        addSubfieldIfExists(builder, '4', TextUtils.getCodeStringFromUrl(it.roleId))
                    } else {
                        addSubfieldIfExists(builder, '4', it.role)
                    }

                    addSubfieldIfExists(builder, '4', it.relationshipId)
                    addSubfieldIfExists(builder, '1', it.associatedResourceId)

                    record.dataFields.add(builder.build())
                }
            }
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<UniformTitle> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?relationId ?marcKey ?relationshipId ?relationship ?hubId ?title ?partNumber ?partName ?nonSortNum ?legalDate ?musicMedium ?musicKey ?languageId ?language ?arrangement ?version ?originDate ?sourceId ?source ?roleId ?role
            WHERE {
                {
                    {
                        ?id                 bf:relation             ?relationId .
                        ?relationId         rdf:type                bf:Relation ;
                                            bf:relationship         ?relationshipId .
                        ?relationshipId     rdfs:label              ?relationship .
                        ?relationId         bf:associatedResource   ?hubId .
                        ?hubId              rdf:type                bf:Hub ;
                                            bf:title                ?titleId .
                        ?titleId            rdf:type                bf:Title ;
                                            bf:mainTitle            ?title .
                        FILTER NOT EXISTS {
                            ?hubId bf:contribution ?contributionId .
                            ?contributionId rdf:type bf:Contribution .
                        }
                        OPTIONAL {
                            ?titleId    bf:partNumber   ?partNumber .
                        }
                        OPTIONAL {
                            ?titleId    bf:partName     ?partName .
                        }
                    }
                    OPTIONAL {
                        ?relationId     bflc:nonSortNum ?nonSortNum .
                    }
                    OPTIONAL {
                        ?relationId     bf:legalDate    ?legalDate .
                    }
                    OPTIONAL {
                        ?relationId     bf:musicMedium  ?musicMediumId .
                        ?musicMediumId  rdf:type        bf:MusicMedium ;
                                        rdfs:label      ?musicMedium .
                    }
                    OPTIONAL {
                        ?relationId     bf:musicKey     ?musicKey .
                    }
                    OPTIONAL {
                        ?relationId     bf:language     ?languageId .
                        ?languageId     rdf:type        bf:Language ;
                        OPTIONAL {
                            ?languageId rdfs:label      ?language .
                        }
                    }
                    OPTIONAL {
                        ?relationId     rdf:type        ?arrangement .
                        FILTER(?arrangement = bf:Arrangement)
                    }
                    OPTIONAL {
                        ?relationId     bf:version      ?version .
                    }
                    OPTIONAL {
                        ?relationId     bf:originDate   ?originDate .
                    }
                    OPTIONAL {
                        ?relationId bf:source   ?sourceId .
                        ?sourceId   rdf:type    bf:Source .
                        OPTIONAL {
                            ?sourceId   bf:code ?source .
                        }
                    }
                    OPTIONAL {
                        ?relationId bf:role     ?roleId .
                        ?roleId     rdf:type    bf:Role .
                        OPTIONAL {
                            ?roleId   bf:code ?role .
                        }
                    }
                }
                UNION
                {
                    ?id             bf:relatedTo    ?relationId .
                    ?relationId     rdf:type        bf:Hub ;
                                    bflc:marcKey    ?marcKey .
                    FILTER NOT EXISTS {
                        ?relationId     bf:contribution ?contributionId .
                        ?contributionId rdf:type        bf:Contribution .
                    }
                }
            }
        """.trimIndent()

        val upperLevelValuesQuery = conn.prepareTupleQuery(queryString)
        upperLevelValuesQuery.setBinding("id", id)

        upperLevelValuesQuery.evaluate().use { result ->
            return result.map { subject ->
                if (subject.hasBinding("marcKey")) {
                    UniformTitleWithMarcKey(subject.getValue("relationId"), subject.getValue("marcKey"))
                } else {
                    UniformTitleWithComponents(
                        subject.getValue("relationId"),
                        subject.getValue("relationshipId"),
                        subject.getValue("relationship"),
                        subject.getValue("hubId"),
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
                        subject.getValue("role")
                    )
                }
            }.toList()
        }
    }

    private sealed interface UniformTitle

    @JvmRecord
    private data class UniformTitleWithMarcKey(val uri: Value, val marcKey: Value) : UniformTitle

    @JvmRecord
    private data class UniformTitleWithComponents(
        val uri: Value,
        val relationshipId: Value,
        val relationship: Value,
        val associatedResourceId: Value,
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
        val sourceId: Value?,
        val source: Value?,
        val roleId: Value?,
        val role: Value?
    ) : UniformTitle
}