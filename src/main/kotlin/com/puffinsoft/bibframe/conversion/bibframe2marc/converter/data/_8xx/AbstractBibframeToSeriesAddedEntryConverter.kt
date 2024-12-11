package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._8xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.Role
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.MarcKeyUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.query.impl.SimpleBinding
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal abstract class AbstractBibframeToSeriesAddedEntryConverter : BibframeToMarcConverter {
    // TODO : need non-marc key examples
    protected fun convert(conn: RepositoryConnection, id: Value, record: BibliographicRecord, tag: String, agentTypes: List<String>) {
        val names = query(conn, id, agentTypes).map {
            when(it) {
                is SeriesAddedEntryNameWithMarcKey -> {
                    val builder = MarcKeyUtils.parseMarcKey(it.marcKey.stringValue()).apply { this.tag = tag }

                    addSubfieldIfExists(builder, 'v', it.seriesEnumeration)
                    addRoleSubfields(builder, it.roles)

                    addSubfieldIfExists(builder, '1', it.uri)

                    builder.build()
                }
                is SeriesAddedEntryNameWithComponents -> {
                    // TODO : need examples to finish
                    val builder = DataFieldBuilder().apply {
                        this.tag = tag
                        indicator1 = if (tag == "800") '1' else '2'
                    }

                    addSubfieldIfExists(builder, 'a', it.agentLabel)
                    addRoleSubfields(builder, it.roles)

                    if (it.uri.isIRI) {
                        addSubfieldIfExists(builder, '1', it.uri)
                    }

                    builder.build()
                }
            }
        }.associateBy { df -> df.getData() }

        names.values.forEach { record.dataFields.add(it) }
    }

    private fun query(conn: RepositoryConnection, id: Value, agentTypes: List<String>): List<SeriesAddedEntryName> {
        val upperLevelValuesQueryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?relationId ?hubMarcKey ?seriesEnumeration ?hubId
            WHERE {
                {
                    ?id                 bf:relation             ?relationId .
                    ?relationId         rdf:type                bf:Relation ;
                                        bf:relationship         bf:hasSeries ;
                                        bf:associatedResource   ?hubId .
                    ?hubId              rdf:type                bf:Hub ;
                                        bf:contribution         ?contributionId .
                    ?contributionId     bf:agent                ?agentId .
                    ?agentId            rdf:type                ?agentType .
                    FILTER EXISTS { ?agentId rdf:type bf:Agent . }
                    FILTER(?agentType IN (${agentTypes.joinToString { "<$it>" }}))
                    ?hubId              bflc:marcKey            ?hubMarcKey .
                    FILTER REGEX(?hubMarcKey, "^(100|110|111)")
                    OPTIONAL {
                        ?relationId     bf:seriesEnumeration    ?seriesEnumeration .
                    }
                }
            }
        """.trimIndent()

        val upperLevelValuesQuery = conn.prepareTupleQuery(upperLevelValuesQueryString)
        upperLevelValuesQuery.setBinding("id", id)

        upperLevelValuesQuery.evaluate().use { result ->
            return result.map { relation ->
                val relationId = relation.getValue("relationId")

                val rolesQueryString = """
                    PREFIX bf: <$BIBFRAME_ONTOLOGY>
                    PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
                    PREFIX mads: <$LOC_MADS_RDF>
                    SELECT DISTINCT ?roleId ?role
                    WHERE {
                        {
                            ?id                 bf:relation             ?relationId .
                            ?relationId         rdf:type                bf:Relation ;
                                                bf:relationship         bf:hasSeries ;
                                                bf:associatedResource   ?hubId .
                            ?hubId              rdf:type                bf:Hub ;
                                                bf:contribution         ?contributionId .
                            ?contributionId     bf:agent                ?agentId ;
                                                bf:role                 ?roleId .
                            ?roleId             rdf:type                bf:Role ;
                            FILTER(ISIRI(?roleId))
                            OPTIONAL {
                                GRAPH <http://id.loc.gov/vocabulary/relators> {
                                    ?roleId mads:authoritativeLabel ?role .
                                }
                            }
                        }
                        UNION
                        {
                            ?id                 bf:relation             ?relationId .
                            ?relationId         rdf:type                bf:Relation ;
                                                bf:relationship         bf:hasSeries ;
                                                bf:associatedResource   ?hubId .
                            ?hubId              rdf:type                bf:Hub ;
                                                bf:contribution         ?contributionId .
                            ?contributionId     bf:agent                ?agentId ;
                                                bf:role                 ?roleId .
                            ?roleId             rdf:type                bf:Role ;
                                                rdfs:label              ?role .
                            FILTER(ISBLANK(?roleId))
                        }
                    }
                """.trimIndent()

                val roles = queryRoles(conn, rolesQueryString, listOf(SimpleBinding("id", id), SimpleBinding("relationId", relationId)))

                if (relation.hasBinding("hubMarcKey")) {
                    SeriesAddedEntryNameWithMarcKey(relation.getValue("hubId"), relation.getValue("hubMarcKey"), relation.getValue("seriesEnumeration"), listOf())
                } else {
                    SeriesAddedEntryNameWithComponents(relation.getValue("hubId"), relation.getValue("agentLabel"), relation.getValue("seriesEnumeration"), roles)
                }
            }.toList()
        }
    }

    private sealed interface SeriesAddedEntryName {
        val uri: Value
        val seriesEnumeration: Value?
        val roles: List<Role>
    }

    @JvmRecord
    private data class SeriesAddedEntryNameWithComponents(override val uri: Value, val agentLabel: Value, override val seriesEnumeration: Value?, override val roles: List<Role>) : SeriesAddedEntryName

    @JvmRecord
    private data class SeriesAddedEntryNameWithMarcKey(override val uri: Value, val marcKey: Value, override val seriesEnumeration: Value?, override val roles: List<Role>) : SeriesAddedEntryName
}