package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.ReliefLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.SpecialFormatCharacteristicsLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal object BibframeToField008ElementsMapConverter : AbstractBibframeToField008ElementsConverter() {
    override fun convert(conn: RepositoryConnection, workData: WorkData): String {
        with(StringBuilder()) {
            append(getRelief(conn, workData.workId).padEnd(4, ' '))
            append(getProjection(conn, workData.workId))
            append(" |  ")
            append(getGovernmentPublication(conn, workData.workId))
            append(getFormOfItem(conn, workData))
            append(' ')
            append(getIndex(conn, workData.workId))
            append(' ')
            append(getSpecialFormatCharacteristics(workData.workGenreForms).padEnd(2, ' '))

            return toString()
        }
    }

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        TODO("Not implemented")
    }

    private fun getRelief(conn: RepositoryConnection, workId: Value): String {
        return queryRelief(conn, workId).mapNotNull { ReliefLookup.lookup(it) }.joinToString(separator = "")
    }

    private fun getProjection(conn: RepositoryConnection, workId: Value): String {
        return queryProjection(conn, workId)?.let { TextUtils.getCodeStringFromUrl(it.stringValue()) } ?: "  "
    }

    private fun getSpecialFormatCharacteristics(genreForms: Set<Value>): String {
        return genreForms.mapNotNull { SpecialFormatCharacteristicsLookup.lookup(it) }.joinToString(separator = "")
    }

    private fun queryRelief(conn: RepositoryConnection, workId: Value): Set<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?relief
            WHERE {
                ?workId         bf:cartographicAttributes   ?cartographicId .
                ?cartographicId rdf:type                    bf:Cartographic ;
                                bf:relief                   ?relief .
                ?relief         rdf:type                    bf:Relief .
                FILTER(ISIRI(?relief))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("relief") }.map { it.getValue("relief") }.toSet()
        }
    }

    private fun queryProjection(conn: RepositoryConnection, workId: Value): Value? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?projection
            WHERE {
                ?workId         bf:cartographicAttributes   ?cartographicId .
                ?cartographicId rdf:type                    bf:Cartographic ;
                                bf:projection               ?projection .
                ?projection     rdf:type                    bf:Projection .
                FILTER(ISIRI(?projection))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("projection") }.map { it.getValue("projection") }.firstOrNull()
        }
    }
}