package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField502Converter : BibframeToMarcConverter {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "502" }
            addSubfieldIfExists(builder, 'a', it.note)
            addSubfieldIfExists(builder, 'b', it.degree)
            addSubfieldIfExists(builder, 'c', it.institution)
            addSubfieldIfExists(builder, 'd', it.date)
            addSubfieldIfExists(builder, 'g', it.miscInformation)
            addSubfieldIfExists(builder, 'o', it.identifiedBy)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<DissertationNoteData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?note ?degree ?institution ?date ?miscInformation ?identifiedBy
            WHERE {
                ?id             bf:dissertation ?dissertationId .
                ?dissertationId rdf:type        bf:Dissertation ;
                                rdfs:label      ?note .
                OPTIONAL {
                    ?dissertationId bf:degree ?degree .
                }
                OPTIONAL {
                    ?dissertationId bf:grantingInstitution  ?institutionId .
                    ?institutionId  rdf:type                bf:Agent ;
                                    rdfs:label              ?institution .
                }
                OPTIONAL {
                    ?dissertationId bf:date ?date .
                }
                OPTIONAL {
                    ?dissertationId bf:note     ?noteId .
                    ?noteId         rdf:type    bf:Note ;
                                    rdfs:label  ?miscInformation .
                }
                OPTIONAL {
                    ?dissertationId bf:identifiedBy ?identifiedById .
                    ?identifiedById rdf:type        bf:DissertationIdentifier ;
                                    rdf:value       ?identifiedBy .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map {
                DissertationNoteData(
                    it.getValue("note"),
                    it.getValue("degree"),
                    it.getValue("institution"),
                    it.getValue("date"),
                    it.getValue("miscInformation"),
                    it.getValue("identifiedBy")
                )
            }.toList()
        }
    }

    @JvmRecord
    private data class DissertationNoteData(val note: Value, val degree: Value?, val institution: Value?, val date: Value?, val miscInformation: Value?, val identifiedBy: Value?)
}