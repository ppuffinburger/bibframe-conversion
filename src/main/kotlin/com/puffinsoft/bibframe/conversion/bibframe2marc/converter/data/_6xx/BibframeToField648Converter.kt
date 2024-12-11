package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField648Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        if (isComponentList(conn, workData.workId)) {
            // TODO : need an example of the data in a component list
        } else {
            queryNoComponentList(conn, workData.workId).forEach {
                val builder = DataFieldBuilder().apply {
                    tag = "648"
                    indicator2 = it.source?.let { source ->
                        when(source.stringValue()) {
                            "http://id.loc.gov/authorities/subjects" -> '0'
                            "http://id.loc.gov/authorities/childrensSubjects" -> '1'
                            "http://id.loc.gov/vocabulary/subjectSchemes/mesh" -> '2'
                            "http://id.loc.gov/vocabulary/subjectSchemes/nal" -> '3'
                            "http://id.loc.gov/vocabulary/subjectSchemes/cash" -> '5'
                            "http://id.loc.gov/vocabulary/subjectSchemes/rvm" -> '6'
                            else -> '7'
                        }
                    } ?: '4'
                }

                addSubfieldIfExists(builder, '3', it.appliesTo)
                addSubfieldIfExists(builder, 'a', it.term)
                addSubfieldIfExists(builder, '0', it.uri)
                addSourceSubfieldIfExists(builder, it.source)

                record.dataFields.add(builder.build())
            }
        }
    }

    private fun isComponentList(conn: RepositoryConnection, id: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            ASK
            WHERE {
                ?id             bf:subject          ?subjectId .
                ?subjectId      rdf:type            ?type ;
                                rdf:type            mads:Temporal ;
                                mads:componentList  ?itemId .
                FILTER(?type IN (bf:Temporal, bf:Topic))
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("id", id)

        return query.evaluate()
    }

    private fun queryNoComponentList(conn: RepositoryConnection, id: Value): List<ChronologicalTermData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT DISTINCT ?term ?appliesTo ?subjectId ?sourceId
            WHERE {
                ?id             bf:subject          ?subjectId .
                ?subjectId      rdf:type            ?type ;
                                rdf:type            mads:Temporal ;
                                rdfs:label          ?term .
                FILTER(?type IN (bf:Temporal, bf:Topic))
                OPTIONAL {
                    ?subjectId      bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
                OPTIONAL {
                    ?subjectId  bf:source           ?sourceId .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { ChronologicalTermData(it.getValue("term"), it.getValue("appliesTo"), if ((it.hasBinding("subjectId") && it.getValue("subjectId").isIRI)) it.getValue("subjectId") else null, it.getValue("sourceId")) }.toList()
        }
    }

    @JvmRecord
    private data class ChronologicalTermData(val term: Value, val appliesTo: Value?, val uri: Value?, val source: Value?)
}