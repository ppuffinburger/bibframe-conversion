package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField024Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        add(conn, workData.getAllInstanceIds(), record, "Isrc", '0', null)
        add(conn, workData.getAllInstanceIds(), record, "Upc", '1', null)
        add(conn, workData.getAllInstanceIds(), record, "Ismn", '2', null)
        add(conn, workData.getAllInstanceIds(), record, "Ean", '3', null)
        add(conn, workData.getAllInstanceIds(), record, "Sici", '4', null)

        add(conn, workData.getAllInstanceIds(), record, "Ansi", '7', "ansi")
        add(conn, workData.getAllInstanceIds(), record, "Doi", '7', "doi")
        add(conn, workData.getAllInstanceIds(), record, "Gtin14Number", '7', "gtin-14")
        add(conn, workData.getAllInstanceIds(), record, "Hdl", '7', "hdl")
        add(conn, workData.getAllInstanceIds(), record, "Isan", '7', "isan")
        add(conn, workData.getAllInstanceIds(), record, "Isni", '7', "isni")
        add(conn, workData.getAllInstanceIds(), record, "Iso", '7', "iso")
        add(conn, workData.getAllInstanceIds(), record, "Istc", '7', "istc")
        add(conn, workData.getAllInstanceIds(), record, "Iswc", '7', "iswc")
        add(conn, workData.getAllInstanceIds(), record, "Urn", '7', "urn")
        add(conn, workData.getAllInstanceIds(), record, "Identifier", null, null)

        add(conn, listOf(workData.workId), record, "Eidr", '7', "eidr")
    }

    private fun add(conn: RepositoryConnection, ids: List<Value>, record: BibliographicRecord, identifierType: String, firstIndicator: Char?, source: String?) {
        for (id in ids) {
            query(conn, id, identifierType).forEach {
                val builder = DataFieldBuilder().apply {
                    tag = "024"
                    indicator1 = firstIndicator ?: if (it.source == null) '8' else '7'
                }

                addSubfieldIfExists(builder, if (it.cancelled) 'z' else 'a', it.identifier)
                addSubfieldIfExists(builder, 'c', it.acquisitionTerms)
                addSubfieldIfExists(builder, 'q', it.qualifier)

                if (source == null) {
                    addSubfieldIfExists(builder, '2', it.source)
                } else {
                    addSubfieldIfExists(builder, '2', source)
                }

                record.dataFields.add(builder.build())
            }
        }
    }

    private fun query(conn: RepositoryConnection, id: Value, identifierType: String): List<OtherStandardIdentifierData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?identifier ?cancelled ?qualifier ?source ?acquisitionTerms
            WHERE {
                {
                    ?id             bf:identifiedBy ?identifiedById .
                    ?identifiedById rdf:type        bf:$identifierType ;
                                    rdf:value       ?identifier .
                    OPTIONAL {
                        ?identifiedById bf:source   ?sourceId .
                        ?sourceId       rdf:type    bf:Source ;
                                        bf:code     ?source .
                    }
                    OPTIONAL {
                        ?identifiedById bf:qualifier ?qualifier .
                    }
                    OPTIONAL {
                        ?identifiedById bf:assigner ?assigner .
                    }
                    OPTIONAL {
                        ?identifiedById bf:acquisitionTerms ?acquisitionTerms .
                    }
                    FILTER(?assigner != <http://id.loc.gov/vocabulary/organizations/dgpo>)
                }
                OPTIONAL {
                    ?id             bf:identifiedBy ?identifiedById .
                    ?identifiedById bf:status       ?cancelled .
                    ?cancelled      rdf:type        bf:Status .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("identifier") }
                .map { OtherStandardIdentifierData(it.getValue("identifier"), it.hasBinding("cancelled"), it.getValue("qualifier"), it.getValue("source"), it.getValue("acquisitionTerms")) }
                .toList()
        }
    }

    @JvmRecord
    private data class OtherStandardIdentifierData(val identifier: Value, val cancelled: Boolean, val qualifier: Value?, val source: Value?, val acquisitionTerms: Value?)
}