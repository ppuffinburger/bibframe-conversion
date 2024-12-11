package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField080Converter : BibframeToMarcConverter {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "080" }

            when(it.edition.stringValue()) {
                "full" -> builder.indicator1 = '0'
                "abridged" -> builder.indicator1 = '1'
                else -> builder.indicator1 = ' '
            }

            addSubfieldIfExists(builder, 'a', it.classificationPortion)
            addSubfieldIfExists(builder, 'b', it.itemPortion)

            it.subdivisions.forEach { subdivision ->
                addSubfieldIfExists(builder, 'x', subdivision)
            }

            if (it.uri.isIRI) {
                addSubfieldIfExists(builder, '0', it.uri)
            }

            addSourceSubfieldIfExists(builder, it.source)

            record.dataFields.add(builder.build())
        }

    }

    private fun query(conn: RepositoryConnection, id: Value): List<UniversalDecimalClassificationNumber> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?classificationId ?classificationPortion ?itemPortion ?edition ?assigner ?assignerValue ?source
            WHERE {
                ?id                 bf:classification           ?classificationId .
                ?classificationId   rdf:type                    bf:ClassificationUdc ;
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
            return result.map {
                val subdivisionQueryString = """
                    PREFIX bf: <$BIBFRAME_ONTOLOGY>
                    SELECT ?subdivision
                    WHERE {
                        ?id                 bf:classification           ?classificationId .
                        ?classificationId   rdf:type                    bf:ClassificationUdc ;
                                            bf:code                     ?subdivision .
                    }
                """.trimIndent()

                val subdivisionQuery = conn.prepareTupleQuery(subdivisionQueryString)
                subdivisionQuery.setBinding("id", id)
                subdivisionQuery.setBinding("classificationId", it.getValue("classificationId"))

                val subdivisions = subdivisionQuery.evaluate().use { subdivisionResult ->
                    subdivisionResult.map { subdivisionIt -> subdivisionIt.getValue("subdivision") }.toList()
                }

                UniversalDecimalClassificationNumber(it.getValue("classificationId"), it.getValue("classificationPortion"), it.getValue("itemPortion"), it.getValue("edition"), subdivisions, it.getValue("source"))
            }.toList()
        }
    }

    @JvmRecord
    private data class UniversalDecimalClassificationNumber(val uri: Value, val classificationPortion: Value, val itemPortion: Value?, val edition: Value, val subdivisions: List<Value>, val source: Value?)
}