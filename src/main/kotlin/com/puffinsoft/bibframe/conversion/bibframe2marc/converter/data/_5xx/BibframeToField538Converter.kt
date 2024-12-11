package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField538Converter : BibframeToMarcConverter {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply { tag = "538" }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.note)
            addSubfieldIfExists(builder, 'u', it.electronicLocator)
            addInstitutionSubfieldIfExists(builder, it.institution)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<SystemDetailsNoteData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?note ?electronicLocator ?appliesTo ?institution ?institutionId
            WHERE {
                ?id         bf:systemRequirement    ?noteId .
                ?noteId     rdf:type                bf:SystemRequirement ;
                            rdfs:label              ?note .
                OPTIONAL {
                    ?noteId bf:electronicLocator    ?electronicLocator .
                }
                OPTIONAL {
                    ?noteId         bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
                OPTIONAL {
                    ?noteId         bflc:applicableInstitution  ?institutionId .
                    ?institutionId  rdf:type                    bf:Agent .
                    OPTIONAL {
                        ?institutionId bf:code ?institution .
                    }
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { SystemDetailsNoteData(it.getValue("note"), it.getValue("electronicLocator"), it.getValue("appliesTo"), if (it.hasBinding("institution")) it.getValue("institution") else it.getValue("institutionId")) }
                .toList()
        }
    }

    @JvmRecord
    private data class SystemDetailsNoteData(val note: Value, val electronicLocator: Value?, val appliesTo: Value?, val institution: Value?)
}