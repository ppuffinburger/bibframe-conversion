package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.OrganizationCodeLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.ControlField
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField001Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        with(record.controlFields) {
            val identifiedByAndAssigner = queryIdentifiedByAndAssigner(conn, workData)

            add(ControlField("001", identifiedByAndAssigner.identifiedBy.stringValue()))
            add(ControlField("003", OrganizationCodeLookup.lookup(identifiedByAndAssigner.assigner.stringValue()) ?: throw RuntimeException("Could not find code during lookup")))
        }
    }

    private fun queryIdentifiedByAndAssigner(conn: RepositoryConnection, workData: WorkData): IdentifiedByAndAssigner {
        // Not filtering out source, because even though 001's are not required (because the control number could be in an 016), a lot of things would break without it.
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?identifiedByValue ?assignerValue
            WHERE {
                ?id                 bf:adminMetadata    ?adminMetadataId .
                ?adminMetadataId    bf:identifiedBy     ?identifiedById .
                ?identifiedById     rdf:type            bf:Local ;
                                    rdf:value           ?identifiedByValue ;
                                    bf:assigner         ?assignerValue .
                FILTER(?id IN (${workData.getWorkAndAllInstanceIdsAsStrings().joinToString { "<$it>" }}))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("identifiedByValue") && it.hasBinding("assignerValue") }
                .map { IdentifiedByAndAssigner(it.getValue("identifiedByValue"), it.getValue("assignerValue")) }
                .firstOrNull() ?: throw RuntimeException("No identifiedBy and/or assigner value found in the Admin Metadata")
        }
    }

    @JvmRecord
    private data class IdentifiedByAndAssigner(val identifiedBy: Value, val assigner: Value)
}