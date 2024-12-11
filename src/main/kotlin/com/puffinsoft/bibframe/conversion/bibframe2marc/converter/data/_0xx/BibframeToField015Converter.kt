package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField015Converter : BibframeToMarcConverter {
    // TODO : do we do a 015 for every Instance?  Do we want to try to make more single fields if they have the same source?
    //  Do not have an EXAMPLE record.
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply { tag = "015" }
            addSubfieldIfExists(builder, if (it.cancelled) 'z' else 'a', it.nbn)
            addSubfieldIfExists(builder, '2', TextUtils.getCodeStringFromUrl(it.source.stringValue()))
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<NationalBibliographyNumberData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?nbn ?cancelled ?source
            WHERE {
                ?id             bf:identifiedBy ?identifiedById .
                ?identifiedById rdf:type        bf:Nbn ;
                                rdf:value       ?nbn .
                OPTIONAL {
                    ?identifiedById bf:status  ?cancelled .
                    ?cancelled      rdf:type    bf:Status .
                }
                OPTIONAL {
                    ?identifiedById bf:source   ?source .
                    ?source         rdf:type    bf:Source .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { NationalBibliographyNumberData(it.getValue("nbn"), it.hasBinding("cancelled"), it.getValue("source")) }.toList()
        }
    }

    @JvmRecord
    private data class NationalBibliographyNumberData(val nbn: Value, val cancelled: Boolean, val source: Value)
}