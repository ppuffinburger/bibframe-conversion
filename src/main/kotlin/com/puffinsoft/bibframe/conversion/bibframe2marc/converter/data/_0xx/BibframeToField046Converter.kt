package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_DATATYPES_EDTF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.edtf4k.EdtfDate
import org.edtf4k.EdtfDateFactory
import org.edtf4k.EdtfDatePair
import org.marc4k.marc.DataField
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField046Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId, "originDate").forEach {
            record.dataFields.add(buildDataField(it, 'k', 'l'))
        }

        query(conn, workData.workId, "validDate").forEach {
            record.dataFields.add(buildDataField(it, 'm', 'n'))
        }
    }

    private fun buildDataField(dateValue: Value, startSubfieldName: Char, endSubfieldName: Char): DataField {
        val field = DataField("046")

        when (val edtfDateType = EdtfDateFactory.parse(dateValue.stringValue())) {
            is EdtfDate -> {
                field.subfields.add(Subfield(startSubfieldName, edtfDateType.toString()))
            }
            is EdtfDatePair -> {
                field.subfields.add(Subfield(startSubfieldName, edtfDateType.start.toString()))
                if (edtfDateType.isRange) {
                    field.subfields.add(Subfield(endSubfieldName, edtfDateType.end.toString()))
                }
            }
            else -> {}
        }

        return field
    }

    private fun query(conn: RepositoryConnection, id: Value, type: String): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?date
            WHERE {
                ?id     bf:$type    ?date .
                FILTER(DATATYPE(?date) = <$LOC_DATATYPES_EDTF>)
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { it.getValue("date") }.toList()
        }
    }
}