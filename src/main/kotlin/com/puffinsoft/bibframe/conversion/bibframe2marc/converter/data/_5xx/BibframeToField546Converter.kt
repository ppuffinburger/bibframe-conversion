package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField546Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "546" }
            if (it.notationAppliesTo != null || it.noteAppliesTo != null) {
                if (it.notationAppliesTo != null) {
                    addSubfieldIfExists(builder, '3', it.notationAppliesTo)
                } else {
                    addSubfieldIfExists(builder, '3', it.noteAppliesTo)
                }
            }
            addSubfieldIfExists(builder, 'a', it.note)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<LanguageNoteData> {
        val languageUri = lookupNoteTypeUri("lang")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT DISTINCT ?notation ?notationAppliesTo ?note ?noteAppliesTo
            WHERE {
                OPTIONAL {
                    ?id         bf:notation             ?notationId .
                    ?notationId rdf:type                bf:Notation ;
                                rdfs:label              ?notation .
                    OPTIONAL {
                        ?notationId             bflc:appliesTo  ?notationAppliesToId .
                        ?notationAppliesToId    rdf:type        bflc:AppliesTo ;
                                                rdfs:label      ?notationAppliesTo .
                    }
                }
                OPTIONAL {
                    ?id         bf:note     ?noteId .
                    ?noteId     rdf:type    bf:Note ;
                                rdf:type    <$languageUri> ;
                                rdfs:label  ?note .
                    OPTIONAL {
                        ?noteId     bflc:appliesTo  ?noteId .
                        ?noteId     rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?noteAppliesTo .
                    }
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("notation") || it.hasBinding("note") }
                .map { LanguageNoteData(it.getValue("notation"), it.getValue("notationAppliesTo"), it.getValue("note"), it.getValue("noteAppliesTo")) }
                .toList()
        }
    }

    @JvmRecord
    private data class LanguageNoteData(val notation: Value?, val notationAppliesTo: Value?, val note: Value?, val noteAppliesTo: Value?)
}