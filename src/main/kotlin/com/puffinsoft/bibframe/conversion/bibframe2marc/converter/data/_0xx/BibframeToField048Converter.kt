package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.PerformerLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField048Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        val subfieldDataList = mutableListOf<String>()
        subfieldDataList.addAll(createSubfieldData(conn, workData.workId, "instrument", "MusicInstrument", "instrumentalType"))
        subfieldDataList.addAll(createSubfieldData(conn, workData.workId, "ensemble", "MusicEnsemble", "ensembleType"))
        subfieldDataList.addAll(createSubfieldData(conn, workData.workId, "voice", "MusicVoice", "voiceType"))

        if (subfieldDataList.isNotEmpty()) {
            val dataField = DataField("048")
            subfieldDataList.forEach {
                dataField.subfields.add(Subfield('a', it))
            }
            record.dataFields.add(dataField)
        }
    }

    private fun createSubfieldData(conn: RepositoryConnection, id: Value, workType: String, objectType: String, predicateType: String): List<String> {
        val subfieldDataList = mutableListOf<String>()
        query(conn, id, workType, objectType, predicateType).forEach { data ->
            PerformerLookup.lookup(data.type.stringValue(), if (data.label == null) "" else data.label.stringValue())?.let {
                with(StringBuilder()) {
                    append(it)

                    if (data.count != null) {
                        append(data.count.stringValue().padStart(2, '0'))
                    }

                    subfieldDataList.add(toString())
                }
            }
        }
        return subfieldDataList
    }

    private fun query(conn: RepositoryConnection, id: Value, workType: String, objectType: String, predicateType: String): List<NumberOfInstrumentsOrVoicesCode> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?type ?label ?count
            WHERE {
                ?id     bf:$workType        ?typeId .
                ?typeId rdf:type            bf:$objectType ;
                        bf:$predicateType   ?type ;
                OPTIONAL {
                    ?typeId rdfs:label  ?label .
                }
                OPTIONAL {
                    ?typeId bf:count ?count .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { NumberOfInstrumentsOrVoicesCode(it.getValue("type"), it.getValue("label"), it.getValue("count")) }.toList()
        }
    }

    @JvmRecord
    private data class NumberOfInstrumentsOrVoicesCode(val type: Value, val label: Value?, val count: Value?)
}