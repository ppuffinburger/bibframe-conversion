package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField351Converter : BibframeToMarcConverter {
    // TODO : no example
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "351" }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'c', it.hierarchicalLevel)
            addSubfieldIfExists(builder, 'a', it.organization)
            addSubfieldIfExists(builder, 'b', it.pattern)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<CollectionArrangement> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?organization ?pattern ?hierarchicalLevel ?appliesTo
            WHERE {
                ?id     bf:collectionArrangement    ?caId .
                ?caId   rdf:type                    bf:CollectionArrangement .
                OPTIONAL {
                    ?caId   bf:collectionOrganization   ?organization .
                }
                OPTIONAL {
                    ?caId   bf:pattern                  ?pattern .
                }
                OPTIONAL {
                    ?caId   bf:hierarchicalLevel        ?hierarchicalLevel .
                }
                OPTIONAL {
                    ?caId               bflc:appliesTo  ?appliesToId .
                    ?appliesToId        rdf:type        bflc:AppliesTo ;
                                        rdfs:label      ?appliesTo .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("organization") || it.hasBinding("pattern") || it.hasBinding("hierarchicalLevel") || it.hasBinding("appliesTo") }
                .map { CollectionArrangement(it.getValue("organization"), it.getValue("pattern"), it.getValue("hierarchicalLevel"), it.getValue("appliesTo")) }.toList()
        }
    }

    @JvmRecord
    private data class CollectionArrangement(val organization: Value?, val pattern: Value?, val hierarchicalLevel: Value?, val appliesTo: Value?)
}