package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField082Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "082" }

            when(it.edition.stringValue()) {
                "full" -> builder.indicator1 = '0'
                "abridged" -> builder.indicator1 = '1'
                else -> builder.indicator1 = '7'
            }

            addSubfieldIfExists(builder, 'a', it.classificationPortion)
            addSubfieldIfExists(builder, 'b', it.itemPortion)

            if (it.assigner != null) {
                if (it.assigner.stringValue() == "http://id.loc.gov/vocabulary/organizations/dlc") {
                    builder.indicator2 = '0'
                } else {
                    builder.indicator2 = '4'
                    addSubfieldIfExists(builder, 'q', it.assignerValue)
                }
            }

            addSourceSubfieldIfExists(builder, it.source)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<DeweyDecimalClassificationNumber> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?classificationPortion ?itemPortion ?edition ?assigner ?assignerValue ?source
            WHERE {
                ?id                 bf:classification           ?classificationId .
                ?classificationId   rdf:type                    bf:ClassificationDdc ;
                                    bf:classificationPortion    ?classificationPortion ;
                                    bf:edition                  ?edition .
                OPTIONAL {
                    ?classificationId bf:itemPortion ?itemPortion .
                }
                OPTIONAL {
                    ?classificationId   bf:assigner ?assigner .
                    ?assigner           rdfs:label  ?assignerValue .
                }
                OPTIONAL {
                    ?classificationId bf:source ?sourceId .
                    ?sourceId           bf:code ?source .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { DeweyDecimalClassificationNumber(it.getValue("classificationPortion"), it.getValue("itemPortion"), it.getValue("edition"), it.getValue("assigner"), it.getValue("assignerValue"), it.getValue("source")) }.toList()
        }
    }

    @JvmRecord
    private data class DeweyDecimalClassificationNumber(val classificationPortion: Value, val itemPortion: Value?, val edition: Value, val assigner: Value?, val assignerValue: Value?, val source: Value?)
}