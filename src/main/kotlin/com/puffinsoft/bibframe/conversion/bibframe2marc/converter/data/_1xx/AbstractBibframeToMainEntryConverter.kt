package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._1xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.Role
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.MarcKeyUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.SingleIndicatorConfig
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.query.impl.SimpleBinding
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal abstract class AbstractBibframeToMainEntryConverter : BibframeToMarcConverter {
    // TODO : don't have examples with identifiedBy or source properties so haven't included ‡0 or ‡2
    protected fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord, vararg agentTypes: String) {
        if (containsPrimaryContribution(conn, workData.workId, *agentTypes)) {
            val datafield = if (containsMarcKey(conn, workData.workId)) {
                createDataField(queryWithMarcKey(conn, workData.workId))
            } else {
                createDataField(queryWithoutMarcKey(conn, workData.workId, *agentTypes))
            }

            record.dataFields.add(datafield)
        }
    }

    private fun queryWithMarcKey(conn: RepositoryConnection, id: Value): PrimaryContributionWithMarcKey {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?contributionId ?agentId ?marcKey
            WHERE {
                ?id             bf:contribution ?contributionId .
                ?contributionId rdf:type        bf:PrimaryContribution ;
                                bf:agent        ?agentId .
                ?agentId        rdf:type        ?agentType ;
                                bflc:marcKey    ?marcKey .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { PrimaryContributionWithMarcKey(it.getValue("agentId"), it.getValue("marcKey"), queryRoles(conn, id, it.getValue("contributionId"))) }.first()
        }
    }

    private fun queryWithoutMarcKey(conn: RepositoryConnection, id: Value, vararg agentTypes: String): PrimaryContributionWithoutMarcKey {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?contributionId ?agentId ?agentType ?contributorLabel
            WHERE {
                ?id             bf:contribution ?contributionId .
                ?contributionId rdf:type        bf:PrimaryContribution ;
                                bf:agent        ?agentId .
                ?agentId        rdf:type        ?agentType ;
                                rdfs:label      ?contributorLabel .
                FILTER(?agentType IN (${agentTypes.joinToString { "bf:$it" }}))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { PrimaryContributionWithoutMarcKey(it.getValue("agentId"), it.getValue("agentType"), it.getValue("contributorLabel"), queryRoles(conn, id, it.getValue("contributionId"))) }.first()
        }
    }

    private fun queryRoles(conn: RepositoryConnection, id: Value, contributionId: Value): List<Role> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?roleId ?role
            WHERE {
                {
                    ?id             bf:contribution ?contributionId .
                    ?contributionId rdf:type        bf:PrimaryContribution ;
                                    bf:role         ?roleId .
                    ?roleId         rdf:type        bf:Role ;
                    FILTER(ISIRI(?roleId))
                    OPTIONAL {
                        GRAPH <http://id.loc.gov/vocabulary/relators> {
                            ?roleId mads:authoritativeLabel ?role .
                        }
                    }
                }
                UNION
                {
                    ?id             bf:contribution ?contributionId .
                    ?contributionId rdf:type        bf:PrimaryContribution ;
                                    bf:role         ?roleId .
                    ?roleId         rdf:type        bf:Role ;
                                    rdfs:label      ?role .
                    FILTER(ISBLANK(?roleId))
                }
            }
        """.trimIndent()

        return queryRoles(conn, queryString, listOf(SimpleBinding("id", id), SimpleBinding("contributionId", contributionId)))
    }

    private fun containsPrimaryContribution(conn: RepositoryConnection, id: Value, vararg agentTypes: String): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK
            WHERE {
                ?id             bf:contribution ?contributionId .
                ?contributionId rdf:type        bf:PrimaryContribution ;
                                bf:agent        ?agentId .
                ?agentId        rdf:type        ?agentType .
                FILTER(?agentType IN (${agentTypes.joinToString { "bf:$it" }}))
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("id", id)

        return query.evaluate()
    }

    private fun containsMarcKey(conn: RepositoryConnection, id: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            ASK
            WHERE {
                ?id             bf:contribution ?contributionId .
                ?contributionId rdf:type        bf:PrimaryContribution ;
                                bf:agent        ?agentId .
                ?agentId        rdf:type        ?agentType ;
                                bflc:marcKey    ?marcKey .
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("id", id)

        return query.evaluate()
    }

    private fun createDataField(data: PrimaryContributionWithMarcKey): DataField {
        val builder: DataFieldBuilder = MarcKeyUtils.parseMarcKey(data.marcKey.stringValue(), SingleIndicatorConfig(SingleIndicatorConfig.IndicatorPlacement.IGNORE))
        addDataToField(builder, data.agentId, data.roles)
        return builder.build()
    }

    private fun createDataField(data: PrimaryContributionWithoutMarcKey): DataField {
        val builder = DataFieldBuilder()

        when (data.agentType.stringValue()) {
            "http://id.loc.gov/ontologies/bibframe/Person" -> {
                builder.apply {
                    tag = "100"
                    indicator1 = '1'
                }
            }
            "http://id.loc.gov/ontologies/bibframe/Family" -> {
                builder.apply {
                    tag = "100"
                    indicator1 = '3'
                }
            }
            "http://id.loc.gov/ontologies/bibframe/Organization" -> {
                builder.apply {
                    tag = "110"
                    indicator1 = '2'
                }
            }
            "http://id.loc.gov/ontologies/bibframe/Meeting" -> {
                builder.apply {
                    tag = "111"
                    indicator1 = '2'
                }
            }
        }

        addSubfieldIfExists(builder, 'a', data.contributorLabel)
        addDataToField(builder, data.agentId, data.roles)

        return builder.build()
    }

    private fun addDataToField(builder: DataFieldBuilder, agentId: Value?, roles: List<Role>) {
        addRoleSubfields(builder, roles)

        if (agentId != null) {
            if (!agentId.isBNode) {
                addSubfieldIfExists(builder, '1', agentId)
            }
        }
    }

    @JvmRecord
    private data class PrimaryContributionWithMarcKey(val agentId: Value, val marcKey: Value, val roles: List<Role>)

    @JvmRecord
    private data class PrimaryContributionWithoutMarcKey(val agentId: Value, val agentType: Value, val contributorLabel: Value, val roles: List<Role>)
}