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

internal class BibframeToField035Converter : BibframeToMarcConverter {
    // TODO : all instances?
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply { tag = "035" }

            val subfieldBuilder = StringBuilder()
            if (it.assigner != null) {
                val org = OrganizationCodeLookup.lookup(it.assigner.stringValue()) ?: TextUtils.getCodeStringFromUrl(it.assigner.stringValue())
                subfieldBuilder.append("($org)")
            }
            subfieldBuilder.append(it.cn.stringValue())

            addSubfieldIfExists(builder, if (it.cancelled) 'z' else 'a', subfieldBuilder.toString())

            record.dataFields.add(builder.build())
        }
    }

    //identifiedBy - OclcNumber	035 - System Control Number (R); ind1=#; ind2=#
    //   rdf:value	   $a ; add "(OCoLC)" before number
    //   status - Status - "cancinv" or with URI "http://id.loc.gov/vocabulary/mstatus/cancinv"
    //      rdf:value	   $z ; add "(OCoLC)" before number
    private fun query(conn: RepositoryConnection, id: Value): List<SystemControlNumber> {
        val cancelledOrInvalidUri = lookupStatusCodeUri("cancinv")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?cn ?cancelled ?assigner
            WHERE {
                {
                    ?id             bf:identifiedBy ?identifiedById .
                    ?identifiedById rdf:type        bf:Local ;
                                    rdf:value       ?cn .
                    OPTIONAL {
                        ?identifiedById bf:assigner ?assigner .
                        ?assigner       rdf:type    bf:Organization .
                    }
                    OPTIONAL {
                        ?identifiedById bf:status   ?cancelled .
                        ?cancelled      rdf:type    bf:Status .
                        FILTER(?cancelled = <$cancelledOrInvalidUri>)
                    }
                }
                UNION
                {
                    ?id             bf:identifiedBy ?identifiedById .
                    ?identifiedById rdf:type        bf:OclcNumber ;
                                    rdf:value       ?cn .
                    BIND("OCoLC" AS ?assigner)
                    OPTIONAL {
                        ?identifiedById bf:status   ?cancelled .
                        ?cancelled      rdf:type    bf:Status .
                        FILTER(?cancelled = <$cancelledOrInvalidUri>)
                    }
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { SystemControlNumber(it.getValue("cn"), it.hasBinding("cancelled"), it.getValue("assigner")) }.toList()
        }
    }

    @JvmRecord
    private data class SystemControlNumber(val cn: Value, val cancelled: Boolean, val assigner: Value?)
}