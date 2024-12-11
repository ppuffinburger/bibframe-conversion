package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.apache.commons.lang3.StringUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField043Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        val geoCodes = query(conn, workData.workId)

        if (geoCodes.isNotEmpty()) {
            val builder = DataFieldBuilder().apply { tag = "043" }

            geoCodes.forEach {
                if (it.codeValue == null) {
                    addSubfieldIfExists(builder, 'a', StringUtils.rightPad(TextUtils.getCodeStringFromUrl(it.codeUri.stringValue()), 7, '-'))
                    addSubfieldIfExists(builder, '0', it.codeUri)
                } else {
                    if (it.source != null) {
                        if (it.source.stringValue() == "ISO 3166") {
                            addSubfieldIfExists(builder, 'c', it.codeValue)
                        } else {
                            addSubfieldIfExists(builder, 'b', it.codeValue)
                            addSubfieldIfExists(builder, '2', it.source)
                        }
                    }
                }
            }

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<GeographicCoverage> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?codeUri ?codeValue ?source
            WHERE {
                ?id         bf:geographicCoverage   ?codeUri .
                ?codeUri    rdf:type                bf:GeographicCoverage .
                FILTER(STRSTARTS(STR(?codeUri), "http://id.loc.gov/vocabulary/geographicAreas/"))
                OPTIONAL {
                    ?codeUri rdf:value ?codeValue .
                }
                OPTIONAL {
                    ?codeUri    bf:source   ?sourceId .
                    ?sourceId   rdf:type    bf:Source ;
                                bf:code     ?source
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { GeographicCoverage(it.getValue("codeUri"), it.getValue("codeValue"), it.getValue("source")) }.toList()
        }
    }

    @JvmRecord
    private data class GeographicCoverage(val codeUri: Value, val codeValue: Value?, val source: Value?)
}