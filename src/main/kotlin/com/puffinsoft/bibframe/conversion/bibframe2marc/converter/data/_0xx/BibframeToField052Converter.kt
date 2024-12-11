package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField052Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            record.dataFields.add(DataField("052", subfields = mutableListOf(Subfield('a', it.stringValue().substringAfter("classification/G")))))
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?classification
            WHERE {
                ?id bf:geographicCoverage ?classification .
                FILTER(STRSTARTS(STR(?classification), "http://id.loc.gov/authorities/classification/G"))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { it.getValue("classification") }.toList()
        }
    }
}