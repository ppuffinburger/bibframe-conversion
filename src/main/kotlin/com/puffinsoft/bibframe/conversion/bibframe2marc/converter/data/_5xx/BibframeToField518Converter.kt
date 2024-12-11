package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_DATATYPES_EDTF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField518Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "518" }
            addSubfieldIfExists(builder, '3', it.materialsSpecified)

            addSubfieldIfExists(builder, 'o', it.note)

            if (it.dateTimeAndPlaceOfEvent == null) {
                addSubfieldIfExists(builder, 'd', it.date)
                addSubfieldIfExists(builder, 'p', it.placeOfEvent)
            } else {
                addSubfieldIfExists(builder, 'a', it.dateTimeAndPlaceOfEvent)
            }
            addSourceSubfieldIfExists(builder, it.source)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<DateTimeAndPlaceOfEventNoteData> {
        return queryValidCaptureIds(conn, id).map { captureId: Value ->
            val queryString = """              
                PREFIX bf: <$BIBFRAME_ONTOLOGY>
                PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
                SELECT ?dateTimeAndPlaceOfEvent ?placeOfEvent ?date ?note ?source ?sourceId ?materialsSpecified
                WHERE {
                    {
                        ?captureId  rdf:type    bf:Capture .
                        OPTIONAL {
                            ?captureId  rdfs:label  ?dateTimeAndPlaceOfEvent .
                        }
                        OPTIONAL {
                            ?captureId  bf:place    ?placeId .
                            ?placeId    rdf:type    bf:Place ;
                                        rdfs:label  ?placeOfEvent .
                        }
                        OPTIONAL {
                            ?captureId bf:date ?date .
                        }
                        OPTIONAL {
                            ?captureId  bf:note     ?noteId .
                            ?noteId     rdf:type    bf:Note ;
                                        rdfs:label  ?note .
                        }
                        OPTIONAL {
                            ?captureId  bf:source   ?sourceId .
                            ?sourceId   rdf:type    bf:Source .
                            OPTIONAL {
                                ?sourceId bf:code ?source .
                            }
                        }
                        OPTIONAL {
                            ?captureId      bflc:appliesTo  ?appliesToId .
                            ?appliesToId    rdf:type        bflc:AppliesTo ;
                                            rdfs:label      ?materialsSpecified .
                        }
                    }
                }""".trimIndent()

            val query = conn.prepareTupleQuery(queryString)
            query.setBinding("captureId", captureId)

            query.evaluate().use { result ->
                return result.map {
                    DateTimeAndPlaceOfEventNoteData(
                        it.getValue("dateTimeAndPlaceOfEvent"),
                        it.getValue("placeOfEvent"),
                        it.getValue("date"),
                        it.getValue("note"),
                        it.getValue("source") ?: it.getValue("sourceId"),
                        it.getValue("materialsSpecified")
                    )
                }.toList()
            }
        }.toList()
    }

    private fun queryValidCaptureIds(conn: RepositoryConnection, id: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?captureId
            WHERE {
                ?id         bf:capture ?captureId .
                ?captureId  rdf:type   bf:Capture .
                OPTIONAL {
                    ?captureId bf:date ?date .
                    FILTER(DATATYPE(?date) != <$LOC_DATATYPES_EDTF>)
                }
                OPTIONAL {
                    {
                        ?captureId  rdfs:label  ?placeValue .
                    }
                    UNION
                    {
                        ?captureId  bf:place    ?placeId .
                        ?placeId    rdf:type    bf:Place ;
                                    rdfs:label  ?placeValue .
                        FILTER(ISBLANK(?placeId))
                    }
                }
                FILTER(BOUND(?date) || BOUND(?placeValue))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { it.getValue("captureId") }.toList()
        }
    }

    @JvmRecord
    private data class DateTimeAndPlaceOfEventNoteData(val dateTimeAndPlaceOfEvent: Value?, val placeOfEvent: Value?, val date: Value?, val note: Value?, val source: Value?, val materialsSpecified: Value?)
}