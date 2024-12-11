package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField027Converter : BibframeToMarcConverter {
    // TODO : Do not have an EXAMPLE record.
    //  Do we want one for every instance?
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply { tag = "027" }

            addSubfieldIfExists(builder, if (it.cancelled) 'z' else 'a', it.strn)
            addSubfieldIfExists(builder, 'q', it.qualifier)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<StandardTechnicalReportNumberData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?strn ?cancelled ?qualifier
            WHERE {
                {
                    ?instanceId     bf:identifiedBy ?identifiedById .
                    ?identifiedById rdf:type        bf:Strn ;
                                    rdf:value       ?strn .
                    OPTIONAL {
                        ?identifiedById bf:qualifier ?qualifier .
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
            return result.map { StandardTechnicalReportNumberData(it.getValue("strn"), it.hasBinding("cancelled"), it.getValue("qualifier")) }.toList()
        }
    }

    @JvmRecord
    private data class StandardTechnicalReportNumberData(val strn: Value, val cancelled: Boolean, val qualifier: Value?)
}