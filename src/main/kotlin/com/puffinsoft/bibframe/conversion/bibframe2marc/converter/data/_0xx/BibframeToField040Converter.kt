package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.OrganizationCodeLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.StatusCodeLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField040Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        val data = query(conn, workData)

        val assigner = if (data.assigner.isLiteral) data.assigner.stringValue() else OrganizationCodeLookup.lookup(data.assigner.stringValue())
        val language = data.language?.let { TextUtils.getCodeStringFromUrl(data.language.stringValue()) }
        val descriptionModifier = data.descriptionModifier?.let { OrganizationCodeLookup.lookup(data.descriptionModifier.stringValue()) }
        val internalNote = data.internalNote?.let { OrganizationCodeLookup.lookup(data.internalNote.stringValue()) }

        val builder = DataFieldBuilder().apply { tag = "040" }

        addSubfieldIfExists(builder, 'a', assigner)
        addSubfieldIfExists(builder, 'b', language)
        addSubfieldIfExists(builder, 'c', assigner)

        if (data.descriptionConvention != null) {
            addSubfieldIfExists(builder, 'e', TextUtils.getCodeStringFromUrl(data.descriptionConvention.stringValue()))
        }

        addSubfieldIfExists(builder, 'd', descriptionModifier)
        addSubfieldIfExists(builder, 'd', internalNote)

        record.dataFields.add(builder.build())
    }

    private fun query(conn: RepositoryConnection, workData: WorkData): CatalogingSourceData {
        val aacrUri = lookupDescriptionConventionUri("aacr")
        val isbdUri = lookupDescriptionConventionUri("isbd")
        val noteUri = lookupNoteTypeUri("internal")
        val ids = workData.getWorkAndAllInstanceIdsAsStrings().joinToString { "<$it>" }

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?assigner ?language ?descriptionConvention ?descriptionModifier ?internalNote
            WHERE {
                {
                    SELECT ?assigner
                    WHERE {
                        {
                            ?id                 bf:adminMetadata    ?adminMetadataId .
                            ?adminMetadataId    bf:assigner         ?assignerId .
                            ?assignerId         rdf:type            bf:Agent ;
                                                bf:code             ?assigner .
                        }
                        UNION
                        {
                            ?id                 bf:adminMetadata    ?adminMetadataId .
                            ?adminMetadataId    bf:assigner         ?assigner .
                            ?assigner           rdf:type            bf:Organization .
                        }
                        UNION
                        {
                            ?id                 bf:adminMetadata    ?adminMetadataId .
                            ?adminMetadataId    bf:status           <${StatusCodeLookup.lookup("n")?.uri}> ;
                                                bf:agent            ?agentId .
                            ?agentId            bf:code             ?assigner .
                        }
                        FILTER(?id IN ($ids))
                    } LIMIT 1
                }
                OPTIONAL {
                    ?id                 bf:adminMetadata        ?adminMetadataId .
                    ?adminMetadataId    bf:descriptionLanguage  ?language .
                    ?language           rdf:type                bf:Language .
                    FILTER(?id IN ($ids))
                }
                OPTIONAL {
                    SELECT ?descriptionConvention
                    WHERE {
                        ?id                     bf:adminMetadata            ?adminMetadataId .
                        ?adminMetadataId        bf:descriptionConventions   ?descriptionConvention .
                        ?descriptionConvention  rdf:type                    bf:DescriptionConventions .
                        FILTER(?descriptionConvention NOT IN (<$aacrUri>, <$isbdUri>))
                        FILTER(?id IN ($ids))
                    }
                    LIMIT 1
                }
                OPTIONAL {
                    ?id                     bf:adminMetadata        ?adminMetadataId .
                    ?adminMetadataId        bf:descriptionModifier  ?descriptionModifier .
                    ?descriptionModifier    rdf:type                bf:Organization .
                    FILTER(?id IN ($ids))
                }
                OPTIONAL {
                    ?id                 bf:adminMetadata        ?adminMetadataId .
                    ?adminMetadataId    bf:note                 ?internalNoteId .
                    ?internalNoteId     rdf:type                bf:Note .
                    ?internalNoteId     rdfs:label              ?internalNote .
                    FILTER(?internalNoteId = <$noteUri>)
                    FILTER(?id IN ($ids))
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("assigner") }  // really only care that we can populate $a and $c
                .map { CatalogingSourceData(it.getValue("assigner"), it.getValue("language"), it.getValue("descriptionConvention"), it.getValue("descriptionModifier"), it.getValue("internalNote")) }
                .firstOrNull() ?: throw RuntimeException("No assigner found for 040 in the Admin Metadata")
        }
    }

    @JvmRecord
    private data class CatalogingSourceData(val assigner: Value, val language: Value?, val descriptionConvention: Value?, val descriptionModifier: Value?, val internalNote: Value?)
}