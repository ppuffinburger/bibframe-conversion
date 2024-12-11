package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField017Converter :BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId)?.let { data ->
            val builder = DataFieldBuilder().apply { tag = "017" }
            data.copyrights[false]?.forEach { addSubfieldIfExists(builder, 'a', it) }
            data.copyrights[true]?.forEach { addSubfieldIfExists(builder, 'z', it) }

            addSubfieldIfExists(builder, 'b', data.assigner)
            addSubfieldIfExists(builder, 'd', data.date)
            addSubfieldIfExists(builder, 'i', data.displayText)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): CopyrightData? {
        val mainQueryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?copyright ?cancelled
            WHERE {
                {
                    ?id             bf:identifiedBy ?identifiedById .
                    ?identifiedById rdf:type        bf:CopyrightNumber ;
                                    rdf:value       ?copyright .
                }
                OPTIONAL {
                    ?identifiedById bf:status  ?cancelled .
                    ?cancelled      rdf:type    bf:Status .
                }
            }""".trimIndent()

        val mainQuery = conn.prepareTupleQuery(mainQueryString)
        mainQuery.setBinding("id", id)

        mainQuery.evaluate().use { mainResult ->
            val copyrights = mainResult.filter { it.hasBinding("copyright") }.groupBy({ it.hasBinding("cancelled") }, { it.getBinding("copyright").value })

            if (copyrights.isEmpty()) {
                return null
            }

            val miscQueryString = """
                PREFIX bf: <$BIBFRAME_ONTOLOGY>
                SELECT ?assigner ?date ?displayText
                WHERE {
                    OPTIONAL {
                        ?id             bf:identifiedBy ?identifiedById .
                        ?identifiedById bf:assigner     ?agentId .
                        ?agentId        rdf:type        bf:Agent ;
                                        rdf:label       ?assigner .
                    }
                    OPTIONAL {
                        ?instanceId     bf:identifiedBy ?identifiedById .
                        ?identifiedById bf:date         ?date .
                    }
                    OPTIONAL {
                        ?instanceId     bf:identifiedBy ?identifiedById .
                        ?identifiedById bf:note         ?noteId .
                        ?noteId         rdf:type        bf:Note ;
                                        rdfs:label      ?displayText .
                    }
                }""".trimIndent()

            val miscQuery = conn.prepareTupleQuery(miscQueryString)
            miscQuery.setBinding("id", id)

            miscQuery.evaluate().use { miscResult ->
                return miscResult.map { CopyrightData(copyrights, it.getValue("assigner"), it.getValue("date"), it.getValue("displayText")) }.firstOrNull()
            }
        }
    }

    @JvmRecord
    private data class CopyrightData(val copyrights: Map<Boolean, List<Value>>, val assigner: Value?, val date: Value?, val displayText: Value?)
}