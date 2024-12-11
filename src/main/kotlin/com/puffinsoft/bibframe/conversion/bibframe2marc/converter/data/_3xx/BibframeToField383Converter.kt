package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField383Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId)?.let {
            val builder = DataFieldBuilder().apply { tag = "383" }
            addSubfieldIfExists(builder, 'a', it.serialNumber)
            addSubfieldIfExists(builder, 'b', it.opusNumber)
            addSubfieldIfExists(builder, 'c', it.thematicNumber)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): NumericDesignationData? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?opusNumber ?serialNumber ?thematicNumber
            WHERE {
                OPTIONAL {
                    ?id bf:musicOpusNumber ?opusNumber .
                }
                OPTIONAL {
                    ?id bf:musicSerialNumber ?serialNumber .
                }
                OPTIONAL {
                    ?id bf:musicThematicNumber ?thematicNumber .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("opusNumber") || it.hasBinding("serialNumber") || it.hasBinding("thematicNumber") }
                .map { NumericDesignationData(it.getValue("opusNumber"), it.getValue("serialNumber"), it.getValue("thematicNumber")) }
                .firstOrNull()
        }
    }

    @JvmRecord
    private data class NumericDesignationData(val opusNumber: Value?, val serialNumber: Value?, val thematicNumber: Value?)
}