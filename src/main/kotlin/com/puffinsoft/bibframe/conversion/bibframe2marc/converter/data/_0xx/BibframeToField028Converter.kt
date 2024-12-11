package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField028Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        add(conn, workData.getAllInstanceIds(), record, "AudioIssueNumber", '0')
        add(conn, workData.getAllInstanceIds(), record, "MatrixNumber", '1')
        add(conn, workData.getAllInstanceIds(), record, "MusicPlate", '2')
        add(conn, workData.getAllInstanceIds(), record, "MusicPublisherNumber", '3')
        add(conn, workData.getAllInstanceIds(), record, "VideoRecordingNumber", '4')
        add(conn, workData.getAllInstanceIds(), record, "PublisherNumber", '5')
        add(conn, workData.getAllInstanceIds(), record, "MusicDistributorNumber", '6')
    }

    private fun add(conn: RepositoryConnection, ids: List<Value>, record: BibliographicRecord, identifierType: String, firstIndicator: Char) {
        for (id in ids) {
            query(conn, id, identifierType).forEach {
                val builder = DataFieldBuilder().apply {
                    tag = "028"
                    indicator1 = firstIndicator
                    indicator2 = '2'
                }

                addSubfieldIfExists(builder, 'a', it.identifier)
                addSubfieldIfExists(builder, 'b', it.source)
                addSubfieldIfExists(builder, 'q', it.qualifier)

                record.dataFields.add(builder.build())
            }
        }
    }

    private fun query(conn: RepositoryConnection, id: Value, identifierType: String): List<PublisherOrDistributorNumberData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?identifier ?qualifier ?source
            WHERE {
                ?id             bf:identifiedBy ?identifiedById .
                ?identifiedById rdf:type        bf:$identifierType ;
                                rdf:value       ?identifier .
                OPTIONAL {
                    ?identifiedById bf:assigner ?agentId .
                    ?agentId        rdf:type    bf:Agent ;
                                    rdfs:label  ?source .
                }
                OPTIONAL {
                    ?identifiedById bf:qualifier ?qualifier .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { PublisherOrDistributorNumberData(it.getValue("identifier"), it.getValue("qualifier"), it.getValue("source")) }.toList()
        }
    }

    @JvmRecord
    private data class PublisherOrDistributorNumberData(val identifier: Value, val qualifier: Value?, val source: Value?)
}