package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField036Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId)?.let {
            val builder = DataFieldBuilder().apply { tag = "036" }
            addSubfieldIfExists(builder, 'a', it.sn)
            addSubfieldIfExists(builder, 'b', it.assigner)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): OriginalStudyNumber? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?sn ?assigner
            WHERE {
                ?id             bf:identifiedBy ?identifiedById .
                ?identifiedById rdf:type        bf:StudyNumber ;
                                rdf:value       ?sn .
                OPTIONAL {
                    ?identifiedById bf:assigner ?assigner .
                    ?assigner       rdf:type    bf:Agent ;
                                    rdfs:label  ?assigner .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { OriginalStudyNumber(it.getValue("sn"), it.getValue("assigner")) }.firstOrNull()
        }
    }

    @JvmRecord
    private data class OriginalStudyNumber(val sn: Value, val assigner: Value?)
}