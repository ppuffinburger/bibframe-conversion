package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField041Converter : BibframeToMarcConverter {
    // TODO : need an example of accompaniedBy and there is none listed in id.loc.gov
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        val languages = mutableListOf<Language>()
        languages.addAll(queryWithParts(conn, workData.workId))
        languages.addAll(queryWithResourceComponents(conn, workData.workId))

        if (languages.size > 1) {
            val source = languages.first().source

            val builder = DataFieldBuilder().apply {
                tag = "041"
                indicator1 = if (languages.any { it.translation }) '1' else ' '
                indicator2 = if (source == null) ' ' else '7'
            }

            languages.forEach {
                when (it) {
                    is LanguageWithParts -> {
                        addSubfieldIfExists(builder, PartLookup.lookup(it.part), TextUtils.getCodeStringFromUrl(it.language.stringValue()))
                    }
                    is LanguageWithComponents -> {
                        addSubfieldIfExists(builder, ResourceComponentLookup.lookup(it.resourceComponentType, it.accompaniedBy), TextUtils.getCodeStringFromUrl(it.language.stringValue()))
                    }
                }
            }

            if (source != null) {
                builder.indicator2 = '7'
                addSourceSubfieldIfExists(builder, source)
            }

            record.dataFields.add(builder.build())
        }
    }

    private fun queryWithResourceComponents(conn: RepositoryConnection, id: Value): List<LanguageWithComponents> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?languageId ?resourceComponentType ?noteLabel ?source ?sourceId
            WHERE {
                ?id             bf:note         ?noteId .
                ?noteId         rdf:type        ?resourceComponentType ;
                                bf:language     ?languageId .
                ?languageId     rdf:type        bf:Language .
                FILTER EXISTS { ?noteId rdf:type    bf:Note }
                FILTER(STRSTARTS(STR(?resourceComponentType), "http://id.loc.gov/vocabulary/resourceComponents/"))
                OPTIONAL {
                    ?noteId     rdfs:label  ?noteLabel .
                    FILTER(STRSTARTS(STR(?noteLabel), "Includes translation"))
                }
                OPTIONAL {
                    ?languageId bf:source   ?sourceId .
                    ?sourceId   rdf:type    bf:Source ;
                                rdfs:label  ?source .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { LanguageWithComponents(it.getValue("languageId"), it.getValue("resourceComponentType"), it.hasBinding("noteLabel"), false, if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId")) }.toList()
        }
    }

    private fun queryWithParts(conn: RepositoryConnection, id: Value): List<LanguageWithParts> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT DISTINCT ?languageId ?part ?noteLabel ?source ?sourceId
            WHERE {
                {
                    ?id             bf:language ?languageNode .
                    ?languageNode   rdf:type    bf:Language ;
                                    rdf:value   ?languageId .
                    ?languageId     rdf:type    mads:Language .
                    OPTIONAL {
                        ?languageNode bf:part ?part .
                    }
                    OPTIONAL {
                        ?languageNode   bf:note     ?noteId .
                        ?noteId         rdf:type    bf:Note ;
                                        rdfs:label  ?noteLabel .
                        FILTER(STRSTARTS(STR(?noteLabel), "Includes translation"))
                    }
                    OPTIONAL {
                        ?languageNode   bf:source   ?sourceId .
                        ?sourceId       rdf:type    bf:Source ;
                                        rdfs:label  ?source .
                    }
                }
                UNION
                {
                    ?id             bf:language ?languageId .
                    ?languageId     rdf:type    bf:Language .
                    FILTER(STRSTARTS(STR(?languageId), "http://id.loc.gov/vocabulary/languages/"))
                    OPTIONAL {
                        ?languageId bf:part ?part .
                    }
                    OPTIONAL {
                        ?languageId     bf:note     ?noteId .
                        ?noteId         rdf:type    bf:Note ;
                                        rdfs:label  ?noteLabel .
                        FILTER(STRSTARTS(STR(?noteLabel), "Includes translation"))
                    }
                    OPTIONAL {
                        ?languageId     bf:source   ?sourceId .
                        ?sourceId       rdf:type    bf:Source ;
                                        rdfs:label  ?source .
                    }
                }
            }
            ORDER BY ?part
            """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { LanguageWithParts(it.getValue("languageId"), it.getValue("part"), it.hasBinding("noteLabel"), if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId")) }.toList()
        }
    }

    sealed interface Language {
        val language: Value
        val translation: Boolean
        val source: Value?
    }

    @JvmRecord
    private data class LanguageWithComponents(override val language: Value, val resourceComponentType: Value, val accompaniedBy: Boolean, override val translation: Boolean, override val source: Value?) : Language

    @JvmRecord
    private data class LanguageWithParts(override val language: Value, val part: Value?, override val translation: Boolean, override val source: Value?) : Language

    private object PartLookup {
        private val partsToNameMap = mapOf(
            "" to 'a',
            "summary" to 'b',
            "sung or spoken text" to 'd',
            "libretto" to 'e',
            "table of contents" to 'f',
            "accompanying material" to 'g',
            "original" to 'h',
            "intertitles" to 'i',
            "subtitles or captions" to 'j',
            "intermediate translations" to 'k',
            "original accompanying materials" to 'm',
            "original libretto" to 'n',
            "captions" to 'p',
            "accessible audio" to 'q',
            "accessible visual material" to 'r',
            "accompanying transcripts" to 't'
        )

        fun lookup(part: Value?): Char {
            if (part == null) {
                return 'a'
            }
            return partsToNameMap[part.stringValue()] ?: 'a'
        }
    }

    private object ResourceComponentLookup {
        private val componentTypeToNameMap = mapOf(
            "http://id.loc.gov/vocabulary/resourceComponents/res" to 'a',
            "http://id.loc.gov/vocabulary/resourceComponents/str" to 'a',
            "http://id.loc.gov/vocabulary/resourceComponents/sum" to 'b',
            "http://id.loc.gov/vocabulary/resourceComponents/stx" to 'd',
            "http://id.loc.gov/vocabulary/resourceComponents/lib" to 'e',
            "http://id.loc.gov/vocabulary/resourceComponents/toc" to 'f',
//            "http://id.loc.gov/vocabulary/resourceComponents/amt" to 'g',
            "http://id.loc.gov/vocabulary/resourceComponents/otx" to 'h',
            "http://id.loc.gov/vocabulary/resourceComponents/int" to 'i',
            "http://id.loc.gov/vocabulary/resourceComponents/sub" to 'j',
            "http://id.loc.gov/vocabulary/resourceComponents/itr" to 'k',
//            "http://id.loc.gov/vocabulary/resourceComponents/amt" to 'm',
            "http://id.loc.gov/vocabulary/resourceComponents/olb" to 'n',
            "http://id.loc.gov/vocabulary/resourceComponents/cap" to 'p',
            "http://id.loc.gov/vocabulary/resourceComponents/aud" to 'q',
            "http://id.loc.gov/vocabulary/resourceComponents/vis" to 'r',
            "http://id.loc.gov/vocabulary/resourceComponents/tav" to 't'
        )

        fun lookup(resourceComponentType: Value, accompaniedBy: Boolean): Char {
            if (resourceComponentType.stringValue() == "http://id.loc.gov/vocabulary/resourceComponents/amt") {
                return if (accompaniedBy) 'g' else 'm'
            }
            return componentTypeToNameMap[resourceComponentType.stringValue()] ?: 'a'
        }
    }
}