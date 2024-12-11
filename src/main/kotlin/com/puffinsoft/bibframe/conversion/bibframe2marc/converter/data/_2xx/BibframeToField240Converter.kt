package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.MarcKeyUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.SingleIndicatorConfig
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField240Converter : BibframeToMarcConverter {
    // TODO : need more examples for other parts of the field when no marcKey which should be what 130 also does.
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        if (containsPrimaryContribution(conn, workData.workId)) {
            if (containsMarcKey(conn, workData.workId)) {
                queryWithMarcKey(conn, workData.workId).also { data ->
                    // the marcKey may not be the correct data, so we will rebuild the field
                    val marcKeyField: DataField = MarcKeyUtils.parseMarcKey(data.marcKey.stringValue(), SingleIndicatorConfig(SingleIndicatorConfig.IndicatorPlacement.IGNORE)).build()
                    if (marcKeyField.tag == "240") {
                        // if the marcKey is using 240 as a tag
                        record.dataFields.add(marcKeyField)
                    } else if (marcKeyField.tag.startsWith('1')) {
                        // if the marcKey tag is in the 1XX range then change to 240
                        val builder = DataFieldBuilder().apply {
                            tag = "240"
                            indicator1 = '1'
                            indicator2 = '0'
                        }

                        // skip fields up to the $t
                        marcKeyField.subfields.dropWhile { it.name != 't' }.withIndex().forEach {
                            if (it.index == 0) {
                                addSubfieldIfExists(builder, 'a', it.value.data)
                            } else {
                                addSubfieldIfExists(builder, it.value.name, it.value.data)
                            }
                        }

                        if (data.expressionOfId.isIRI) {
                            addSubfieldIfExists(builder, '1', data.expressionOfId)
                        }

                        record.dataFields.add(builder.build())
                    }
                }
            } else {
                queryWithoutMarcKey(conn, workData.workId)?.let {
                    val builder = DataFieldBuilder().apply {
                        tag = "240"
                        indicator1 = getNonFilingCharacters(it.nonSortNum)
                    }

                    addSubfieldIfExists(builder, 'a', it.mainTitle)
                    addSubfieldIfExists(builder, 'n', it.partNumber)
                    addSubfieldIfExists(builder, 'p', it.partName)

                    if (it.expressionOfId.isIRI) {
                        addSubfieldIfExists(builder, '1', it.expressionOfId)
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
                ?id             bf:expressionOf ?expressionOfId .
                ?expressionOfId rdf:type        bf:Hub ;
                                bf:contribution ?contributionId .
                ?contributionId rdf:type        bf:PrimaryContribution .
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
    private data class UniformTitleWithMarcKey(val expressionOfId: Value, val marcKey: Value)

    @JvmRecord
    private data class UniformTitleWithoutMarcKey(val expressionOfId: Value, val mainTitle: Value, val nonSortNum: Value?, val partNumber: Value?, val partName: Value?)
}