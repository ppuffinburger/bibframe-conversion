package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField026Converter : BibframeToMarcConverter {
    //  TODO : Do we want one for every instance?
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply { tag = "026" }

            addSubfieldIfExists(builder, 'e', it.fingerprint)
            addSourceSubfieldIfExists(builder, it.source)
            addInstitutionSubfieldIfExists(builder, it.institution)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<FingerprintData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?fingerprint ?sourceId ?source ?agentId ?institution
            WHERE {
                ?id             bf:identifiedBy ?identifiedById .
                ?identifiedById rdf:type        bf:Fingerprint ;
                                rdf:value       ?fingerprint .
                OPTIONAL {
                    ?identifiedById bf:source   ?sourceId .
                    ?sourceId       rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId       bf:code     ?source .
                    }
                }
                OPTIONAL {
                    ?identifiedById bflc:applicableInstitution  ?agentId .
                    ?agentId        rdf:type                    bf:Agent .
                    OPTIONAL {
                        ?agentId bf:code ?institution .
                    }
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { miscResult ->
            return miscResult.filter { it.hasBinding("fingerprint") }
                .map { FingerprintData(it.getValue("fingerprint"), if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId"), if (it.hasBinding("institution")) it.getValue("institution") else it.getValue("agentId")) }
                .toList()
        }
    }

    @JvmRecord
    private data class FingerprintData(val fingerprint: Value, val source: Value?, val institution: Value?)
}