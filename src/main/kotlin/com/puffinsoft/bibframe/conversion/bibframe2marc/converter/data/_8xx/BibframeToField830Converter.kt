package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._8xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.MarcKeyUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.SingleIndicatorConfig
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField830Converter : BibframeToMarcConverter {
    // TODO : not sure about most of the subfields.  Not enough examples to show where they are in the graph.  Role is also a $4 when code puts in the relationshipId.  Also has Item fields.
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            when(it) {
                is SeriesUniformTitleWithMarcKey -> {
                    val builder = MarcKeyUtils.parseMarcKey(it.marcKey.stringValue(), SingleIndicatorConfig(SingleIndicatorConfig.IndicatorPlacement.USE_DEFAULTS, ' ')).apply { tag = "830" }

                    addSubfieldIfExists(builder, '1', it.uri)

                    record.dataFields.add(builder.build())
                }
                is SeriesUniformTitleWithComponents -> {
                    val builder = DataFieldBuilder().apply {
                        tag = "830"
                        indicator2 = it.nonSortNum?.stringValue()?.get(0) ?: '0'
                    }

                    addSubfieldIfExists(builder, 'a', it.title)
                    addSubfieldIfExists(builder, 'n', it.partNumber)
                    addSubfieldIfExists(builder, 'p', it.partName)
                    addSubfieldIfExists(builder, 'v', it.seriesEnumeration)

                    addSourceSubfieldIfExists(builder, it.source ?: it.sourceId)

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
    }

    //<http://id.loc.gov/resources/works/12124799> <http://id.loc.gov/ontologies/bibframe/relation> _:b230iddOtlocdOtgovresourcesworks12124799 .
    //_:b230iddOtlocdOtgovresourcesworks12124799 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://id.loc.gov/ontologies/bibframe/Relation> .
    //_:b230iddOtlocdOtgovresourcesworks12124799 <http://id.loc.gov/ontologies/bibframe/relationship> <http://id.loc.gov/ontologies/bibframe/hasSeries> .
    //_:b230iddOtlocdOtgovresourcesworks12124799 <http://id.loc.gov/ontologies/bibframe/seriesEnumeration> "00-19." .
    //_:b230iddOtlocdOtgovresourcesworks12124799 <http://id.loc.gov/ontologies/bibframe/associatedResource> <http://id.loc.gov/resources/hubs/bed84f9e-4ec8-6209-12f9-35fdbbdd16af> .
    //<http://id.loc.gov/resources/hubs/bed84f9e-4ec8-6209-12f9-35fdbbdd16af> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://id.loc.gov/ontologies/bibframe/Hub> .
    //<http://id.loc.gov/resources/hubs/bed84f9e-4ec8-6209-12f9-35fdbbdd16af> <http://www.w3.org/2000/01/rdf-schema#label> "U.S. Geological Survey open-file report" .
    //<http://id.loc.gov/resources/hubs/bed84f9e-4ec8-6209-12f9-35fdbbdd16af> <http://id.loc.gov/ontologies/bibframe/title> _:b239iddOtlocdOtgovresourcesworks12124799 .
    //_:b239iddOtlocdOtgovresourcesworks12124799 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://id.loc.gov/ontologies/bibframe/Title> .
    //_:b239iddOtlocdOtgovresourcesworks12124799 <http://id.loc.gov/ontologies/bibframe/mainTitle> "U.S. Geological Survey open-file report" .
    //<http://id.loc.gov/resources/hubs/bed84f9e-4ec8-6209-12f9-35fdbbdd16af> <http://id.loc.gov/ontologies/bflc/marcKey> "1300 $aU.S. Geological Survey open-file report," .
    private fun query(conn: RepositoryConnection, id: Value): List<SeriesUniformTitle> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?relationId ?hubId ?marcKey ?title ?partNumber ?partName ?nonSortNum ?seriesEnumeration ?sourceId ?source ?roleId ?role
            WHERE {
                {
                    {
                        ?id                 bf:relation             ?relationId .
                        ?relationId         rdf:type                bf:Relation ;
                                            bf:relationship         bf:hasSeries ;
                                            bf:associatedResource   ?hubId .
                        ?hubId              rdf:type                bf:Hub ;
                                            bf:title                ?titleId .
                        ?titleId            rdf:type                bf:Title ;
                                            bf:mainTitle            ?title .
                        OPTIONAL {
                            ?titleId    bf:partNumber   ?partNumber .
                        }
                        OPTIONAL {
                            ?titleId    bf:partName     ?partName .
                        }
                        FILTER NOT EXISTS {
                            ?hubId          bf:contribution ?contributionId .
                            ?contributionId rdf:type        bf:PrimaryContribution .
                        }
                    }
                    OPTIONAL {
                        ?relationId     bflc:nonSortNum ?nonSortNum .
                    }
                    OPTIONAL {
                        ?relationId     bf:seriesEnumeration    ?seriesEnumeration .
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
                    ?id                 bf:relation             ?relationId .
                    ?relationId         rdf:type                bf:Relation ;
                                        bf:relationship         bf:Series ;
                                        bf:associatedResource   ?hubId .
                    ?hubId              rdf:type                bf:Hub ;
                                        bflc:marcKey            ?marcKey .
                    FILTER REGEX(?marcKey, "^130")
                }
            }
        """.trimIndent()

        val upperLevelValuesQuery = conn.prepareTupleQuery(queryString)
        upperLevelValuesQuery.setBinding("id", id)

        upperLevelValuesQuery.evaluate().use { result ->
            return result.map { subject ->
                if (subject.hasBinding("marcKey")) {
                    SeriesUniformTitleWithMarcKey(subject.getValue("hubId"), subject.getValue("marcKey"))
                } else {
                    SeriesUniformTitleWithComponents(
                        subject.getValue("hubId"),
                        subject.getValue("title"),
                        subject.getValue("partNumber"),
                        subject.getValue("partName"),
                        subject.getValue("nonSortNum"),
                        subject.getValue("seriesEnumeration"),
                        subject.getValue("sourceId"),
                        subject.getValue("source"),
                        subject.getValue("roleId"),
                        subject.getValue("role")
                    )
                }
            }.toList()
        }
    }

    private sealed interface SeriesUniformTitle

    @JvmRecord
    private data class SeriesUniformTitleWithMarcKey(val uri: Value, val marcKey: Value) : SeriesUniformTitle

    @JvmRecord
    private data class SeriesUniformTitleWithComponents(
        val uri: Value,
        val title: Value,
        val partNumber: Value?,
        val partName: Value?,
        val nonSortNum: Value?,
        val seriesEnumeration: Value?,
        val sourceId: Value?,
        val source: Value?,
        val roleId: Value?,
        val role: Value?
    ) : SeriesUniformTitle
}