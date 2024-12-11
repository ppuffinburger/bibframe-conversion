package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField243Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId)?.let {
            val builder = DataFieldBuilder().apply {
                tag = "243"
                indicator1 = '1'
                indicator2 = getNonFilingCharacters(it.nonSortNum)
            }

            addSubfieldIfExists(builder, 'a', it.mainTitle)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): CollectiveUniformTitleData? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?mainTitle ?nonSortNum
            WHERE {
                ?id         bf:title        ?titleId .
                ?titleId    rdf:type        bf:CollectiveTitle ;
                            bf:mainTitle    ?mainTitle .
                OPTIONAL {
                    ?titleId bflc:nonSortNum ?nonSortNum .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { CollectiveUniformTitleData(it.getValue("mainTitle"), it.getValue("nonSortNum")) }.firstOrNull()
        }
    }

    @JvmRecord
    private data class CollectiveUniformTitleData(val mainTitle: Value, val nonSortNum: Value?)
}