package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField222Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "222"
                indicator2 = getNonFilingCharacters(it.nonSortNum)
            }

            addSubfieldIfExists(builder, 'a', it.mainTitle)
            addSubfieldIfExists(builder, 'q', it.qualifier)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<KeyTitleData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT DISTINCT ?mainTitle ?nonSortNum ?qualifier
            WHERE {
                ?id         bf:title        ?titleId .
                ?titleId    rdf:type        bf:KeyTitle ;
                            bf:mainTitle    ?mainTitle .
                OPTIONAL {
                    ?titleId bflc:nonSortNum ?nonSortNum .
                }
                OPTIONAL {
                    ?titleId bf:qualifier ?qualifier .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { KeyTitleData(it.getValue("mainTitle"), it.getValue("nonSortNum"), it.getValue("qualifier")) }.toList()
        }
    }

    @JvmRecord
    private data class KeyTitleData(val mainTitle: Value, val nonSortNum: Value?, val qualifier: Value?)
}