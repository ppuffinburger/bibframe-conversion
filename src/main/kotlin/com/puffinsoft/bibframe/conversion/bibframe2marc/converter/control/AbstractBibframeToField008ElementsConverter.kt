package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.FormOfItemLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.GovernmentPublicationLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.NatureOfContentsLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.TargetAudienceLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection

internal abstract class AbstractBibframeToField008ElementsConverter : BibframeToMarcConverter {
    abstract fun convert(conn: RepositoryConnection, workData: WorkData): String

    protected fun getTargetAudience(conn: RepositoryConnection, workId: Value): Char {
        return queryIntendedAudience(conn, workId)?.let { TargetAudienceLookup.lookup(it) ?: ' ' } ?: ' '
    }

    protected fun getFormOfItem(conn: RepositoryConnection, workData: WorkData): Char {
        return if (isLargePrint(conn, workData)) {
            'd'
        } else if (isBraille(conn, workData)) {
            'f'
        } else {
            getCarrier(conn, workData)
        }
    }

    protected fun getIndex(conn: RepositoryConnection, workId: Value): Char {
        if (queryIsIndex(conn, workId)) {
            return '1'
        }
        return '0'
    }

    protected fun getNatureOfContents(supplementaryContent: Set<Value>, genreForms: Set<Value>): String {
        val foundSupplementaryContent = supplementaryContent.mapNotNull { NatureOfContentsLookup.lookup(it) }
        val foundGenreForms = genreForms.mapNotNull { NatureOfContentsLookup.lookup(it) }

        return (foundSupplementaryContent.joinToString("") + foundGenreForms.joinToString("")).padEnd(4, ' ').substring(0..3)
    }

    protected fun getGovernmentPublication(conn: RepositoryConnection, workId: Value): Char {
        return queryGovernmentPubType(conn, workId)?.let { GovernmentPublicationLookup.lookup(it) } ?: ' '
    }

    protected fun getConferencePublication(conn: RepositoryConnection, workId: Value): Char {
        if (queryIsConferencePublication(conn, workId)) {
            return '1'
        }
        return '0'
    }

    private fun isLargePrint(conn: RepositoryConnection, workData: WorkData): Boolean {
        return queryIsLargePrint(conn, workData.primaryInstanceId)
    }

    private fun isBraille(conn: RepositoryConnection, workData: WorkData): Boolean {
        return queryIsBraille(conn, workData.primaryInstanceId)
    }

    private fun getCarrier(conn: RepositoryConnection, workData: WorkData): Char {
        return queryCarrier(conn, workData.primaryInstanceId).firstNotNullOfOrNull { FormOfItemLookup.lookup(it) } ?: ' '
    }

    private fun queryIsLargePrint(conn: RepositoryConnection, instanceId: Value): Boolean {
        val largePrintUri = lookupFontSizeUri("lp")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                ?instanceId         bf:fontSize <$largePrintUri> .
                <$largePrintUri>    rdf:type    bf:FontSize .
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("instanceId", instanceId)

        return query.evaluate()
    }

    private fun queryIsBraille(conn: RepositoryConnection, instanceId: Value): Boolean {
        val brailleCodeUri = lookupTactileNotationUri("brail")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                ?instanceId         bf:notation <$brailleCodeUri> .
                <$brailleCodeUri>   rdf:type    bf:TactileNotation .
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("instanceId", instanceId)

        return query.evaluate()
    }

    private fun queryCarrier(conn: RepositoryConnection, instanceId: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?carrier
            WHERE {
                ?instanceId bf:carrier  ?carrier .
                ?carrier    rdf:type    bf:Carrier .
                FILTER(ISIRI(?carrier))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("instanceId", instanceId)

        query.evaluate().use { result ->
            return result.map { it.getValue("carrier") }.toList()
        }
    }

    private fun queryIntendedAudience(conn: RepositoryConnection, workId: Value): Value? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?intendedAudience
            WHERE {
                ?workId             bf:intendedAudience ?intendedAudience .
                ?intendedAudience   rdf:type            bf:IntendedAudience .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("intendedAudience") }.map { it.getValue("intendedAudience") }.firstOrNull()
        }
    }

    private fun queryGovernmentPubType(conn: RepositoryConnection, workId: Value): Value? {
        val queryString = """
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?governmentPubType
            WHERE {
                ?workId            bflc:governmentPubType ?governmentPubType .
                ?governmentPubType rdf:type               bflc:GovernmentPubType .
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.map { it.getValue("governmentPubType") }.firstOrNull()
        }
    }

    private fun queryIsConferencePublication(conn: RepositoryConnection, workId: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                ?workId                                                 bf:genreForm    <http://id.loc.gov/authorities/genreForms/gf2014026068> .
                <http://id.loc.gov/authorities/genreForms/gf2014026068> rdf:type        bf:GenreForm .
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("workId", workId)

        return query.evaluate()
    }

    private fun queryIsIndex(conn: RepositoryConnection, workId: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                ?workId                                             bf:supplementaryContent <http://id.loc.gov/vocabulary/msupplcont/index> .
                <http://id.loc.gov/vocabulary/msupplcont/index>     rdf:type                bf:SupplementaryContent .
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("workId", workId)

        return query.evaluate()
    }
}