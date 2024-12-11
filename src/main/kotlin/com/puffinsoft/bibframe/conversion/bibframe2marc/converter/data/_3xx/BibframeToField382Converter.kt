package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.MarcKeyUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.SingleIndicatorConfig
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField382Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val dataField = MarcKeyUtils.parseReadMarc382(it.readMarc382.stringValue(), SingleIndicatorConfig(SingleIndicatorConfig.IndicatorPlacement.INDICATOR_2)).apply {
                indicator1 = if (it.partial) '1' else '0'
            }.build()

            if (it.appliesTo != null) {
                insertSubfieldIfExists(dataField, 0, '3', it.appliesTo)
            }

            record.dataFields.add(dataField)
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<MusicMediumData> {
        val partialUri = lookupStatusCodeUri("part")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?partial ?readMarc382 ?appliesTo
            WHERE {
                ?id             bf:musicMedium      ?musicMediumId .
                ?musicMediumId  rdf:type            bf:MusicMedium ;
                                bflc:readMarc382    ?readMarc382 .
                OPTIONAL {
                    SELECT (?statusId AS ?partial)
                    WHERE {
                        ?musicMediumId  bf:status   ?statusId .
                        ?statusId       rdf:type    bf:Status .
                        FILTER(?statusId = <$partialUri>)
                    }
                }
                OPTIONAL {
                    ?musicMediumId  bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { MusicMediumData(it.hasBinding("partial"), it.getValue("readMarc382"), it.getValue("appliesTo")) }.toList()
        }
    }

    @JvmRecord
    private data class MusicMediumData(val partial: Boolean, val readMarc382: Value, val appliesTo: Value?)
}