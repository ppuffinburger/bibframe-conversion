package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField242Converter : BibframeToMarcConverter {
    // TODO : need examples.  need a language part.  Code and Spec don't match.
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        // TODO : all instances?
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "242"
                indicator1 = '1'
                indicator2 = getNonFilingCharacters(it.nonSortNum)
            }

            addSubfieldIfExists(builder, 'a', it.mainTitle)
            addSubfieldIfExists(builder, 'b', it.subtitle)
            addSubfieldIfExists(builder, 'n', it.partNumber)
            addSubfieldIfExists(builder, 'p', it.partName)

            record.dataFields.add(builder.build())
        }

    }

    private fun query(conn: RepositoryConnection, id: Value): List<TranslatedTitleData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?mainTitle ?nonSortNum ?subtitle ?partNumber ?partName
            WHERE {
                ?id         bf:title        ?titleId .
                ?titleId    rdf:type        bf:VariantType ;
                            bf:variantType  ?variantType ;
                            bf:mainTitle    ?mainTitle .
                OPTIONAL {
                    ?titleId bflc:nonSortNum ?nonSortNum .
                }
                OPTIONAL {
                    ?titleId bf:subtitle ?subtitle .
                }
                OPTIONAL {
                    ?titleId bf:partNumber ?partNumber .
                }
                OPTIONAL {
                    ?titleId bf:partNumber ?partName .
                }
                FILTER(?variantType = "translated")
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { TranslatedTitleData(it.getValue("mainTitle"), it.getValue("nonSortNum"), it.getValue("subtitle"), it.getValue("partNumber"), it.getValue("partName")) }.toList()
        }
    }

    @JvmRecord
    private data class TranslatedTitleData(val mainTitle: Value, val nonSortNum: Value?, val subtitle: Value?, val partNumber: Value?, val partName: Value?)
}