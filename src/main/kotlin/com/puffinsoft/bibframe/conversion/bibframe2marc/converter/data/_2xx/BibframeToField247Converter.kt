package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField247Converter : BibframeToMarcConverter {
    // TODO : subfield X
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        workData.getAllIdsAsValues().forEach { id ->
            query(conn, id).forEach {
                val builder = DataFieldBuilder().apply {
                    tag = "247"
                    indicator1 = '1'
                    indicator2 = '0'
                }

                addSubfieldIfExists(builder, 'a', it.mainTitle)
                addSubfieldIfExists(builder, 'b', it.subtitle)
                addSubfieldIfExists(builder, 'f', it.date)

                if (it.qualifier != null) {
                    addSubfieldIfExists(builder, 'g', "(${it.qualifier.stringValue()})")
                }

                addSubfieldIfExists(builder, 'n', it.partNumber)
                addSubfieldIfExists(builder, 'p', it.partName)
                addSubfieldIfExists(builder, 'x', it.issn)

                record.dataFields.add(builder.build())
            }
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<TitleData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT DISTINCT ?mainTitle ?subtitle ?date ?qualifier ?partNumber ?partName ?issn
            WHERE {
                ?id         bf:title        ?titleId .
                ?titleId    rdf:type        ?titleType ;
                            bf:mainTitle    ?mainTitle ;
                            bf:variantType  ?variantType .
                OPTIONAL {
                    ?titleId bf:subtitle ?subtitle .
                }
                OPTIONAL {
                    ?titleId bf:date ?date .
                }
                OPTIONAL {
                    ?titleId bf:qualifier ?qualifier .
                }
                OPTIONAL {
                    ?titleId bf:partNumber ?partNumber .
                }
                OPTIONAL {
                    ?titleId bf:partName ?partName .
                }
                FILTER(?variantType = "former")
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { TitleData(it.getValue("mainTitle"), it.getValue("subtitle"), it.getValue("date"), it.getValue("qualifier"), it.getValue("partNumber"), it.getValue("partName"), it.getValue("issn")) }.toList()
        }
    }

    @JvmRecord
    private data class TitleData(val mainTitle: Value, val subtitle: Value?, val date: Value?, val qualifier: Value?, val partNumber: Value?, val partName: Value?, val issn: Value?)
}