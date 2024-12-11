package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField025Converter : BibframeToMarcConverter {
    //  TODO : Do we want one for every instance?
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId)?.let {
            record.dataFields.add(DataField("025", subfields = mutableListOf(Subfield('a', it.stringValue()))))
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): Value? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?oan
            WHERE {
                ?id             bf:identifiedBy ?identifiedById .
                ?identifiedById rdf:type        bf:LcOverseasAcq ;
                                rdf:value       ?oan .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { it.getValue("oan") }.firstOrNull()
        }
    }
}