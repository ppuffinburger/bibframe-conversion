package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField037Converter : BibframeToMarcConverter {
    // TODO : all instances?
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply { tag = "037" }
            addSubfieldIfExists(builder, 'a', it.stockNumber)
            addSubfieldIfExists(builder, 'b', it.sourceOfAcquisition)
            addSubfieldIfExists(builder, 'c', it.terms)

            if (it.note != null) {
                when (it.note.stringValue()) {
                    "intervening source" -> builder.indicator1 = '2'
                    "current source" -> builder.indicator1 = '3'
                    else -> builder.indicator1 = ' '
                }

                addSubfieldIfExists(builder, 'n', it.note)
            }

            addSubfieldIfExists(builder, '3', it.materialsSpecified)

            if (it.applicableInstitution != null) {
                addSubfieldIfExists(builder, '5', if (it.applicableInstitution.isLiteral) it.applicableInstitution.stringValue() else TextUtils.getCodeStringFromUrl(it.applicableInstitution.stringValue()))
            }

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<SourceOfAcquisition> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?stockNumber ?sourceFromAcquisition ?sourceFromSource ?terms ?note ?materialsSpecified ?institution ?agentId
            WHERE {
                ?id                     bf:acquisitionSource    ?acquisitionSourceId .
                ?acquisitionSourceId    rdf:type                bf:AcquisitionSource .
                OPTIONAL {
                    ?acquisitionSourceId    rdfs:label          ?sourceFromAcquisition .
                }
                OPTIONAL {
                    ?acquisitionSourceId    bf:identifiedBy     ?identifiedById .
                    ?identifiedById         rdf:type            bf:StockNumber ;
                                            rdf:value           ?stockNumber .
                }
                OPTIONAL {
                    ?acquisitionSourceId    bf:source   ?sourceId .
                    ?sourceId               rdf:type    bf:Source ;
                                            rdfs:label  ?sourceFromSource .
                }
                OPTIONAL {
                    ?acquisitionSourceId bf:acquisitionTerms ?terms .
                }
                OPTIONAL {
                    ?acquisitionSourceId    bf:note     ?noteId .
                    ?noteId                 rdf:type    bf:Note ;
                                            rdfs:label  ?note .
                }
                OPTIONAL {
                    ?acquisitionSourceId    bflc:appliesTo  ?appliesToId .
                    ?appliesToId            rdf:type        bflc:AppliesTo ;
                                            rdfs:label      ?materialsSpecified .
                }
                OPTIONAL {
                    ?acquisitionSourceId    bflc:applicableInstitution  ?agentId .
                    ?agentId                rdf:type                    bf:Agent .
                    OPTIONAL {
                        ?agentId bf:code ?institution .
                    }
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { SourceOfAcquisition(
                        it.getValue("stockNumber"),
                        if (it.hasBinding("sourceFromAcquisition")) it.getValue("sourceFromAcquisition") else it.getValue("sourceFromSource"),
                        it.getValue("terms"),
                        it.getValue("note"),
                        it.getValue("materialsSpecified"),
                        if (it.hasBinding("institution")) it.getValue("institution") else it.getValue("agentId")
                    )
                }.toList()
        }
    }

    @JvmRecord
    private data class SourceOfAcquisition(val stockNumber: Value?, val sourceOfAcquisition: Value?, val terms: Value?, val note: Value?, val materialsSpecified: Value?, val applicableInstitution: Value?)
}