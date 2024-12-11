package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField545Converter : BibframeToMarcConverter {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        val biographicalUri = lookupNoteTypeUri("biogdata")
        val historicalUri = lookupNoteTypeUri("adminhist")

        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "545"
                indicator1 = when(it.noteType.stringValue()) {
                    biographicalUri -> '0'
                    historicalUri -> '1'
                    else -> ' '
                }
            }

            addSubfieldIfExists(builder, 'a', it.note)
            addSubfieldIfExists(builder, 'u', it.electronicLocator)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<BiographicalOrHistoricalNoteData> {
        val biographicalUri = lookupNoteTypeUri("biogdata")
        val historicalUri = lookupNoteTypeUri("adminhist")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?note ?noteType ?electronicLocator
            WHERE {
                ?id         bf:note     ?noteId .
                ?noteId     rdf:type    bf:Note ;
                            rdf:type    ?noteType ;
                            rdfs:label  ?note .
                OPTIONAL {
                    ?noteId bf:electronicLocator    ?electronicLocator .
                }
                FILTER(?noteType IN (<$biographicalUri>, <$historicalUri>))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { BiographicalOrHistoricalNoteData(it.getValue("note"), it.getValue("noteType"), it.getValue("electronicLocator")) }.toList()
        }
    }

    @JvmRecord
    private data class BiographicalOrHistoricalNoteData(val note: Value, val noteType: Value, val electronicLocator: Value?)
}