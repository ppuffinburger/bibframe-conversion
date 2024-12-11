package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField536Converter : BibframeToMarcConverter {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach { (_, values) ->
            val buckets = bucketNotes(values)
            val builder = DataFieldBuilder().apply { tag = "536" }

            buckets[InformationType.OTHER]?.forEach { addSubfieldIfExists(builder, 'a', it) }
            buckets[InformationType.CONTRACT]?.forEach { addSubfieldIfExists(builder, 'b', it) }
            buckets[InformationType.GRANT]?.forEach { addSubfieldIfExists(builder, 'c', it) }
            buckets[InformationType.PROGRAM_ELEMENT]?.forEach { addSubfieldIfExists(builder, 'e', it) }
            buckets[InformationType.PROJECT]?.forEach { addSubfieldIfExists(builder, 'f', it) }
            buckets[InformationType.TASK]?.forEach { addSubfieldIfExists(builder, 'g', it) }
            buckets[InformationType.WORK_UNIT]?.forEach { addSubfieldIfExists(builder, 'h', it) }

            record.dataFields.add(builder.build())
        }
    }

    private fun bucketNotes(notes: List<FundingInformationNoteData>): Map<InformationType, List<String>> {
        return notes.map { it.note.stringValue() }.groupBy { InformationType.fromString(it) }
    }

    private fun query(conn: RepositoryConnection, id: Value): Map<Value, List<FundingInformationNoteData>> {
        val fundingInfoUri = lookupNoteTypeUri("fundinfo")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?noteId ?note
            WHERE {
                ?id         bf:note     ?noteId .
                ?noteId     rdf:type    bf:Note ;
                            rdf:type    <$fundingInfoUri> ;
                            rdfs:label  ?note .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { FundingInformationNoteData(it.getValue("noteId"), it.getValue("note")) }.groupBy { it.noteId }
        }
    }

    @JvmRecord
    private data class FundingInformationNoteData(val noteId: Value, val note: Value)

    private enum class InformationType {
        CONTRACT,
        GRANT,
        PROGRAM_ELEMENT,
        PROJECT,
        TASK,
        WORK_UNIT,
        OTHER;

        companion object {
            fun fromString(note: String): InformationType {
                if (note.startsWith("Contract:", true)) {
                    return CONTRACT
                }
                if (note.startsWith("Grant:", true)) {
                    return GRANT
                }
                if (note.startsWith("Program element:", true)) {
                    return PROGRAM_ELEMENT
                }
                if (note.startsWith("Project:", true)) {
                    return PROJECT
                }
                if (note.startsWith("Task:", true)) {
                    return TASK
                }
                if (note.startsWith("Work unit:", true)) {
                    return WORK_UNIT
                }
                return OTHER
            }
        }
    }
}