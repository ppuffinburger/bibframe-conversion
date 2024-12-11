package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.LanguageScriptLookup
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField264Converter : BibframeToMarcConverter {
    // TODO : can you have multiple languages?
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        PROVISION_ACTIVITY_TYPES.forEach { type ->
            val provisionActivityData = queryProvisionActivityData(conn, workData.primaryInstanceId, type)
            provisionActivityData.forEach { buildFields(workData, record, it, provisionActivityData.size) }
        }

        queryCopyrightDates(conn, workData.primaryInstanceId)
            .map {
                val copyrightDate = it.copyrightDate.stringValue()
                if (copyrightDate.startsWith('℗') || copyrightDate.startsWith('©')) {
                    copyrightDate
                } else {
                    "${if (it.needsPhonogram) "℗" else "©"}${it.copyrightDate.stringValue()}"
                }
            }
            .distinct()
            .forEach {
                record.dataFields.add(DataField("264", indicator2 = '4', subfields = mutableListOf(Subfield('c', it))))
            }
    }

    private fun buildFields(workData: WorkData, record: BibliographicRecord, data: ProvisionActivityData, provisionActivityCount: Int) {
        val builder264 = DataFieldBuilder().apply {
            tag = "264"
            indicator1 = getIndicator1(data, provisionActivityCount)
            indicator2 = getIndicator2(data)
        }

        if (data.hasAlternate()) {
            addSubfieldIfExists(builder264, '6', "880-" + workData.getNextAlternateGraphicRepresentationOccurrence())
        }

        addSubfieldIfExists(builder264, '3', data.appliesTo)
        addPlaceNameDateFields(builder264, data.places, data.agents, data.dates)

        record.dataFields.add(builder264.build())

        if (data.hasAlternate()) {
            val builder880 = DataFieldBuilder().apply {
                tag = "880"
                indicator1 = getIndicator1(data, provisionActivityCount)
                indicator2 = getIndicator2(data)
            }

            addSubfieldIfExists(builder880, '6', "264-${workData.getNextAlternateGraphicRepresentationOccurrence()}/${data.placesWithLang.first().let { p -> LanguageScriptLookup.lookup(p) } ?: ""}")
            addSubfieldIfExists(builder880, '3', data.appliesTo)
            addPlaceNameDateFields(builder880, data.placesWithLang, data.agentsWithLang, data.datesWithLang)

            workData.addAlternateGraphicRepresentations(builder880.build())
        }
    }

    private fun getIndicator1(data: ProvisionActivityData, provisionActivityCount: Int): Char {
        if (data.issuance != null) {
            when (data.issuance.stringValue()) {
                "serial" -> {
                    if (provisionActivityCount == 1) {
                        return ' '
                    } else {
                        var status: String? = null
                        if (data.status != null) {
                            status = data.status.stringValue()
                        }

                        if (status == null) {
                            return '2'
                        } else {
                            if ("earliest" == status) {
                                return ' '
                            } else if ("current" == status && provisionActivityCount > 1) {
                                return '3'
                            }
                        }
                    }
                }

                "integrating" -> {
                    if (provisionActivityCount == 1) {
                        return '3'
                    }
                }
            }
        }
        return ' '
    }

    private fun getIndicator2(data: ProvisionActivityData): Char {
        return when (data.provisionActivityType.stringValue()) {
            "http://id.loc.gov/ontologies/bibframe/Production" -> '0'
            "http://id.loc.gov/ontologies/bibframe/Publication" -> '1'
            "http://id.loc.gov/ontologies/bibframe/Distribution" -> '2'
            "http://id.loc.gov/ontologies/bibframe/Manufacture" -> '3'
            else -> throw IllegalStateException("Unexpected value: ${data.provisionActivityType.stringValue()}")
        }
    }

    private fun addPlaceNameDateFields(builder: DataFieldBuilder, places: List<Value>, agents: List<Value>, dates: List<Value>) {
        val placesWithIndex = places.map { it.stringValue() }.filter { it.isNotEmpty() }.withIndex()
        val placesLastIndex = placesWithIndex.count() - 1
        val agentsWithIndex = agents.map { it.stringValue() }.filter { it.isNotEmpty() }.withIndex()
        val agentsLastIndex = agentsWithIndex.count() - 1
        val datesWithIndex = dates.map { it.stringValue() }.filter { it.isNotEmpty() }.withIndex()
        val datesLastIndex = datesWithIndex.count() - 1

        placesWithIndex.forEach {
            addSubfieldIfExists(builder, 'a', "${it.value}${determinePlacePunctuation(it, placesLastIndex, agentsLastIndex, datesLastIndex)}")
        }

        agentsWithIndex.forEach {
            addSubfieldIfExists(builder, 'b', "${it.value}${determineAgentPunctuation(it, agentsLastIndex, datesLastIndex)}")
        }

        datesWithIndex.forEach {
            addSubfieldIfExists(builder, 'c', "${it.value}${determineDatePunctuation(it, datesLastIndex)}")
        }
    }

    private fun determinePlacePunctuation(placeIndexedValue: IndexedValue<String>, placesLastIndex: Int, agentsLastIndex: Int, datesLastIndex: Int): String {
        return if (placeIndexedValue.index < placesLastIndex) {
            " ;"
        } else {
            if (agentsLastIndex >= 0) {
                " :"
            } else if (datesLastIndex >= 0) {
                " ,"
            } else {
                ""
            }
        }
    }

    private fun determineAgentPunctuation(agentIndexedValue: IndexedValue<String>, agentsLastIndex: Int, datesLastIndex: Int): String {
        return if (agentIndexedValue.index < agentsLastIndex) {
            ","
        } else {
            if (datesLastIndex >= 0) {
                return ","
            } else {
                ""
            }
        }
    }

    private fun determineDatePunctuation(dateIndexedValue: IndexedValue<String>, datesLastIndex: Int): String {
        return if (dateIndexedValue.index < datesLastIndex) {
            ","
        } else {
            val dateValue = dateIndexedValue.value
            if (dateValue.endsWith(".")) {
                ""
            } else {
                "."
            }
        }
    }

    private fun queryProvisionActivityData(conn: RepositoryConnection, id: Value, provisionActivityType: Value): List<ProvisionActivityData> {
        val typeAndValueQueryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?paId ?simpleType ?simpleValue
            WHERE {
                {
                    SELECT ?paId
                    WHERE {
                        ?id     bf:provisionActivity    ?paId .
                        ?paId   rdf:type                bf:ProvisionActivity ;
                                rdf:type                ?provisionActivityType .
                    }
                }
                ?paId   ?simpleType   ?simpleValue .
                FILTER(?simpleType IN (bflc:simplePlace, bflc:simpleAgent, bflc:simpleDate))
            }
        """.trimIndent()

        val typeAndValueQuery = conn.prepareTupleQuery(typeAndValueQueryString)
        typeAndValueQuery.setBinding("id", id)
        typeAndValueQuery.setBinding("provisionActivityType", provisionActivityType)

        val paData = mutableListOf<ProvisionActivityData>()

        typeAndValueQuery.evaluate().use { result ->
            result.groupBy { it.getValue("paId") }
                .forEach {
                    val places = mutableListOf<Value>()
                    val placesWithLang = mutableListOf<Value>()
                    val agents = mutableListOf<Value>()
                    val agentsWithLang = mutableListOf<Value>()
                    val dates = mutableListOf<Value>()
                    val datesWithLang = mutableListOf<Value>()

                    it.value.forEach { bs ->
                        val value = bs.getValue("simpleValue") as Literal
                        when (bs.getValue("simpleType").stringValue()) {
                            "${BIBFRAME_LC_EXTENSION_ONTOLOGY}simplePlace" -> {
                                if (value.language.isEmpty) {
                                    places.add(value)
                                } else {
                                    placesWithLang.add(value)
                                }
                            }
                            "${BIBFRAME_LC_EXTENSION_ONTOLOGY}simpleAgent" -> {
                                if (value.language.isEmpty) {
                                    agents.add(value)
                                } else {
                                    agentsWithLang.add(value)
                                }
                            }
                            "${BIBFRAME_LC_EXTENSION_ONTOLOGY}simpleDate" -> {
                                if (value.language.isEmpty) {
                                    dates.add(value)
                                } else {
                                    datesWithLang.add(value)
                                }
                            }
                        }
                    }

                    val paId = it.key

                    val queryString = """
                        PREFIX bf: <$BIBFRAME_ONTOLOGY>
                        PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
                        SELECT DISTINCT ?issuance ?status ?appliesTo
                        WHERE {
                            ?id     bf:provisionActivity    ?paId .
                            ?paId   rdf:type                bf:ProvisionActivity ;
                                    rdf:type                ?provisionActivityType .
                            OPTIONAL {
                                ?paId       bf:status   ?statusId .
                                ?statusId   rdf:type    bf:Status ;
                                            rdfs:label  ?status .
                            }
                            OPTIONAL {
                                ?paId           bflc:appliesTo  ?appliesToId .
                                ?appliesToId    rdf:type        bflc:AppliesTo ;
                                                rdfs:label      ?appliesTo .
                            }
                            {
                                ?id         bf:issuance ?issuanceId .
                                ?issuanceId rdf:type    bf:Issuance ;
                                            rdfs:label  ?issuance .
                            }
                        }
                    """.trimIndent()

                    val query = conn.prepareTupleQuery(queryString)
                    query.setBinding("id", id)
                    query.setBinding("paId", paId)
                    query.setBinding("provisionActivityType", provisionActivityType)

                    query.evaluate().use { result ->
                        result.map { data ->
                            paData.add(ProvisionActivityData(
                                provisionActivityType,
                                places,
                                placesWithLang,
                                agents,
                                agentsWithLang,
                                dates,
                                datesWithLang,
                                data.getValue("issuance"),
                                data.getValue("status"),
                                data.getValue("appliesTo")
                            ))
                        }
                    }
                }
        }

        return paData
    }

    private fun queryCopyrightDates(conn: RepositoryConnection, id: Value): List<CopyrightDateData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?copyrightDate ?carrier
            WHERE {
                {
                    ?id bf:copyrightDate ?copyrightDate .
                }
                OPTIONAL {
                    ?id         bf:carrier  ?carrierId .
                    ?carrierId  rdf:type    bf:Carrier ;
                                rdfs:label  ?carrier .
                    FILTER(?carrier IN ("audio belt", "audio cartridge", "audio cylinder", "audio disc", "audio roll", "audio wire tape reel", "audiocassette", "audiotape reel", "online resource", "sound track reel"))
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { CopyrightDateData(it.getValue("copyrightDate"), it.hasBinding("carrier")) }.toList()
        }
    }

    @JvmRecord
    private data class CopyrightDateData(val copyrightDate: Value, val needsPhonogram: Boolean)

    @JvmRecord
    private data class ProvisionActivityData(
        val provisionActivityType: Value,
        val places: List<Value>,
        val placesWithLang: List<Value>,
        val agents: List<Value>,
        val agentsWithLang: List<Value>,
        val dates: List<Value>,
        val datesWithLang: List<Value>,
        val issuance: Value?,
        val status: Value?,
        val appliesTo: Value?
    ) {
        fun hasAlternate(): Boolean {
            return placesWithLang.isNotEmpty() || agentsWithLang.isNotEmpty() || datesWithLang.isNotEmpty()
        }
    }

    companion object {
        private val PROVISION_ACTIVITY_TYPES = listOf(
            SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/Publication"),
            SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/Production"),
            SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/Distribution"),
            SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/Manufacture")
        )
    }
}