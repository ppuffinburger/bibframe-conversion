package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.apache.commons.lang3.StringUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord
import java.util.function.Consumer

internal class BibframeToField072Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "072"
                indicator2 = '7'
            }
            val code = it.code.stringValue()

            if (code.contains(' ')) {
                addSubfieldIfExists(builder, 'a', code.substringBefore(' '))
                addSubfieldIfExists(builder, 'x', code.substringAfter(' '))
            } else {
                addSubfieldIfExists(builder, 'a', code)
            }

            val source = if (it.source.isLiteral) it.source.stringValue() else TextUtils.getCodeStringFromUrl(it.source.stringValue())
            if (source == "agricola") {
                builder.indicator2 = '0'
            }
            addSubfieldIfExists(builder, '2', source)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<SubjectCategoryCode> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?code ?source ?sourceId
            WHERE {
                ?id         bf:subject  ?subjectId .
                ?subjectId  rdf:type    bf:Topic ;
                            bf:code     ?code ;
                            bf:source   ?sourceId .
                ?sourceId   rdf:type    bf:Source .
                OPTIONAL {
                    ?sourceId bf:code ?source
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { SubjectCategoryCode(it.getValue("code"), if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId")) }.toList()
        }
    }

    @JvmRecord
    private data class SubjectCategoryCode(val code: Value, val source: Value)
}