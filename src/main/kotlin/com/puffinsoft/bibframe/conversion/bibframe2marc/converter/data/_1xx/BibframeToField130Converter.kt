package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._1xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.MarcKeyUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.SingleIndicatorConfig
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField130Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        if (!containsPrimaryContribution(conn, workData.workId)) {
            if (containsMarcKey(conn, workData.workId)) {
                queryWithMarcKey(conn, workData.workId).let {
                    record.dataFields.add(MarcKeyUtils.parseMarcKey(it.marcKey.stringValue(), SingleIndicatorConfig(SingleIndicatorConfig.IndicatorPlacement.USE_DEFAULTS, '0', ' ')).build())
                }
            } else {
                queryWithoutMarcKey(conn, workData.workId)?.let {
                    val builder = DataFieldBuilder().apply {
                        tag = "130"
                        indicator1 = getNonFilingCharacters(it.nonSortNum)
                    }

                    addSubfieldIfExists(builder, 'a', it.mainTitle)
                    addSubfieldIfExists(builder, 'n', it.partNumber)
                    addSubfieldIfExists(builder, 'p', it.partName)

                    if (it.uri.isIRI) {
                        addSubfieldIfExists(builder, '1', it.uri)
                    }

                    record.dataFields.add(builder.build())
                }
            }
        }
    }

    private fun queryWithMarcKey(conn: RepositoryConnection, id: Value): UniformTitleWithMarcKey {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?expressionOfId ?marcKey
            WHERE {
                ?id             bf:expressionOf ?expressionOfId .
                ?expressionOfId rdf:type        bf:Hub ;
                                bflc:marcKey    ?marcKey .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { UniformTitleWithMarcKey(it.getValue("expressionOfId"), it.getValue("marcKey")) }.first()
        }
    }

    private fun queryWithoutMarcKey(conn: RepositoryConnection, id: Value): UniformTitleWithoutMarcKey? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?expressionOfId ?mainTitle ?nonSortNum ?partNumber ?partName
            WHERE {
                ?id             bf:expressionOf ?expressionOfId .
                ?expressionOfId rdf:type        bf:Hub ;
                                bf:title        ?titleId .
                ?titleId        rdf:type        bf:Title ;
                                bf:mainTitle    ?mainTitle .
                OPTIONAL {
                    ?titleId bflc:nonSortNum ?nonSortNum .
                }
                OPTIONAL {
                    ?titleId bf:partNumber ?partNumber .
                }
                OPTIONAL {
                    ?titleId bf:partName ?partName .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { UniformTitleWithoutMarcKey(it.getValue("expressionOfId"), it.getValue("mainTitle"), it.getValue("nonSortNum"), it.getValue("partNumber"), it.getValue("partName")) }.firstOrNull()
        }
    }

    private fun containsPrimaryContribution(conn: RepositoryConnection, id: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK
            WHERE {
                {
                    ?id             bf:expressionOf ?expressionOfId .
                    ?expressionOfId rdf:type        bf:Hub ;
                                    bf:contribution ?contributionId .
                    ?contributionId rdf:type        bf:PrimaryContribution .
                }
                UNION
                {
                    ?id             bf:contribution ?contributionId .
                    ?contributionId rdf:type        bf:PrimaryContribution .
                }
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("id", id)

        return query.evaluate()
    }

    private fun containsMarcKey(conn: RepositoryConnection, id: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            ASK
            WHERE {
                ?id             bf:expressionOf ?expressionOfId .
                ?expressionOfId rdf:type        bf:Hub ;
                                bflc:marcKey    ?marcKey .
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("id", id)

        return query.evaluate()
    }

    @JvmRecord
    private data class UniformTitleWithMarcKey(val uri: Value, val marcKey: Value)

    @JvmRecord
    private data class UniformTitleWithoutMarcKey(val uri: Value, val mainTitle: Value, val nonSortNum: Value?, val partNumber: Value?, val partName: Value?)
}