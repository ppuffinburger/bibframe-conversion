package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField010Converter : BibframeToMarcConverter {
    // TODO : do we do a 010 for every Instance?
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        val subfields: List<Subfield> = query(conn, workData.primaryInstanceId).map { data ->
                val lccnValue = data.lccn.stringValue().trim()
                val lccn = if (lccnValue.length == 10) {
                    lccnValue.padStart(12, ' ')
                } else if (lccnValue.length < 10) {
                    lccnValue.padStart(11, ' ') + ' '
                } else {
                    lccnValue
                }
                Subfield(if (data.cancelled) 'z' else 'a', lccn)
            }
            .toList()

        if (subfields.isNotEmpty()) {
            record.dataFields.add(DataField("010", subfields = subfields.toMutableList()))
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<LccnData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?lccn ?cancelled
            WHERE {
                {
                    ?id             bf:identifiedBy ?identifiedById .
                    ?identifiedById rdf:type        bf:Lccn ;
                                    rdf:value       ?lccn .
                }
                OPTIONAL {
                    ?identifiedById bf:status  ?cancelled .
                    ?cancelled      rdf:type    bf:Status .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { LccnData(it.getValue("lccn"), it.hasBinding("cancelled")) }.toList()
        }
    }

    @JvmRecord
    private data class LccnData(val lccn: Value, val cancelled: Boolean)
}