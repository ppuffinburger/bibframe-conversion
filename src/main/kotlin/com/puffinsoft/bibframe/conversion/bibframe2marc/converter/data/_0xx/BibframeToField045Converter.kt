package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField045Converter : BibframeToMarcConverter {
    // TODO : not sure what to do.  docs say one thing, code says doing lookups when X's are involved.  following docs for now, but lookup class has already been coded.
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val dataField: DataField = if (it.stringValue().contains('/')) {
                DataField("045", '2', subfields = mutableListOf(Subfield('b', it.stringValue().substringBefore('/')), Subfield('b', it.stringValue().substringAfter('/'))))
            } else {
                if (it.stringValue().length == 4) {
                    DataField("045", subfields = mutableListOf(Subfield('a', it.stringValue())))
                } else {
                    DataField("045", subfields = mutableListOf(Subfield('b', it.stringValue())))
                }
            }

            record.dataFields.add(dataField)
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?timePeriod
            WHERE {
                ?id bf:temporalCoverage ?timePeriod .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { it.getValue("timePeriod") }.toList()
        }
    }
}