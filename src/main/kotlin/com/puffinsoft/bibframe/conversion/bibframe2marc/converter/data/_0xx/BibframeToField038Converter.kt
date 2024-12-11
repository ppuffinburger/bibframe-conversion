package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField038Converter : BibframeToMarcConverter {
    // TODO : Not entirely sure, haven't seen an example
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData)?.let { record.dataFields.add(DataField("038", subfields = mutableListOf(Subfield('a', it.stringValue())))) }
    }

    private fun query(conn: RepositoryConnection, workData: WorkData): Value? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?metadataLicensor
            WHERE {
                ?id                 bf:adminMetadata        ?adminMetadataId .
                ?adminMetadataId    bflc:metadataLicensor   ?metadataLicensorId .
                ?metadataLicensorId rdf:type                bflc:MetadataLicensor ;
                                    rdfs:label              ?metadataLicensor .
                FILTER(?id IN (${workData.getWorkAndAllInstanceIdsAsStrings().joinToString { "<$it>" }}))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("metadataLicensor") }.map { it.getValue("metadataLicensor") }.firstOrNull()
        }
    }
}