package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField341Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "341" }

            addSubfieldIfExists(builder, '3', it.appliesTo)

            it.notes.forEach { note ->
                val name = when (note.stringValue().substringBefore(":").lowercase()) {
                    "content access mode" -> 'a'
                    "textual assistive features" -> 'b'
                    "visual assistive features" -> 'c'
                    "auditory assistive features" -> 'd'
                    "tactile assistive features" -> 'e'
                    else -> '9'
                }
                addSubfieldIfExists(builder, name, note.stringValue().substringAfter(": "))
            }

            addSourceSubfieldIfExists(builder, it.source ?: it.sourceId)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<AccessibilityNoteData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?caId ?appliesTo ?sourceId ?source
            WHERE {
                ?id     bf:contentAccessibility ?caId .
                ?caId   rdf:type                bf:ContentAccessibility .
                OPTIONAL {
                    ?caId           bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
                OPTIONAL {
                    ?caId           bf:source   ?sourceId .
                    ?sourceId       rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code     ?source .
                    }
                }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map {
                val caId = it.getValue("caId")

                val noteQueryString = """
                    PREFIX bf: <$BIBFRAME_ONTOLOGY>
                    PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
                    SELECT ?note
                    WHERE {
                        ?caId   rdf:type    bf:ContentAccessibility ;
                                rdfs:label  ?note .
                    }
                """.trimIndent()

                val noteQuery = conn.prepareTupleQuery(noteQueryString)
                noteQuery.setBinding("caId", caId)

                val notes = noteQuery.evaluate().use { noteResult ->
                    noteResult.map { note -> note.getValue("note") }.toList()
                }

                AccessibilityNoteData(notes, it.getValue("appliesTo"), it.getValue("sourceId"), it.getValue("source"))
            }.toList()
        }
    }

    @JvmRecord
    private data class AccessibilityNoteData(val notes: List<Value>, val appliesTo: Value?, val sourceId: Value?, val source: Value?)
}