package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField336Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "336" }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.contentType)
            addSubfieldIfExists(builder, 'b', TextUtils.getCodeStringFromUrl(it.uri))
            addSubfieldIfExists(builder, '0', it.uri)
            addSourceSubfieldIfExists(builder, it.source)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<ContentType> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?contentType ?contentTypeId ?source ?sourceId ?appliesTo
            WHERE {
                ?id             bf:content  ?contentTypeId .
                ?contentTypeId  rdf:type    bf:Content ;
                                rdfs:label  ?contentType .
                OPTIONAL {
                    ?contentTypeId  bf:source   ?sourceId .
                    ?sourceId       rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?contentTypeId  bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { ContentType(it.getValue("contentType"), it.getValue("contentTypeId"), if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId"), it.getValue("appliesTo")) }.toList()
        }
    }

    @JvmRecord
    private data class ContentType(val contentType: Value, val uri: Value, val source: Value?, val appliesTo: Value?)
}