package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField020Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply { tag = "020" }
            addSubfieldIfExists(builder, if (it.cancelled) 'z' else 'a', it.isbn)
            addSubfieldIfExists(builder, 'c', it.acquisitionTerms)
            addSubfieldIfExists(builder, 'q', it.qualifier)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<IsbnData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?isbn ?cancelled ?qualifier ?acquisitionTerms
            WHERE {
                {
                    ?id             bf:identifiedBy ?identifiedById .
                    ?identifiedById rdf:type        bf:Isbn .
                    ?identifiedById rdf:value       ?isbn .
                    OPTIONAL {
                        ?identifiedById bf:qualifier ?qualifier .
                    }
                    OPTIONAL {
                        ?identifiedById bf:acquisitionTerms ?acquisitionTerms .
                    }
                }
                OPTIONAL {
                    ?identifiedById bf:status  ?cancelled .
                    ?cancelled      rdf:type    bf:Status .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { IsbnData(it.getValue("isbn"), it.hasBinding("cancelled"), it.getValue("qualifier"), it.getValue("acquisitionTerms")) }.toList()
        }
    }

    @JvmRecord
    private data class IsbnData(val isbn: Value, val cancelled: Boolean, val qualifier: Value?, val acquisitionTerms: Value?)
}