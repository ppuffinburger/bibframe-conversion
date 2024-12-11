package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField655Converter : BibframeToMarcConverter {
    // TODO : spec says it can also be something like a component list, but haven't seen that and the spec doesn't specifically say component list.
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        queryTopicalTerm(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "655"
                indicator2 = getIndicator2(it.uri, it.sourceId)
            }

            addSubfieldIfExists(builder, 'a', it.term)

            if (builder.indicator2 == '7') {
                if (it.source == null) {
                    if (it.sourceId == null) {
                        val uri = it.uri?.stringValue()
                        if (uri?.startsWith("http://id.loc.gov/authorities/genreForms/") == true) {
                            addSubfieldIfExists(builder, '2', "lcgft")
                        } else {
                            addSubfieldIfExists(builder, '2', TextUtils.getCodeStringFromUrl(it.uri?.stringValue()?.substringBeforeLast("/") ?: ""))
                        }
                    } else {
                        addSourceSubfieldIfExists(builder, it.sourceId)
                    }
                } else {
                    addSourceSubfieldIfExists(builder, it.source)
                }
            }

            addSubfieldIfExists(builder, '0', it.uri)

            record.dataFields.add(builder.build())
        }
    }

    private fun getIndicator2(uri: Value?, sourceId: Value?): Char {
        return if (sourceId == null) {
            if (uri == null) {
                '4'
            } else {
                getSourceIndicatorFromString(uri.stringValue().substringBeforeLast("/"))
            }
        } else {
            getSourceIndicatorFromString(sourceId.stringValue())
        }
    }

    private fun queryTopicalTerm(conn: RepositoryConnection, id: Value): List<GenreFormWithTerm> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT DISTINCT ?term ?genreFormId ?authorityId ?sourceId ?source
            WHERE {
                {
                    ?id             bf:genreForm                        ?genreFormId .
                    ?genreFormId    rdf:type                            ?genreFormType ;
                                    mads:authoritativeLabel|rdfs:label  ?term .
                    FILTER(?genreFormType IN (bf:GenreForm, mads:GenreForm))
                    OPTIONAL {
                        ?genreFormId    mads:isIdentifiedByAuthority    ?authorityId .
                    }
                    OPTIONAL {
                        ?genreFormId    bf:source   ?sourceId .
                        OPTIONAL {
                            ?sourceId   bf:code     ?source .
                        }
                    }
                }
                UNION
                {
                    ?id             bf:subject                          ?genreFormId .
                    ?genreFormId    rdf:type                            bf:Topic ;
                                    rdf:type                            mads:GenreForm ;
                                    mads:authoritativeLabel|rdfs:label  ?term .
                    OPTIONAL {
                        ?genreFormId    mads:isIdentifiedByAuthority    ?authorityId .
                    }
                    OPTIONAL {
                        ?genreFormId    bf:source   ?sourceId .
                        OPTIONAL {
                            ?sourceId   bf:code     ?source .
                        }
                    }
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map {
                val uri = if (it.hasBinding("authorityId") && it.getValue("authorityId").isIRI) {
                    it.getValue("authorityId")
                } else {
                    if (it.hasBinding("genreFormId") && it.getValue("genreFormId").isIRI) {
                        it.getValue("genreFormId")
                    } else {
                        null
                    }
                }
                GenreFormWithTerm(it.getValue("term"), uri, it.getValue("sourceId"), if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId"))
            }.toList()
        }
    }

    @JvmRecord
    private data class GenreFormWithTerm(val term: Value, val uri: Value?, val sourceId: Value?, val source: Value?)
}