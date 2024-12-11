package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.FrequencyLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.TypeOfContinuingResourceLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal object BibframeToField008ElementsContinuingResourceConverter : AbstractBibframeToField008ElementsConverter() {
    override fun convert(conn: RepositoryConnection, workData: WorkData): String {
        with(StringBuilder()) {
            append(getFrequency(workData.frequencies))
            append(getRegularity(workData.frequencies))
            append(' ')
            append(getTypeOfContinuingResource(conn, workData.workId))
            append('|')
            append(getFormOfItem(conn, workData))
            append(getNatureOfContents(workData.supplementaryContent, workData.workGenreForms))
            append(getGovernmentPublication(conn, workData.workId))
            append(getConferencePublication(conn, workData.workId))
            append("   ")
            append(getOriginalAlphabetOrScriptOfTitle(conn, workData.workId))
            append(getEntryConvention(conn, workData))

            return toString()
        }
    }

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        TODO("Not implemented")
    }

    private fun getFrequency(frequencies: Set<Value>): Char {
        return frequencies.firstNotNullOfOrNull { FrequencyLookup.lookup(it) } ?: '|'
    }

    private fun getRegularity(frequencies: Set<Value>): Char {
        return frequencies.filter { it.stringValue() == "http://id.loc.gov/vocabulary/frequencies/irr" }.map { 'n' }.firstOrNull() ?: 'r'
    }

    private fun getTypeOfContinuingResource(conn: RepositoryConnection, workId: Value): Char {
        return querySerialPubType(conn, workId)?.let { TypeOfContinuingResourceLookup.lookup(it) } ?: '|'
    }

    private fun getOriginalAlphabetOrScriptOfTitle(conn: RepositoryConnection, workId: Value): Char {
        return queryScriptNotation(conn, workId)?.let { TextUtils.getCodeCharFromUrl(it.stringValue()) } ?: ' '
    }

    private fun getEntryConvention(conn: RepositoryConnection, workData: WorkData): Char {
        return queryMetaEntryNote(conn, workData)?.let {
            when (it.stringValue()) {
                "0 - successive" -> {
                    return '0'
                }
                "1 - latest" -> {
                    return '1'
                }
                "2 - integrated" -> {
                    return '2'
                }
                else -> {
                    return '|'
                }
            }
        } ?: '|'
    }

    private fun queryMetaEntryNote(conn: RepositoryConnection, workData: WorkData): Value? {
        val metaentryUri = lookupNoteTypeUri("metaentry")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?metaEntryNote
            WHERE {
                ?id                 bf:adminMetadata    ?adminMetadataId .
                ?adminMetadataId    bf:note             ?noteId .
                ?noteId             rdf:type            bf:Note ;
                                    rdf:type            <$metaentryUri> ;
                                    rdfs:label          ?metaEntryNote .
                FILTER(?id IN (${workData.getWorkAndAllInstanceIdsAsStrings().joinToString { "<$it>" }}))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("metaEntryNote") }.map { it.getValue("metaEntryNote") }.firstOrNull()
        }
    }

    private fun querySerialPubType(conn: RepositoryConnection, workId: Value): Value? {
        val queryString = """
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?serialPubType
            WHERE {
                ?workId         bflc:serialPubType  ?serialPubType .
                ?serialPubType  rdf:type            bflc:SerialPubType .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("serialPubType") }.map { it.getValue("serialPubType") }.firstOrNull()
        }
    }

    private fun queryScriptNotation(conn: RepositoryConnection, workId: Value): Value? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?scriptNotation
            WHERE {
                ?workId         bf:notation ?scriptNotation .
                ?scriptNotation rdf:type    bf:Script .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("scriptNotation") }.map { it.getValue("scriptNotation") }.firstOrNull()
        }
    }
}