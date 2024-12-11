package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.StatusCodeLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField088Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        for (instanceId in workData.getAllInstanceIds()) {
            query(conn, instanceId).forEach {
                val builder = DataFieldBuilder().apply { tag = "088" }
                addSubfieldIfExists(builder, if (it.cancelled) 'z' else 'a', it.reportNumber)
                record.dataFields.add(builder.build())
            }
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<ReportNumber> {
        val cancelledOrInvalidUri = lookupStatusCodeUri("cancinv")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?reportNumber ?cancelled
            WHERE {
                ?id             bf:identifiedBy ?identifiedById .
                ?identifiedById rdf:type        bf:ReportNumber ;
                                rdf:value       ?reportNumber .
                OPTIONAL {
                    ?identifiedById bf:status   ?cancelled .
                    ?cancelled      rdf:type    bf:Status .
                    FILTER(?statusId = <$cancelledOrInvalidUri>)
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { ReportNumber(it.getValue("reportNumber"), it.hasBinding("cancelled")) }.toList()
        }
    }

    @JvmRecord
    private data class ReportNumber(val reportNumber: Value, val cancelled: Boolean)
}