package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.DateUtils
import kotlinx.datetime.LocalDateTime
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.ControlField
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField005Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        val changeDate = queryChangeDate(conn, workData)
        if (changeDate == null) {
            record.controlFields.add(ControlField("005", DateUtils.format005(workData.creationDate)))
        } else {
            record.controlFields.add(ControlField("005", DateUtils.format005(LocalDateTime.parse(changeDate.stringValue()))))
        }
    }

    private fun queryChangeDate(conn: RepositoryConnection, workData: WorkData): Value? {
        val idFilter = workData.getWorkAndAllInstanceIdsAsStrings().joinToString { "<$it>" }
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?changeDate ?changeStatusDate
            WHERE {
                {
                    ?id                 bf:adminMetadata    ?adminMetadataId .
                    ?adminMetadataId    bf:changeDate       ?changeDate .
                    FILTER(?id IN ($idFilter))
                }
                UNION
                {
                    ?id                                         bf:adminMetadata    ?adminMetadataId .
                    ?adminMetadataId                            bf:status           <http://id.loc.gov/vocabulary/mstatus/c> .
                    <http://id.loc.gov/vocabulary/mstatus/c>    rdf:type            bf:Status .
                    ?adminMetadataId                            bf:date             ?changeStatusDate .
                    FILTER(?id IN ($idFilter))
                    FILTER NOT EXISTS { ?adminMetadataId    bf:generationProcess    [] }
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("changeDate") || it.hasBinding("changeStatusDate") }
                .map { if (it.hasBinding("changeDate")) it.getValue("changeDate") else it.getValue("changeStatusDate") }
                .lastOrNull()
        }
    }
}