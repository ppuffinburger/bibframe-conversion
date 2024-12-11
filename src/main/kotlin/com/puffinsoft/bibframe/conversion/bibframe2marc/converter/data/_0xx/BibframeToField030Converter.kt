package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField030Converter : BibframeToMarcConverter {
    // TODO : Do not have an EXAMPLE record.
    //  Do we want one for every instance?
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            record.dataFields.add(DataField("030", subfields = mutableListOf(Subfield(if (it.cancelled) 'z' else 'a', it.coden.stringValue()))))
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<CodenData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?coden ?cancelled
            WHERE {
                ?id             bf:identifiedBy ?identifiedById .
                ?identifiedById rdf:type        bf:Coden ;
                                rdf:value       ?identifier .
                OPTIONAL {
                    ?identifiedById bf:status   ?cancelled .
                    ?cancelled      rdf:type    bf:Status .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { CodenData(it.getValue("identifier"), it.hasBinding("cancelled")) }.toList()
        }
    }

    @JvmRecord
    private data class CodenData(val coden: Value, val cancelled: Boolean)
}