package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField530Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply { tag = "530" }
            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.note)
            addSubfieldIfExists(builder, 'd', it.identifiedBy)
            addSubfieldIfExists(builder, 'u', it.electronicLocator)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<AdditionPhysicalFormAvailableNoteData> {
        val addPhysicalUri = lookupNoteTypeUri("addphys")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?note ?electronicLocator ?appliesTo ?identifiedBy
            WHERE {
                ?id     bf:note     ?noteId .
                ?noteId rdf:type    bf:Note ;
                        rdf:type    <$addPhysicalUri> ;
                        rdfs:label  ?note .
                OPTIONAL {
                    ?noteId bf:electronicLocator    ?electronicLocator .
                }
                OPTIONAL {
                    ?noteId         bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
                OPTIONAL {
                    ?noteId         bf:identifiedBy ?identifiedById .
                    ?identifiedById rdf:type        bf:StockNumber ;
                                    rdf:value       ?identifiedBy .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { AdditionPhysicalFormAvailableNoteData(it.getValue("note"), it.getValue("electronicLocator"), it.getValue("appliesTo"), it.getValue("identifiedBy")) }.toList()
        }
    }

    @JvmRecord
    private data class AdditionPhysicalFormAvailableNoteData(val note: Value, val electronicLocator: Value?, val appliesTo: Value?, val identifiedBy: Value?)
}