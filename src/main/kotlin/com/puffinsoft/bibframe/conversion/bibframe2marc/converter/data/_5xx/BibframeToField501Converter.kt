package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField501Converter : BibframeToMarcConverter {
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
            val builder = DataFieldBuilder().apply { tag = "501" }
            addSubfieldIfExists(builder, 'a', it.note)
            addInstitutionSubfieldIfExists(builder, it.institution)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<WithNoteData> {
        val withNoteUri = lookupNoteTypeUri("with")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?note ?institution ?institutionId
            WHERE {
                ?id         bf:note     ?noteId .
                ?noteId     rdf:type    bf:Note ;
                            rdf:type    <$withNoteUri> ;
                            rdfs:label  ?note .
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
            }
            """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            // TODO : same problem as 500's.  Can't limit to IRIs of applicableInstitution to only Agent if some other part of the graph gives an additional type
            return result.map { WithNoteData(it.getValue("note"), it.getValue("institution") ?: it.getValue("institutionId")) }.toList()
        }
    }

    @JvmRecord
    private data class WithNoteData(val note: Value, val institution: Value?)
}