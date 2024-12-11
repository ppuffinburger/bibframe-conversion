package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField032Converter : BibframeToMarcConverter {
    // TODO : Do not have an EXAMPLE record.
    //  Do we want one for every instance?
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "032"
                subfields {
                    subfield {
                        name = 'a'
                        data = it.prn.stringValue()
                    }
                }
            }

            addSubfieldIfExists(builder, 'b', it.source)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<PostalRegistrationNumberData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?prn ?source
            WHERE {
                ?id             bf:identifiedBy ?identifiedById .
                ?identifiedById rdf:type        bf:PostalRegistration ;
                                rdf:value       ?identifier .
                OPTIONAL {
                    ?identifiedById bf:assigner ?agentId .
                    ?agentId        rdf:type    bf:Agent ;
                                    rdfs:label  ?source .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { PostalRegistrationNumberData(it.getValue("prn"), it.getValue("source")) }.toList()
        }
    }

    @JvmRecord
    private data class PostalRegistrationNumberData(val prn: Value, val source: Value?)
}