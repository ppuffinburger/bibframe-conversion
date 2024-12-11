package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField500Converter : BibframeToMarcConverter {
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
            val builder = DataFieldBuilder().apply { tag = "500" }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.note)
            addInstitutionSubfieldIfExists(builder, it.institution)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<GeneralNoteData> {
        val noteIdQueryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?noteId ?note
            WHERE {
                ?id         bf:note     ?noteId .
                ?noteId     rdf:type    bf:Note ;
                            rdfs:label  ?note .
                OPTIONAL {
                    ?noteId     rdf:type    ?noteType .
                    FILTER(?noteType != bf:Note)
                }
                FILTER(!BOUND(?noteType))
            }
        """.trimIndent()

        val noteIdQuery = conn.prepareTupleQuery(noteIdQueryString)
        noteIdQuery.setBinding("id", id)

        noteIdQuery.evaluate().use { noteIdResult ->
            return noteIdResult.map { noteIdBindings ->
                val noteId = noteIdBindings.getValue("noteId")
                val note = noteIdBindings.getValue("note")

//                val qString = """
//                    PREFIX bf: <$BIBFRAME_ONTOLOGY>
//                    PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
//                    SELECT ?institutionType
//                    WHERE {
//                        ?noteId         bflc:applicableInstitution  ?institutionId .
//                        ?institutionId  rdf:type                    ?institutionType .
//                    }
//                """.trimIndent()
//
//                val q = conn.prepareTupleQuery(qString)
//                q.setBinding("noteId", noteId)
//                q.evaluate().use { qResult ->
//                    qResult.map { qBindings ->
//                        println(qBindings.getValue("institutionType"))
//                    }
//                }

                val notePropsQueryString = """
                    PREFIX bf: <$BIBFRAME_ONTOLOGY>
                    PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
                    SELECT ?institutionId ?institution ?appliesTo
                    WHERE {
                        OPTIONAL {
                            SELECT ?institution ?institutionId
                            WHERE {
                                ?noteId         bflc:applicableInstitution  ?institutionId .
                                ?institutionId  rdf:type                    bf:Agent .
                                OPTIONAL {
                                    ?institutionId  rdf:type    ?institutionType .
                                    FILTER(?institutionType != bf:Agent)
                                }
                                FILTER(!BOUND(?institutionType))
                                OPTIONAL {
                                    ?institutionId  bf:code ?institution .
                                    FILTER(DATATYPE(?institution) = <http://id.loc.gov/datatypes/orgs/code>)
                                }
                            }
                        }
                        OPTIONAL {
                            SELECT ?appliesTo
                            WHERE {
                                ?noteId         bflc:appliesTo  ?appliesToId .
                                ?appliesToId    rdf:type        bflc:AppliesTo ;
                                                rdfs:label      ?appliesTo .
                            }
                        }
                    }
                """.trimIndent()

                val notePropsQuery = conn.prepareTupleQuery(notePropsQueryString)
                notePropsQuery.setBinding("noteId", noteId)

                notePropsQuery.evaluate().use { notePropsResult ->
                    if (notePropsResult.hasNext()) {
                        return notePropsResult.map { notePropsBindings ->
                            // TODO : currently filtering out institution to only institutions which are ONLY Agents in the query.  If institution has been assigned multiple types from this record (or others) in other parts of the data
                            //  it could wrongly pick up data.
                            GeneralNoteData(note, notePropsBindings.getValue("institution") ?: notePropsBindings.getValue("institutionId"), notePropsBindings.getValue("appliesTo"))
                        }
                    } else {
                        GeneralNoteData(note, null, null)
                    }
                }
            }
        }
    }

    @JvmRecord
    private data class GeneralNoteData(val note: Value, val institution: Value?, val appliesTo: Value?)
}