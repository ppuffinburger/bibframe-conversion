package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.Role
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.MarcKeyUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.LanguageScriptLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.query.impl.SimpleBinding
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal abstract class AbstractBibframeToAddedEntryNameConverter : BibframeToMarcConverter {
    // TODO : need non-marc key examples
    protected fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord, tag: String, agentTypes: List<String>) {
        val names = query(conn, workData.getAllIdsAsValues(), agentTypes).map {
            when (it) {
                is AddedEntryNameWithMarcKey -> {
                    val builder = if (it.hasAlternate()) {
                        val alternateBuilder = DataFieldBuilder().apply {
                            addSubfieldIfExists(this, '6', "880-${workData.getNextAlternateGraphicRepresentationOccurrence()}")
                        }
                        MarcKeyUtils.parseMarcKey(alternateBuilder, it.marcKey.stringValue()).apply { this.tag = tag }
                    } else {
                        MarcKeyUtils.parseMarcKey(it.marcKey.stringValue()).apply { this.tag = tag }
                    }

                    addRoleSubfields(builder, it.roles)
                    addSubfieldIfExists(builder, '1', it.uri)

                    if (it.hasAlternate()) {
                        val builder880 = MarcKeyUtils.parseMarcKey(DataFieldBuilder().apply {
                            addSubfieldIfExists(this, '6', "$tag-${workData.getNextAlternateGraphicRepresentationOccurrence()}/${it.marcKeyWithLang?.let { t -> LanguageScriptLookup.lookup(t) } ?: ""}")
                        }, it.marcKeyWithLang!!.stringValue()).apply { this.tag = "880" }

                        addRoleSubfields(builder880, it.roles)

                        if (it.uri.isIRI) {
                            addSubfieldIfExists(builder880, '1', it.uri)
                        }

                        workData.addAlternateGraphicRepresentations(builder880.build())
                    }

                    builder.build()
                }

                is AddedEntryNameWithComponents -> {
                    val builder = DataFieldBuilder().apply {
                        this.tag = tag
                        indicator1 = if (tag == "700") '1' else '2'
                    }

                    if (it.hasAlternate()) {
                        addSubfieldIfExists(builder, '6', "880-${workData.getNextAlternateGraphicRepresentationOccurrence()}")
                    }

                    addSubfieldIfExists(builder, 'a', it.agentLabel)
                    addRoleSubfields(builder, it.roles)

                    if (it.uri.isIRI) {
                        addSubfieldIfExists(builder, '1', it.uri)
                    }

                    if (it.hasAlternate()) {
                        val builder880 = DataFieldBuilder().apply {
                            this.tag = "880"
                            this.indicator1 = if (tag == "700") '1' else '2'
                        }

                        addSubfieldIfExists(builder880, '6', "$tag-${workData.getNextAlternateGraphicRepresentationOccurrence()}/${it.agentLabelWithLang?.let { t -> LanguageScriptLookup.lookup(t) } ?: ""}")

                        addSubfieldIfExists(builder, 'a', it.agentLabelWithLang)
                        addRoleSubfields(builder, it.roles)

                        if (it.uri.isIRI) {
                            addSubfieldIfExists(builder, '1', it.uri)
                        }

                        workData.addAlternateGraphicRepresentations(builder880.build())
                    }

                    builder.build()
                }
            }
        }.associateBy { df -> df.getData() }

        names.values.forEach { record.dataFields.add(it) }
    }

    private fun query(conn: RepositoryConnection, allIds: List<Value>, agentTypes: List<String>): List<AddedEntryName> {
        return allIds.map { id ->
            val upperLevelValuesQueryString = """
                PREFIX bf: <$BIBFRAME_ONTOLOGY>
                PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
                SELECT DISTINCT ?agentId ?marcKey ?marcKeyWithLang ?agentLabel ?agentLabelWithLang
                WHERE {
                    ?id                 bf:contribution     ?contributionId .
                    ?contributionId     rdf:type            bf:Contribution ;
                                        bf:agent            ?agentId .
                    ?agentId            rdf:type            ?agentType ;
                                        rdfs:label          ?agentLabel .
                    FILTER(LANG(?agentLabel) = "")
                    OPTIONAL {
                        ?agentId        bflc:marcKey        ?marcKey .
                        FILTER(LANG(?marcKey) = "")
                    }
                    OPTIONAL {
                        ?agentId            rdf:type            ?agentType ;
                                            rdfs:label          ?agentLabelWithLang .
                        FILTER(LANG(?agentLabelWithLang) != "")
                    }
                    OPTIONAL {
                        ?agentId        bflc:marcKey        ?marcKeyWithLang .
                        FILTER(LANG(?marcKeyWithLang) != "")
                    }
                    FILTER NOT EXISTS { ?contributionId rdf:type bf:PrimaryContribution . }
                    FILTER EXISTS { ?agentId rdf:type bf:Agent . }
                    FILTER(?agentType IN (${agentTypes.joinToString { "<$it>" }}))
                }
            """.trimIndent()

            val upperLevelValuesQuery = conn.prepareTupleQuery(upperLevelValuesQueryString)
            upperLevelValuesQuery.setBinding("id", id)

            upperLevelValuesQuery.evaluate().use { result ->
                result.map { agent ->
                    val agentId = agent.getValue("agentId")

                    val rolesQueryString = """
                        PREFIX bf: <$BIBFRAME_ONTOLOGY>
                        PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
                        PREFIX mads: <$LOC_MADS_RDF>
                        SELECT DISTINCT ?roleId ?role
                        WHERE {
                            {
                                ?id                 bf:contribution     ?contributionId .
                                ?contributionId     rdf:type            bf:Contribution ;
                                                    bf:agent            ?agentId ;
                                                    bf:role             ?roleId .
                                ?roleId             rdf:type            bf:Role .
                                FILTER(ISIRI(?roleId))
                                OPTIONAL {
                                    GRAPH <http://id.loc.gov/vocabulary/relators> {
                                        ?roleId mads:authoritativeLabel ?role .
                                    }
                                }
                            }
                            UNION
                            {
                                ?id                 bf:contribution     ?contributionId .
                                ?contributionId     rdf:type            bf:Contribution ;
                                                    bf:agent            ?agentId ;
                                                    bf:role             ?roleId .
                                ?roleId             rdf:type            bf:Role ;
                                                    rdfs:label          ?role .
                                FILTER(ISBLANK(?roleId))
                            }
                        }
                    """.trimIndent()

                    val roles = queryRoles(conn, rolesQueryString, listOf(SimpleBinding("id", id), SimpleBinding("agentId", agentId)))

                    if (agent.hasBinding("marcKey")) {
                        AddedEntryNameWithMarcKey(agentId, agent.getValue("marcKey"), agent.getValue("marcKeyWithLang"), roles)
                    } else {
                        AddedEntryNameWithComponents(agentId, agent.getValue("agentLabel"), agent.getValue("agentLabelWithLang"), roles)
                    }
                }.toList()
            }
        }.flatten()
    }

    private sealed interface AddedEntryName {
        val uri: Value
        val roles: List<Role>
    }

    @JvmRecord
    private data class AddedEntryNameWithComponents(override val uri: Value, val agentLabel: Value, val agentLabelWithLang: Value?, override val roles: List<Role>) : AddedEntryName {
        fun hasAlternate(): Boolean {
            return agentLabelWithLang != null
        }
    }

    @JvmRecord
    private data class AddedEntryNameWithMarcKey(override val uri: Value, val marcKey: Value, val marcKeyWithLang: Value?, override val roles: List<Role>) : AddedEntryName {
        fun hasAlternate(): Boolean {
            return marcKeyWithLang != null
        }
    }
}