package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField561Converter : BibframeToMarcConverter {
    // TODO : this seems not correct.  The specs say that the note is the object off bf:custodialHistory but bf:custodialHistory is a Literal, so how do you get to the appliesTo or institution.
    //  Code is commented out, so they must have problems with the spec also.
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        workData.getAllInstanceIds().forEach {
            buildFieldFromId(conn, it, record)
        }

        workData.itemIds.forEach {
            buildFieldFromId(conn, it, record)
        }
    }

    private fun buildFieldFromId(conn: RepositoryConnection, id: Value, record: BibliographicRecord) {
        query(conn, id).forEach {
            val builder = DataFieldBuilder().apply { tag = "561" }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.note)
            addInstitutionSubfieldIfExists(builder, it.institution)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<OwnershipAndCustodialNoteData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?note ?appliesTo ?institution ?institutionId
            WHERE {
                ?id         bf:custodialHistory     ?note .
            }
            """.trimIndent()

        //                OPTIONAL {
        //                    ?noteId         bflc:appliesTo  ?appliesToId .
        //                    ?appliesToId    rdf:type        bflc:AppliesTo ;
        //                                    rdfs:label      ?appliesTo .
        //                }
        //                OPTIONAL {
        //                    SELECT ?institution ?institutionId
        //                    WHERE {
        //                        ?noteId         bflc:applicableInstitution  ?institutionId .
        //                        ?institutionId  rdf:type                    bf:Agent .
        //                        OPTIONAL {
        //                            ?institutionId  rdf:type    ?institutionType .
        //                            FILTER(?institutionType != bf:Agent)
        //                        }
        //                        FILTER(!BOUND(?institutionType))
        //                        OPTIONAL {
        //                            ?institutionId  bf:code ?institution .
        //                            FILTER(DATATYPE(?institution) = <http://id.loc.gov/datatypes/orgs/code>)
        //                        }
        //                    }
        //                }

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { OwnershipAndCustodialNoteData(it.getValue("note"), it.getValue("appliesTo"), it.getValue("institution") ?: it.getValue("institutionId")) }.toList()
        }
    }

    @JvmRecord
    private data class OwnershipAndCustodialNoteData(val note: Value, val appliesTo: Value?, val institution: Value?)
}