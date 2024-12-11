package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField034Converter : BibframeToMarcConverter {
    // TODO : this works like the code, but is different than MARC.  MARC $d-$g appear in 255 according to Bibframe spec
    // MARC
    //034	1_	 |a a |b 62500000 |d W1800000 |e E1800000 |f N0800000 |g S0680000
    //034	1_	 |a a |b 60000000 |d W1800000 |e E1800000 |f N0900000 |g S0900000
    // Bibframe
    //034 1  $aa$b62500000
    //034 1  $aa$b60000000
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val dataField = DataField(
                "034", '1', subfields = mutableListOf(
                    Subfield('a', if (it.category.stringValue().startsWith("linear")) "a" else "b"),
                    Subfield(if (it.category.stringValue().contains("horizontal")) 'b' else 'c', it.scale.stringValue())
                )
            )
            record.dataFields.add(dataField)
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<CodedCartographicMathematicalData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?scale ?category
            WHERE {
                ?id         bf:scale    ?scaleId .
                ?scaleId    rdf:type    bf:Scale ;
                            rdf:value   ?scale ;
                            rdfs:label  ?category .
                FILTER(REGEX(?category, "^linear|^angular"))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { CodedCartographicMathematicalData(it.getValue("scale"), it.getValue("category")) }.toList()
        }
    }

    @JvmRecord
    private data class CodedCartographicMathematicalData(val scale: Value, val category: Value)
}