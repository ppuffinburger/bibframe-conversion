package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField334Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId)?.let {
            val builder = DataFieldBuilder().apply { tag = "334" }
            addSubfieldIfExists(builder, 'a', it.issuance)

            if (it.uri.isIRI) {
                addSubfieldIfExists(builder, 'b', TextUtils.getCodeStringFromUrl(it.uri))
                addSubfieldIfExists(builder, '0', it.uri)
            }

            addSourceSubfieldIfExists(builder, it.source)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): IssuanceData? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?issuance ?issuanceId ?source ?sourceId
            WHERE {
                ?id         bf:issuance ?issuanceId .
                ?issuanceId rdf:type    bf:Issuance ;
                            rdfs:label  ?issuance .
                OPTIONAL {
                    ?issuanceId bf:source   ?sourceId .
                    ?sourceId   rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { IssuanceData(it.getValue("issuance"), it.getValue("issuanceId"), if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId")) }.firstOrNull()
        }
    }

    @JvmRecord
    private data class IssuanceData(val issuance: Value, val uri: Value, val source: Value?)
}