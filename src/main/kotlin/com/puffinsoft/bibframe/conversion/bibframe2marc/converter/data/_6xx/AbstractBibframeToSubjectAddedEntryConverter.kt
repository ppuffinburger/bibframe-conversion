package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.MarcKeyUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

abstract class AbstractBibframeToSubjectAddedEntryConverter : BibframeToMarcConverter {
    protected fun convert(conn: RepositoryConnection, id: Value, record: BibliographicRecord, tag: String, madsNameTypes: List<String>, bibframeNameTypes: List<String>) {
        queryWithComponents(conn, id, madsNameTypes.joinToString { "<$it>" }).forEach {
            val builder = DataFieldBuilder().apply {
                this.tag = tag
                indicator2 = getIndicator2(it.sourceId)
            }

            it.subdivisions.forEach { subdivision ->
                if (subdivision.marcKey == null) {
                    val name = when (subdivision.typeId.stringValue()) {
                        "http://www.loc.gov/mads/rdf/v1#Topic" -> 'x'
                        "http://www.loc.gov/mads/rdf/v1#GenreForm" -> 'v'
                        "http://www.loc.gov/mads/rdf/v1#Temporal" -> 'y'
                        "http://www.loc.gov/mads/rdf/v1#Geographic" -> 'z'
                        else -> 'a'
                    }
                    addSubfieldIfExists(builder, name, subdivision.label)
                } else {
                    val keyDataField = MarcKeyUtils.parseMarcKey(subdivision.marcKey.stringValue()).build()
                    builder.indicator1 = keyDataField.indicator1
                    keyDataField.subfields.forEach { subfield ->
                        addSubfieldIfExists(builder, subfield.name, subfield.data)
                    }
                }
            }

            if (builder.indicator2 == '7') {
                addSourceSubfieldIfExists(builder, it.source ?: it.sourceId)
            }

            if (it.uri.isIRI) {
                addSubfieldIfExists(builder, '0', it.uri)
            }

            record.dataFields.add(builder.build())
        }

        queryWithMarcKey(conn, id, bibframeNameTypes.joinToString { "<$it>" }).forEach {
            val builder = MarcKeyUtils.parseMarcKey(it.marcKey.stringValue()).apply {
                this.tag = tag
            }

            addSubfieldIfExists(builder, '1', it.uri)

            record.dataFields.add(builder.build())
        }
    }

    private fun getIndicator2(sourceId: Value?): Char {
        return if (sourceId == null) {
            '4'
        } else {
            getSourceIndicatorFromString(sourceId.stringValue())
        }
    }

    private fun queryWithMarcKey(conn: RepositoryConnection, id: Value, nameTypes: String): List<SubjectAddEntryWithMarcKey> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?subjectId ?marcKey
            WHERE {
                ?id         bf:subject      ?subjectId .
                ?subjectId  rdf:type        ?subjectType ;
                            bflc:marcKey    ?marcKey .
                FILTER(ISIRI(?subjectId))
                FILTER(?subjectType IN ($nameTypes))
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { SubjectAddEntryWithMarcKey(it.getValue("subjectId"), it.getValue("marcKey")) }
        }
    }

    private fun queryWithComponents(conn: RepositoryConnection, id: Value, nameTypes: String): List<SubjectAddedEntryWithComponents> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?subjectId ?sourceId ?source
            WHERE {
                ?id             bf:subject                              ?subjectId .
                ?subjectId      rdf:type                                bf:Topic ;
                                rdf:type                                mads:ComplexSubject ;
                                mads:componentList/rdf:first/rdf:type   ?nodeType
                FILTER(?nodeType IN ($nameTypes))
                OPTIONAL {
                    ?subjectId  bf:source   ?sourceId .
                    ?sourceId   rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { subject ->
                val subjectId = subject.getValue("subjectId")

                val typesAndLabelsQueryString = """
                        PREFIX bf: <$BIBFRAME_ONTOLOGY>
                        PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
                        PREFIX mads: <$LOC_MADS_RDF>
                        SELECT ?typeId ?marcKey ?text (COUNT(?intermediateNode)-1 as ?position)
                        WHERE {
                            ?id                 bf:subject                          ?subjectId .
                            ?subjectId          rdf:type                            bf:Topic ;
                                                rdf:type                            mads:ComplexSubject ;
                                                mads:componentList                  ?linkage .
                            ?linkage            rdf:rest*                           ?intermediateNode .
                            ?intermediateNode   rdf:rest*                           ?nodeId .
                            ?nodeId             rdf:first                           ?element .
                            ?element            rdf:type                            ?typeId ;
                            OPTIONAL {
                                ?element        mads:authoritativeLabel             ?text .
                            }
                            OPTIONAL {
                                ?element        bflc:marcKey                        ?marcKey .
                            }
                            FILTER(?typeId IN (<http://www.loc.gov/mads/rdf/v1#Topic>, <http://www.loc.gov/mads/rdf/v1#GenreForm>, <http://www.loc.gov/mads/rdf/v1#Temporal>, <http://www.loc.gov/mads/rdf/v1#Geographic>, $nameTypes))                                        
                        }
                        GROUP BY ?nodeId ?typeId ?text ?marcKey
                        ORDER BY ?position
                    """.trimIndent()

                val typesAndLabelsQuery = conn.prepareTupleQuery(typesAndLabelsQueryString)
                typesAndLabelsQuery.setBinding("id", id)
                typesAndLabelsQuery.setBinding("subjectId", subjectId)

                val typesAndTexts = typesAndLabelsQuery.evaluate().use { result ->
                    result.map { SubjectAddedEntrySubdivision(it.getValue("typeId"), it.getValue("marcKey"), it.getValue("text")) }.toList()
                }


                SubjectAddedEntryWithComponents(subject.getValue("subjectId"), subject.getValue("source"), subject.getValue("sourceId"), typesAndTexts)
            }.toList()
        }
    }

    sealed interface SubjectAddedEntry

    @JvmRecord
    private data class SubjectAddEntryWithMarcKey(val uri: Value, val marcKey: Value) : SubjectAddedEntry

    @JvmRecord
    private data class SubjectAddedEntrySubdivision(val typeId: Value, val marcKey: Value?, val label: Value?)

    @JvmRecord
    private data class SubjectAddedEntryWithComponents(val uri: Value, val source: Value?, val sourceId: Value?, val subdivisions: List<SubjectAddedEntrySubdivision>) : SubjectAddedEntry
}