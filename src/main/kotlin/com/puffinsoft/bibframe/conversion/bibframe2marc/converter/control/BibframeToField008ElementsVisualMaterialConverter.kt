package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.MovingImageTechniqueLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.TypeOfVisualMaterialLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord
import kotlin.time.Duration

internal object BibframeToField008ElementsVisualMaterialConverter : AbstractBibframeToField008ElementsConverter() {
    override fun convert(conn: RepositoryConnection, workData: WorkData): String {
       with(StringBuilder()) {
           append(getRuntime(conn, workData.workId))
           append(' ')
           append(getTargetAudience(conn, workData.workId))
           append("     ")
           append(getGovernmentPublication(conn, workData.workId))
           append(getFormOfItem(conn, workData))
           append("   ")
           append(getTypeOfVisualMaterial(workData.workGenreForms))
           append(getTechnique(conn, workData.workId))

           return toString()
       }
    }

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        TODO("Not implemented")
    }

    private fun getTypeOfVisualMaterial(genreForms: Set<Value>): Char {
        return genreForms.firstNotNullOfOrNull { TypeOfVisualMaterialLookup.lookup(it) } ?: '|'
    }

    private fun getRuntime(conn: RepositoryConnection, workId: Value): String {
        // TODO : Always in minutes?
        return queryDuration(conn, workId)?.let { Duration.parse(it.stringValue().substringAfterLast(" ")).inWholeMinutes.toString().padStart(3, '0') } ?: "000"
    }

    private fun getTechnique(conn: RepositoryConnection, workId: Value): Char {
        return queryMovingImageTechnique(conn, workId)?.let { MovingImageTechniqueLookup.lookup(it) } ?: '|'
    }

    private fun queryDuration(conn: RepositoryConnection, workId: Value): Value? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?duration
            WHERE {
                ?workId bf:duration ?duration .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("duration") }.map { it.getValue("duration") }.firstOrNull()
        }
    }

    private fun queryMovingImageTechnique(conn: RepositoryConnection, workId: Value): Value? {
        val queryString = """
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?movingImageTechnique
            WHERE {
                ?workId                 bflc:movingImageTechnique   ?movingImageTechnique .
                ?movingImageTechnique   rdf:type                    bflc:MovingImageTechnique .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("movingImageTechnique") }.map { it.getValue("movingImageTechnique") }.firstOrNull()
        }
    }
}