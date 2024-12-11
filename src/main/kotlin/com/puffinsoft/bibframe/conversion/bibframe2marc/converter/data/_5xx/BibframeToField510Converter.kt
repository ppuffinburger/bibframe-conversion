package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField510Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        queryIndexedIn(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "510"
                indicator1 = '0'
            }
            addSubfieldIfExists(builder, 'a', it.mainTitle)
            addSubfieldIfExists(builder, 'b', it.coverageOfSource)
            addSubfieldIfExists(builder, 'x', it.issn)
            addSubfieldIfExists(builder, 'u', it.electronicLocator)
            record.dataFields.add(builder.build())
        }

        queryReferences(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "510"
                indicator1 = if (it.locationWithinSource == null) '3' else '4'
            }
            addSubfieldIfExists(builder, 'a', it.mainTitle)
            addSubfieldIfExists(builder, 'b', it.coverageOfSource)
            addSubfieldIfExists(builder, 'c', it.locationWithinSource)
            addSubfieldIfExists(builder, 'x', it.issn)
            addSubfieldIfExists(builder, 'u', it.electronicLocator)
            record.dataFields.add(builder.build())
        }
    }

    private fun queryReferences(conn: RepositoryConnection, id: Value): List<CitationReferenceNoteData> {
        val coverageUri = lookupNoteTypeUri("coverage")
        val locationUri = lookupNoteTypeUri("loc")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?mainTitle ?coverageOfSource ?locationWithinSource ?issn ?electronicLocator
            WHERE {
                ?id         bf:references   ?rsId .
                ?rsId       bf:title        ?titleId .
                ?titleId    rdf:type        bf:Title ;
                            bf:mainTitle    ?mainTitle .
                OPTIONAL {
                    ?rsId           bf:note     ?coverageId .
                    ?coverageId     rdf:type    <$coverageUri> .
                    ?coverageId     rdfs:label  ?coverageOfSource .
                }
                OPTIONAL {
                    ?rsId           bf:note     ?locationId .     
                    ?locationId     rdf:type    <$locationUri> .
                    ?locationId     rdfs:label  ?locationWithinSource .
                }
                OPTIONAL {
                    ?rsId   bf:identifiedBy ?ibId .
                    ?ibId   rdf:type        bf:Issn ;
                            rdf:value       ?issn .
                }
                OPTIONAL {
                    ?rsId   bf:electronicLocator ?electronicLocator .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { CitationReferenceNoteData(it.getValue("mainTitle"), it.getValue("coverageOfSource"), it.getValue("locationWithinSource"), it.getValue("issn"), it.getValue("electronicLocator")) }.toList()
        }
    }

    private fun queryIndexedIn(conn: RepositoryConnection, id: Value): List<CitationReferenceNoteData> {
        val coverageUri = lookupNoteTypeUri("coverage")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?mainTitle ?coverageOfSource ?locationWithinSource ?issn ?electronicLocator
            WHERE {
                ?id         bflc:indexedIn  ?iiId .
                ?iiId       bf:title        ?titleId .
                ?titleId    rdf:type        bf:Title ;
                            bf:mainTitle    ?mainTitle .
                OPTIONAL {
                    ?iiId           bf:note     ?coverageId .     
                    ?coverageId     rdf:type    <$coverageUri> .
                    ?coverageId     rdfs:label  ?coverageOfSource .
                }
                OPTIONAL {
                    ?iiId   bf:identifiedBy ?ibId .
                    ?ibId   rdf:type        bf:Issn ;
                            rdf:value       ?issn .
                }
                OPTIONAL {
                    ?iiId   bf:electronicLocator ?electronicLocator .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { CitationReferenceNoteData(it.getValue("mainTitle"), it.getValue("coverageOfSource"), it.getValue("locationWithinSource"), it.getValue("issn"), it.getValue("electronicLocator")) }.toList()
        }
    }

    @JvmRecord
    private data class CitationReferenceNoteData(val mainTitle: Value, val coverageOfSource: Value?, val locationWithinSource: Value?, val issn: Value?, val electronicLocator: Value?)
}