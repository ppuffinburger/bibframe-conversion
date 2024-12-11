package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.IllustrationLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal object BibframeToField008ElementsBookConverter : AbstractBibframeToField008ElementsConverter() {
    private val LITERARY_FORM_FICTION_GENRES = setOf(
        "http://id.loc.gov/authorities/genreForms/gf2014026339",
        "http://id.loc.gov/authorities/genreForms/gf2014026094",
        "http://id.loc.gov/authorities/genreForms/gf2015026020",
        "http://id.loc.gov/authorities/genreForms/gf2014026110",
        "http://id.loc.gov/authorities/genreForms/gf2014026141",
        "http://id.loc.gov/authorities/genreForms/gf2014026542",
        "http://id.loc.gov/authorities/genreForms/gf2014026481",
        "http://id.loc.gov/authorities/genreForms/gf2011026363"
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData): String {
        with(StringBuilder()) {
            append(getIllustrations(conn, workData.workId).padEnd(4, ' '))
            append(getTargetAudience(conn, workData.workId))
            append(getFormOfItem(conn, workData))
            append(getNatureOfContents(workData.supplementaryContent, workData.workGenreForms))
            append(getGovernmentPublication(conn, workData.workId))
            append(getConferencePublication(conn, workData.workId))
            append(getFestschrift(conn, workData.workId))
            append(getIndex(conn, workData.workId))
            append('|')

            val genreForms = workData.workGenreForms.map { it.stringValue() }.toSet()

            append(getLiteraryForm(genreForms))
            append(getBiography(genreForms))

            return toString()
        }
    }

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        TODO("Not implemented")
    }

    private fun getIllustrations(conn: RepositoryConnection, workId: Value): String {
        return queryIllustrativeContent(conn, workId).mapNotNull { IllustrationLookup.lookup(it) }.joinToString(separator = "")
    }

    private fun getFestschrift(conn: RepositoryConnection, workId: Value): Char {
        if (queryIsFestschrift(conn, workId)) {
            return '1'
        }
        return '0'
    }

    private fun getLiteraryForm(genreForms: Set<String>): Char {
        return if (LITERARY_FORM_FICTION_GENRES.any { it in genreForms }) {
            '1'
        } else {
            '0'
        }
    }

    private fun getBiography(genreForms: Set<String>): Char {
        if (genreForms.contains("http://id.loc.gov/authorities/genreForms/gf2014026047")) {
            return 'a'
        }
        if (genreForms.contains("http://id.loc.gov/authorities/genreForms/gf2014026049")) {
            return 'd'
        }
        return '|'
    }

    private fun queryIllustrativeContent(conn: RepositoryConnection, workId: Value): Set<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?illustration
            WHERE {
                ?workId         bf:illustrativeContent  ?illustration .
                ?illustration   rdf:type                bf:Illustration .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("illustration") }.map { it.getValue("illustration") }.toSet()
        }
    }

    private fun queryIsFestschrift(conn: RepositoryConnection, workId: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                ?workId                                                 bf:genreForm    <http://id.loc.gov/authorities/genreForms/gf2016026082> .
                <http://id.loc.gov/authorities/genreForms/gf2016026082> rdf:type        bf:GenreForm .
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("workId", workId)

        return query.evaluate()
    }
}