package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.ClassificationSchemeLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.StatusCodeLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField086Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        val sudocsUri = lookupClassificationSchemeUri("sudocs")
        val cacodocUri = lookupClassificationSchemeUri("cacodoc")

        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "086" }

            when(it.sourceId.stringValue()) {
                sudocsUri -> builder.indicator1 = '0'
                cacodocUri -> builder.indicator1 = '1'
                else -> builder.indicator1 = ' '
            }

            addSubfieldIfExists(builder, if (it.cancelled) 'z' else 'a', it.classificationNumber)
//            addSubfieldIfExists(builder, '2', it.source ?: it.sourceId)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<GovernmentDocumentClassificationNumber> {
        val cancelledOrInvalidUri = lookupStatusCodeUri("cancinv")
        val sudocsUri = lookupClassificationSchemeUri("sudocs")
        val cacodocUri = lookupClassificationSchemeUri("cacodoc")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?classificationNumber ?cancelled ?sourceId ?source
            WHERE {
                ?id                 bf:classification           ?classificationId .
                ?classificationId   rdf:type                    bf:Classification ;
                                    rdfs:label                  ?classificationNumber ;
                                    bf:source                   ?sourceId .
                ?sourceId           rdf:type                    bf:Source .
                OPTIONAL {
                    ?sourceId bf:code ?source .
                }
                OPTIONAL {
                    ?classificationId   bf:status   ?cancelled .
                    ?cancelled          rdf:type    bf:Status .
                    FILTER(?statusId = <$cancelledOrInvalidUri>)
                }
                FILTER(?sourceId IN (<$sudocsUri>, <$cacodocUri>))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { GovernmentDocumentClassificationNumber(it.getValue("classificationNumber"), it.hasBinding("cancelled"), it.getValue("sourceId"), it.getValue("source")) }.toList()
        }
    }

    @JvmRecord
    private data class GovernmentDocumentClassificationNumber(val classificationNumber: Value, val cancelled: Boolean, val sourceId: Value, val source: Value?)
}