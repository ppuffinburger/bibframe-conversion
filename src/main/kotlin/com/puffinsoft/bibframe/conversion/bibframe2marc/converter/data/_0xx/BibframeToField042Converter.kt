package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField042Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData)?.let {
            record.dataFields.add(DataField("042", subfields = mutableListOf(Subfield('a', TextUtils.getCodeStringFromUrl(it.stringValue())))))
        }
    }

    private fun query(conn: RepositoryConnection, workData: WorkData): Value? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?descriptionAuthentication
            WHERE {
                ?id                         bf:adminMetadata                ?adminMetadataId .
                ?adminMetadataId            bf:descriptionAuthentication    ?descriptionAuthentication .
                ?descriptionAuthentication  rdf:type                        bf:DescriptionAuthentication .
                FILTER(?id IN (${workData.getWorkAndAllInstanceIdsAsStrings().joinToString { "<$it>" }}))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("descriptionAuthentication") }.map { it.getValue("descriptionAuthentication") }.firstOrNull()
        }
    }
}