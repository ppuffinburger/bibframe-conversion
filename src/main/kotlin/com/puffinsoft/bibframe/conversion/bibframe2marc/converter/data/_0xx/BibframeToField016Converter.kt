package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.OrganizationCodeLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField016Converter : BibframeToMarcConverter {
    // TODO : Do not have an EXAMPLE record.
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData)?.let {
            val builder = DataFieldBuilder().apply { tag = "016" }

            addSubfieldIfExists(builder, if (it.cancelled) 'z' else 'a', it.nbacn)

            // if Library and Archives Canada
            if (it.source.stringValue() == "http://id.loc.gov/authorities/names/no2004037399") {
                builder.subfields {
                    subfield {
                        name = '2'
                        data = "CaOONL"
                    }
                }
            } else {
                val code = OrganizationCodeLookup.lookup(it.source.stringValue())
                builder.apply {
                    indicator1 = '7'
                    subfields {
                        subfield {
                            name = '2'
                            data = code ?: TextUtils.getCodeStringFromUrl(it.source.stringValue())
                        }
                    }
                }
            }

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, workData: WorkData): NationalBibliographicAgencyControlNumberData? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?nbacn ?cancelled ?source
            WHERE {
                {
                    ?id                 bf:adminMetadata    ?adminMetadataId .
                    ?adminMetadataId    bf:identifiedBy     ?identifiedById .
                    ?identifiedById     rdf:type            bf:Local ;
                                        rdf:value           ?nbacn ;
                                        bf:source           ?source .
                    ?source             rdf:type            bf:Source .
                    FILTER(?id IN (${workData.getWorkAndAllInstanceIdsAsStrings().joinToString { "<$it>" }}))
                }
                OPTIONAL {
                    ?identifiedById bf:status  ?cancelled .
                    ?cancelled      rdf:type    bf:Status .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)

        query.evaluate().use { result ->
            return result.map { NationalBibliographicAgencyControlNumberData(it.getValue("nbacn"), it.hasBinding("cancelled"), it.getValue("source")) }.firstOrNull()
        }
    }

    @JvmRecord
    private data class NationalBibliographicAgencyControlNumberData(val nbacn: Value, val cancelled: Boolean, val source: Value)
}