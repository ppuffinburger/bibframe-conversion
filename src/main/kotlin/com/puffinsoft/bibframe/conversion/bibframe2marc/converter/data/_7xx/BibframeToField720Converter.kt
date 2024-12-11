package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.Role
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.query.impl.SimpleBinding
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField720Converter : BibframeToMarcConverter {
    // TODO : need examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "720"
                indicator1 = if (it.isPerson) '2' else ' '
            }

            addSubfieldIfExists(builder, 'a', it.name)
            addRoleSubfields(builder, it.roles)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<UncontrolledName> {
        val upperLevelValuesQueryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?agentId ?agentType ?agentLabel
            WHERE {
                ?id                 bf:contribution     ?contributionId .
                ?contributionId     rdf:type            bf:Contribution ;
                                    bf:agent            ?agentId .
                ?agentId            rdf:type            ?agentType ;
                                    rdfs:label          ?agentLabel .
                FILTER NOT EXISTS { ?contributionId rdf:type bf:PrimaryContribution . }
                FILTER EXISTS { ?agentId rdf:type bf:Agent . ?agentId rdf:type bflc:Uncontrolled . }
                FILTER(?agentType IN (<http://id.loc.gov/ontologies/bibframe/Person>, <http://id.loc.gov/ontologies/bibframe/Family>, <http://id.loc.gov/ontologies/bibframe/Organization>, <http://id.loc.gov/ontologies/bibframe/Meeting>))
            }
        """.trimIndent()

        val upperLevelValuesQuery = conn.prepareTupleQuery(upperLevelValuesQueryString)
        upperLevelValuesQuery.setBinding("id", id)

        upperLevelValuesQuery.evaluate().use { result ->
            return result.map { agent ->
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
                            ?roleId             rdf:type            bf:Role ;
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

                UncontrolledName(agent.getValue("agentLabel"), agent.getValue("agentType").stringValue() == "http://id.loc.gov/ontologies/bibframe/Person", roles)
            }.toList()
        }
    }

    @JvmRecord
    private data class UncontrolledName(val name: Value, val isPerson: Boolean, val roles: List<Role>)
}