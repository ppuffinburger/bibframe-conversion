package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.AccompanyingMatterLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.FormOfCompositionLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.FormatOfMusicLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.LiteraryTextForSoundRecordingsLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal object BibframeToField008ElementsMusicConverter : AbstractBibframeToField008ElementsConverter() {
    override fun convert(conn: RepositoryConnection, workData: WorkData): String {
        with(StringBuilder()) {
            append(getFormOfComposition(workData.workGenreForms))
            append(getFormatOfMusic(conn, workData.workId))
            append('|')
            append(getTargetAudience(conn, workData.workId))
            append(getFormOfItem(conn, workData))
            append(getAccompanyingMatter(workData.supplementaryContent).padEnd(6, ' '))
            append(getLiteraryTextForSoundRecordings(workData.workGenreForms).padEnd(2, ' '))
            append("|||")

            return toString()
        }
    }

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        TODO("Not implemented")
    }

    private fun getFormOfComposition(genreForms: Set<Value>): String {
        return genreForms.firstNotNullOfOrNull { FormOfCompositionLookup.lookup(it) } ?: "||"
    }

    private fun getFormatOfMusic(conn: RepositoryConnection, workId: Value): Char {
        return queryMusicFormat(conn, workId)?.let { FormatOfMusicLookup.lookup(it) } ?: '|'
    }

    private fun getAccompanyingMatter(supplementaryContent: Set<Value>): String {
        return supplementaryContent.mapNotNull { AccompanyingMatterLookup.lookup(it) }.joinToString(separator = "")
    }

    private fun getLiteraryTextForSoundRecordings(genreForms: Set<Value>): String {
        return genreForms.mapNotNull { LiteraryTextForSoundRecordingsLookup.lookup(it) }.joinToString(separator = "")
    }

    private fun queryMusicFormat(conn: RepositoryConnection, workId: Value): Value? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?musicFormat
            WHERE {
                ?workId         bf:musicFormat  ?musicFormat .
                ?musicFormat    rdf:type        bf:MusicFormat .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("musicFormat") }.map { it.getValue("musicFormat") }.firstOrNull()
        }
    }
}