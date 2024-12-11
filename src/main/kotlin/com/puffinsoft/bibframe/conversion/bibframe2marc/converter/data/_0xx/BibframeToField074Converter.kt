package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField074Converter : BibframeToMarcConverter {
    // TODO : no example
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        for (instanceId in workData.getAllInstanceIds()) {
            query(conn, instanceId).forEach {
                val builder = DataFieldBuilder().apply { tag = "074" }
                addSubfieldIfExists(builder, if (it.cancelled) 'z' else 'a', it.identifier)
                record.dataFields.add(builder.build())
            }
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<GpoItemNumber> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?identifier ?cancelled
            WHERE {
                ?id             bf:identifiedBy ?identifiedById .
                ?identifiedById rdf:type        bf:Identifier ;
                                rdf:value       ?identifier ;
                                bf:assigner     ?assigner .
                OPTIONAL {
                    ?identifiedById bf:status   ?cancelled .
                    ?cancelled      rdf:type    bf:Status .
                }
                FILTER(?assigner = <http://id.loc.gov/vocabulary/organizations/dgpo>)
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("identifier") }.map { GpoItemNumber(it.getValue("identifier"), it.hasBinding("cancelled")) }.toList()
        }
    }

    @JvmRecord
    private data class GpoItemNumber(val identifier: Value, val cancelled: Boolean)
}