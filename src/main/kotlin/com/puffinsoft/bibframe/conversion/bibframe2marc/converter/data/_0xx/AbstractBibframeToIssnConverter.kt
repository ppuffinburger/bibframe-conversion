package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal abstract class AbstractBibframeToIssnConverter : BibframeToMarcConverter {
    protected fun query(conn: RepositoryConnection, id: Value, isQueryClusterIssn: Boolean): Map<IssnData.IssnType, IssnData> {
        val cancelledOrInvalidUri = lookupStatusCodeUri("cancinv")
        val incorrectUri = lookupStatusCodeUri("incorrect")

        // TODO : not sure about the URI.  I don't have an example and the docs say URI (which?  the identifiedById?) and the code says rdf:about which I've only seen in RDF/XML
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?issn ?cancelled ?incorrect ?source ?uri
            WHERE {
                {
                    ?id             bf:identifiedBy ?identifiedById .
                    ?identifiedById rdf:type        bf:${if (isQueryClusterIssn) "IssnL" else "Issn"} ;
                                    rdf:value       ?issn .
                    OPTIONAL {
                        ?identifiedById             bf:status   <$cancelledOrInvalidUri> .
                        <$cancelledOrInvalidUri>    rdf:type    bf:Status ;
                                                    rdfs:label  ?cancelled .
                    }
                    OPTIONAL {
                        ?identifiedById     bf:status   <$incorrectUri> .
                        <$incorrectUri>     rdf:type    bf:Status ;
                                            rdfs:label  ?incorrect .
                    }
                    OPTIONAL {
                        ?identifiedById bf:assigner ?assignerId .
                        ?assignerId     rdf:type    bf:Agent ;
                                        bf:code     ?source .
                    }
                    OPTIONAL {
                        ?identifiedById rdf:about   ?uri .
                    }
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map {
                val issnType = if (it.hasBinding("cancelled")) {
                    IssnData.IssnType.CANCELLED
                } else if (it.hasBinding("incorrect")) {
                    IssnData.IssnType.INCORRECT
                } else {
                    IssnData.IssnType.VALID
                }
                IssnData(issnType, it.getValue("issn"), it.getValue("uri"), it.getValue("source"))
            }.associateBy { it.type }
        }
    }

    protected fun addIssns(issns: Map<IssnData.IssnType, IssnData>, record: BibliographicRecord, issnTag: Int) {
        if (issns.isEmpty()) {
            return
        }

        if (allAssignersAndUrisMatch(issns.values)) {
            val builder = DataFieldBuilder().apply {
                tag = issnTag.toString().padStart(3, '0')
                indicator1 = if (issnTag == 22) ' ' else '0'
            }

            var assigner: String? = null
            var uri: String? = null

            IssnData.IssnType.entries.forEach {
                issns[it]?.let { data ->
                    val name = when (data.type) {
                        IssnData.IssnType.VALID -> 'a'
                        IssnData.IssnType.INCORRECT -> 'y'
                        IssnData.IssnType.CANCELLED -> 'z'
                    }
                    addSubfieldIfExists(builder, name, data.issn)
                    assigner = data.assigner?.stringValue()
                    uri = data.uri?.stringValue()
                }
            }

            uri?.let { addSubfieldIfExists(builder, '0', uri) }
            assigner?.let { addSubfieldIfExists(builder, '2', assigner) }

            record.dataFields.add(builder.build())
        } else {
            IssnData.IssnType.entries.forEach {
                issns[it]?.let { data ->
                    record.dataFields.add(createIssnDataField(data.issn, data.uri, data.assigner, issnTag))
                }
            }
        }
    }

    private fun createIssnDataField(issn: Value, uri: Value?, assigner: Value?, issnTag: Int): DataField {
        val builder = DataFieldBuilder().apply {
            tag = issnTag.toString().padStart(3, '0')
            indicator1 = if (issnTag == 22) ' ' else '0'
        }

        addSubfieldIfExists(builder, 'a', issn)
        addSubfieldIfExists(builder, '0', uri)
        addSubfieldIfExists(builder, '2', assigner)

        return builder.build()
    }

    private fun allAssignersAndUrisMatch(issns: Collection<IssnData>): Boolean {
        val allAssignersMatch = issns.map { return@map it.assigner?.stringValue() ?: "NOT_USED" }.distinct().count() == 1
        val allUrisMatch = issns.map { return@map it.uri?.stringValue() ?: "NOT_USED" }.distinct().count() == 1

        return allAssignersMatch && allUrisMatch
    }

    @JvmRecord
    protected data class IssnData(val type: IssnType, val issn: Value, val uri: Value?, val assigner: Value?) {
        enum class IssnType {
            VALID,
            INCORRECT,
            CANCELLED
        }
    }
}