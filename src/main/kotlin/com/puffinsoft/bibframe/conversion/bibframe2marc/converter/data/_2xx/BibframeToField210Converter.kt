package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField210Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        val abbreviatedTitleData = query(conn, workData.workId)

        if (abbreviatedTitleData.isEmpty()) {
            // TODO : all instances?
            query(conn, workData.primaryInstanceId).forEach { record.dataFields.add(createDataField(it)) }
        } else {
            abbreviatedTitleData.forEach { record.dataFields.add(createDataField(it)) }
        }
    }

    private fun createDataField(data: AbbreviatedTitleData): DataField {
        val agentCode = data.agentCode?.stringValue()

        val builder = DataFieldBuilder().apply {
            tag = "210"
            indicator1 = '0'
            indicator2 = if ("issnkey" == agentCode) ' ' else '0'
        }

        addSubfieldIfExists(builder, 'a', data.mainTitle)
        addSubfieldIfExists(builder, 'q', data.qualifier)

        if (agentCode != null && "issnkey" != agentCode) {
            addSubfieldIfExists(builder, '2', agentCode)
        }

        return builder.build()
    }

    private fun query(conn: RepositoryConnection, id: Value): List<AbbreviatedTitleData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT DISTINCT ?mainTitle ?qualifier ?agentCode
            WHERE {
                ?id         bf:title        ?titleId .
                ?titleId    rdf:type        bf:AbbreviatedTitle ;
                            bf:mainTitle    ?mainTitle .
                OPTIONAL {
                    ?titleId bf:qualifier ?qualifier .
                }
                OPTIONAL {
                    ?titleId    bf:assigner ?assignerId .
                    ?assignerId bf:code     ?agentCode .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { AbbreviatedTitleData(it.getValue("mainTitle"), it.getValue("qualifier"), it.getValue("agentCode")) }.toList()
        }
    }

    @JvmRecord
    private data class AbbreviatedTitleData(val mainTitle: Value, val qualifier: Value?, val agentCode: Value?)
}