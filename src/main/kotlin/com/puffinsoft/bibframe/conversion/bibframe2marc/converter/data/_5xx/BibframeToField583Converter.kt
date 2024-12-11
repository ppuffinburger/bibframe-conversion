package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField583Converter : BibframeToMarcConverter {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply { tag = "583" }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.note)
            addSubfieldIfExists(builder, 'd', it.date)
            addSubfieldIfExists(builder, 'k', it.agent)
            addSubfieldIfExists(builder, 'u', it.electronicLocator)
            addSubfieldIfExists(builder, 'z', it.publicNote)
            addSourceSubfieldIfExists(builder, it.source)
            addInstitutionSubfieldIfExists(builder, it.institution)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<ActionNoteData> {
        val actionUri = lookupNoteTypeUri("action")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?note ?appliesTo ?date ?agent ?electronicLocator ?publicNote ?source ?sourceId ?institution ?institutionId
            WHERE {
                ?id         bf:note     ?noteId .
                ?noteId     rdf:type    bf:Note ;
                            rdf:type    <$actionUri> ;
                            rdfs:label  ?note .
                OPTIONAL {
                    ?noteId         bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
                OPTIONAL {
                    ?noteId bf:date ?date .
                }
                OPTIONAL {
                    ?noteId     bf:agent    ?agentId .
                    ?agentId    rdf:type    bf:Agent ;
                                rdfs:label  ?agent .
                }
                OPTIONAL {
                    ?noteId bf:electronicLocator    ?electronicLocator .
                }
                OPTIONAL {
                    ?noteId         bf:note     ?publicNoteId .
                    ?publicNoteId   rdf:type    bf:Note ;
                                    rdfs:label  ?publicNote .
                }
                OPTIONAL {
                    ?noteId         bf:source   ?sourceId .
                    ?sourceId       rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
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
            return result.map {
                ActionNoteData(
                    it.getValue("note"),
                    it.getValue("appliesTo"),
                    it.getValue("date"),
                    it.getValue("agent"),
                    it.getValue("electronicLocator"),
                    it.getValue("publicNote"),
                    if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId"),
                    if (it.hasBinding("institution")) it.getValue("institution") else it.getValue("institutionId")
                )
            }.toList()
        }
    }

    @JvmRecord
    private data class ActionNoteData(val note: Value, val appliesTo: Value?, val date: Value?, val agent: Value?, val electronicLocator: Value?, val publicNote: Value?, val source: Value?, val institution: Value?)
}