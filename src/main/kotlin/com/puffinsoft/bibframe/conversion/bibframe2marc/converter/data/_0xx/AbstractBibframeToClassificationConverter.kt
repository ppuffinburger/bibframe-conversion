package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal abstract class AbstractBibframeToClassificationConverter : BibframeToMarcConverter {
    fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord, tag: String, classificationType: String, assignerFilter: Value, useAssignerFilter: Boolean) {
        query(conn, workData.workId, classificationType, assignerFilter, useAssignerFilter).forEach {
            val builder = DataFieldBuilder().apply { this.tag = tag }
            if (assignerFilter.stringValue() == it.assigner?.stringValue()) {
                when(it.status.stringValue().substringAfterLast('/')) {
                    "uba" -> {
                        builder.indicator1 = '0'
                        builder.indicator2 = '0'
                    }
                    "nuba" -> {
                        builder.indicator1 = '1'
                        builder.indicator2 = '0'
                    }
                    else -> {
                        builder.indicator1 = '1'
                        builder.indicator2 = '4'
                    }
                }
            } else {
                builder.indicator1 = '1'
                builder.indicator2 = '4'
            }

            addSubfieldIfExists(builder, 'a', it.classificationPortion)
            addSubfieldIfExists(builder, 'b', it.itemPortion)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value, classificationType: String, assignerFilter: Value, useAssignerFilter: Boolean): List<Classification> {
        val queryString = buildQueryString(classificationType, useAssignerFilter)

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        if (useAssignerFilter) {
            query.setBinding("assignerFilter", assignerFilter)
        }

        query.evaluate().use { result ->
            return result.map { Classification(it.getValue("status"), it.getValue("classificationPortion"), it.getValue("itemPortion"), it.getValue("assigner")) }.toList()
        }
    }

    private fun buildQueryString(classificationType: String, useAssignerFilter: Boolean): String {
        return if (useAssignerFilter) {
            """
                PREFIX bf: <$BIBFRAME_ONTOLOGY>
                SELECT ?status ?classificationPortion ?itemPortion ?assigner
                WHERE {
                    ?id                 bf:classification           ?classificationId .
                    ?classificationId   rdf:type                    bf:$classificationType ;
                                        bf:classificationPortion    ?classificationPortion ;
                                        bf:assigner                 ?assigner ;
                                        bf:status                   ?status .
                    ?status             rdf:type                    bf:Status .
                    OPTIONAL {
                        ?classificationId bf:itemPortion ?itemPortion .
                    }
                    FILTER(?assigner = ?assignerFilter)
                }
            """.trimIndent()
        } else {
            """
                PREFIX bf: <$BIBFRAME_ONTOLOGY>
                SELECT ?status ?classificationPortion ?itemPortion ?assigner
                WHERE {
                    ?id                 bf:classification           ?classificationId .
                    ?classificationId   rdf:type                    bf:$classificationType ;
                                        bf:classificationPortion    ?classificationPortion ;
                                        bf:status                   ?status .
                    ?status             rdf:type                    bf:Status .
                    OPTIONAL {
                        ?classificationId bf:itemPortion ?itemPortion .
                    }
                    OPTIONAL {
                        ?classificationId bf:assigner ?assigner .
                    }
                }
            """.trimIndent()
        }
    }

    @JvmRecord
    private data class Classification(val status: Value, val classificationPortion: Value, val itemPortion: Value?, val assigner: Value?)
}