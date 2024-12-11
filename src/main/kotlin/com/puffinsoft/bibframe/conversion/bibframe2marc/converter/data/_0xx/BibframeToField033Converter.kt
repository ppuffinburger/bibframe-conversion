package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.edtf4k.*
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField033Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "033" }

            it.typeOfEvent?.let { type ->
                when (type.stringValue()) {
                    "capture" -> builder.indicator2 = '0'
                    "broadcast" -> builder.indicator2 = '1'
                    "finding" -> builder.indicator2 = '2'
                    else -> builder.indicator2 = ' '
                }
            }

            handleDates(it.dates, builder)

            it.placeOfEvent?.let { placeOfEvent ->
                val place: String = TextUtils.getCodeStringFromUrl(placeOfEvent.stringValue())
                if (place.contains('.')) {
                    builder.subfields {
                        subfield {
                            name = 'b'
                            data = place.substringBefore('.')
                        }
                        subfield {
                            name = 'c'
                            data = place.substringAfter('.')
                        }
                    }
                } else {
                    builder.subfields {
                        subfield {
                            name = 'b'
                            data = place
                        }
                    }
                }
            }

            addSourceSubfieldIfExists(builder, it.source)
            addSubfieldIfExists(builder, '3', it.materialsSpecified)

            record.dataFields.add(builder.build())
        }
    }

    private fun handleDates(dates: List<Value>, builder: DataFieldBuilder) {
        when (dates.size) {
            0 -> {
                builder.indicator1 = ' '
            }
            1 -> {
                if (dates[0].stringValue().contains('/')) {
                    builder.indicator1 = '2'
                } else {
                    builder.indicator1 = '0'
                }
            }
            else -> {
                builder.indicator1 = '1'
            }
        }

        dates.forEach { edtfDate: Value ->
            when (val edtfDateType = EdtfDateFactory.parse(edtfDate.stringValue())) {
                is EdtfDate -> builder.subfields {
                    subfield {
                        name = 'a'
                        data = convertDate(edtfDateType)
                    }
                }
                is EdtfDatePair -> {
                    builder.subfields {
                        subfield {
                            name = 'a'
                            data = convertDate(edtfDateType.start)
                        }
                    }
                    if (edtfDateType.isRange) {
                        builder.subfields {
                            subfield {
                                name = 'a'
                                data = convertDate(edtfDateType.end)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    private fun convertDate(edtfDate: EdtfDate): String {
        return when (edtfDate.status) {
            EdtfDateStatus.NORMAL -> formatEdtfDate(edtfDate)
            EdtfDateStatus.UNKNOWN, EdtfDateStatus.OPEN, EdtfDateStatus.UNUSED, EdtfDateStatus.INVALID -> "--------"
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<DateTimeAndPlaceOfEventData> {
        return queryValidCaptureIds(conn, id).map { captureId: Value ->
                val dates = queryDatesForCaptureId(conn, captureId)
                val queryString = """
                    PREFIX bf: <$BIBFRAME_ONTOLOGY>
                    PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
                    SELECT ?typeOfEvent ?placeId ?source ?sourceId ?materialsSpecified
                    WHERE {
                        OPTIONAL {
                            ?captureId  bf:note     ?noteId .
                            ?noteId     rdf:type    bf:Note ;
                                        rdfs:label  ?typeOfEvent .
                        }
                        OPTIONAL {
                            ?captureId  bf:place ?placeId .
                            ?placeId    rdf:type bf:Place .
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
                    }""".trimIndent()

                val query = conn.prepareTupleQuery(queryString)
                query.setBinding("captureId", captureId)

                query.evaluate().use { result ->
                    return@map result.map { DateTimeAndPlaceOfEventData(dates, it.getValue("typeOfEvent"), it.getValue("placeId"), if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId"), it.getValue("materialsSpecified")) }
                        .firstOrNull()
                }
            }
            .filterNotNull()
            .toList()
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
                    FILTER(DATATYPE(?date) = <http://id.loc.gov/datatypes/edtf>)
                }
                OPTIONAL {
                    ?captureId  bf:place    ?placeId .
                    ?placeId    rdf:type    bf:Place ;
                                rdf:value   ?placeValue .
                }
                FILTER(BOUND(?date) || BOUND(?placeValue))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { it.getValue("captureId") }.toList()
        }
    }

    private fun queryDatesForCaptureId(conn: RepositoryConnection, id: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?date
            WHERE {
                ?captureId bf:date ?date .
                FILTER(DATATYPE(?date) = <http://id.loc.gov/datatypes/edtf>)
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("captureId", id)

        query.evaluate().use { result ->
            return result.map { it.getValue("date") }.toList()
        }
    }

    private fun formatEdtfDate(edtfDate: EdtfDate): String {
        val builder = StringBuilder()

        if (edtfDate.year.isInvalid) {
            builder.append("----")
        } else {
            val year: Int = edtfDate.year.value
            if ((year < 0 || year > 9999)) {
                builder.append("----")
            } else {
                builder.append(edtfDate.year.toString().replace("[?~%]", "").replace('X', '-'))
            }
        }

        if (edtfDate.month.isInvalid) {
            builder.append("--")
        } else {
            val month: Int = edtfDate.month.value
            if (month > 12) {
                builder.append("--")
            } else {
                builder.append(edtfDate.month.toString().replace("[?~%]", "").replace('X', '-'))
            }
        }

        if (edtfDate.day.isInvalid) {
            builder.append("--")
        } else {
            val day: Int = edtfDate.day.value
            if (day > 31) {
                builder.append("--")
            } else {
                builder.append(edtfDate.day.toString().replace("[?~%]", "").replace('X', '-'))
            }
        }

        if (edtfDate.hour > 0) {
            builder.append("${edtfDate.hour.toString().padStart(2, '0')}${edtfDate.minute.toString().padStart(2, '0')}${edtfDate.second.toString().padStart(2, '0')}")
            if (edtfDate.hasTimezoneOffset()) {
                val timezoneOffset: Int = edtfDate.timezoneOffset ?: 0
                val tzHour = timezoneOffset / 60
                val tzMinute = timezoneOffset % 60
                builder.append((if (timezoneOffset < 0) '-' else '+'))
                    .append(tzHour.toString().padStart(2, '0'))
                    .append(tzMinute.toString().padStart(2, '0'))
            }
        }

        return builder.toString()
    }

    @JvmRecord
    private data class DateTimeAndPlaceOfEventData(val dates: List<Value>, val typeOfEvent: Value?, val placeOfEvent: Value?, val source: Value?, val materialsSpecified: Value?)
}