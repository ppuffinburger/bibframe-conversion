package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.*
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField344Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        querySoundContent(conn, workData.workId).forEach { record.dataFields.add(buildSoundContentDataField(it)) }

        // TODO : spec says instance and work, but code only handles work.  following spec for now
        querySoundContent(conn, workData.primaryInstanceId).forEach { record.dataFields.add(buildSoundContentDataField(it)) }

        for (characteristicType in CHARACTERISTIC_TYPES) {
            querySoundCharacteristics(conn, workData.primaryInstanceId, characteristicType.first, characteristicType.second).forEach {
                val builder = DataFieldBuilder().apply { tag = "344" }
                addSubfieldIfExists(builder, '3', it.appliesTo)

                when (it.soundCharacteristicType.stringValue()) {
                    "http://id.loc.gov/ontologies/bibframe/RecordingMethod" -> addSubfieldIfExists(builder, 'a', it.soundCharacteristic)
                    "http://id.loc.gov/ontologies/bibframe/RecordingMedium" -> addSubfieldIfExists(builder, 'b', it.soundCharacteristic)
                    "http://id.loc.gov/ontologies/bibframe/PlayingSpeed" -> addSubfieldIfExists(builder, 'c', it.soundCharacteristic)
                    "http://id.loc.gov/ontologies/bibframe/GrooveCharacteristic" -> addSubfieldIfExists(builder, 'd', it.soundCharacteristic)
                    "http://id.loc.gov/ontologies/bibframe/TrackConfig" -> addSubfieldIfExists(builder, 'e', it.soundCharacteristic)
                    "http://id.loc.gov/ontologies/bibframe/TapeConfig" -> addSubfieldIfExists(builder, 'f', it.soundCharacteristic)
                    "http://id.loc.gov/ontologies/bibframe/PlaybackChannels" -> addSubfieldIfExists(builder, 'g', it.soundCharacteristic)
                    "http://id.loc.gov/ontologies/bibframe/PlaybackCharacteristic" -> addSubfieldIfExists(builder, 'h', it.soundCharacteristic)
                    "http://id.loc.gov/ontologies/bibframe/CaptureStorage" -> addSubfieldIfExists(builder, 'j', it.soundCharacteristic)
                }

                addSubfieldIfExists(builder, '0', it.uri)
                addSourceSubfieldIfExists(builder, it.source)

                record.dataFields.add(builder.build())
            }
        }
    }

    private fun buildSoundContentDataField(data: SoundContentData): DataField {
        val builder = DataFieldBuilder().apply { tag = "344" }

        addSubfieldIfExists(builder, '3', data.appliesTo)
        addSubfieldIfExists(builder, 'i', data.soundContent)
        addSubfieldIfExists(builder, '0', data.uri)
        addSourceSubfieldIfExists(builder, data.source)

        return builder.build()
    }

    private fun querySoundContent(conn: RepositoryConnection, id: Value): List<SoundContentData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?soundContent ?soundContentId ?source ?sourceId ?appliesTo
            WHERE {
                ?id             bf:soundContent ?soundContentId .
                ?soundContentId rdf:type        bf:SoundContent ;
                                rdfs:label      ?soundContent .
                OPTIONAL {
                    ?soundContentId bf:source   ?sourceId .
                    ?sourceId       rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?soundContentId bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { SoundContentData(it.getValue("soundContent"), it.getValue("soundContentId"), if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId"), it.getValue("appliesTo")) }.toList()
        }
    }

    private fun querySoundCharacteristics(conn: RepositoryConnection, id: Value, soundCharacteristicType: Value, graphName: Value): List<SoundCharacteristicsData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?soundCharacteristic ?soundCharacteristicId ?soundCharacteristicType ?source ?sourceId ?appliesTo
            WHERE {
                {
                    ?id                     bf:soundCharacteristic  ?soundCharacteristicId .
                    ?soundCharacteristicId  rdf:type                ?soundCharacteristicType ;
                    FILTER(ISIRI(?soundCharacteristicId) && CONTAINS(STR(?soundCharacteristicId), "/id.loc.gov/vocabulary/"))
                    GRAPH ?graphName {
                        ?soundCharacteristicId  mads:authoritativeLabel ?soundCharacteristic .
                    }
                }
                UNION
                {
                    ?id                     bf:soundCharacteristic  ?soundCharacteristicId .
                    ?soundCharacteristicId  rdf:type                ?soundCharacteristicType ;
                                            rdfs:label              ?soundCharacteristic .
                    FILTER(ISBLANK(?soundCharacteristicId) || (ISIRI(?soundCharacteristicId) && !CONTAINS(STR(?soundCharacteristicId), "/id.loc.gov/vocabulary/")))
                }
                OPTIONAL {
                    ?soundCharacteristicId  bf:source   ?sourceId .
                    ?sourceId               rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?soundCharacteristicId  bflc:appliesTo  ?appliesToId .
                    ?appliesToId            rdf:type        bflc:AppliesTo ;
                                            rdfs:label      ?appliesTo .
                }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)
        query.setBinding("soundCharacteristicType", soundCharacteristicType)
        query.setBinding("graphName", graphName)

        query.evaluate().use { result ->
            return result.map {
                SoundCharacteristicsData(
                    it.getValue("soundCharacteristic"),
                    it.getValue("soundCharacteristicType"),
                    if (it.getValue("soundCharacteristicId")?.isIRI == true) it.getValue("soundCharacteristicId") else null,
                    if (it.hasBinding("source")) it.getValue("source") else it.getValue("sourceId"),
                    it.getValue("appliesTo")
                )
            }.toList()
        }
    }

    @JvmRecord
    private data class SoundCharacteristicsData(val soundCharacteristic: Value, val soundCharacteristicType: Value, val uri: Value?, val source: Value?, val appliesTo: Value?)

    @JvmRecord
    private data class SoundContentData(val soundContent: Value, val uri: Value, val source: Value?, val appliesTo: Value?)

    companion object {
        private val CHARACTERISTIC_TYPES = listOf(
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/RecordingMethod"), SimpleValueFactory.getInstance().createIRI(RECORDING_TYPE_URI)),
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/RecordingMedium"), SimpleValueFactory.getInstance().createIRI(RECORDING_MEDIUM_URI)),
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/PlayingSpeed"), SimpleValueFactory.getInstance().createIRI(PLAY_SPEED_URI)),
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/GrooveCharacteristic"), SimpleValueFactory.getInstance().createIRI(GROOVE_URI)),
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/TrackConfig"), SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/vocabulary/nil")),
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/TapeConfig"), SimpleValueFactory.getInstance().createIRI(TAPE_CONFIG_URI)),
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/PlaybackChannels"), SimpleValueFactory.getInstance().createIRI(PLAYBACK_URI)),
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/PlaybackCharacteristic"), SimpleValueFactory.getInstance().createIRI(SPECIAL_PLAYBACK_URI)),
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/CaptureStorage"), SimpleValueFactory.getInstance().createIRI(CAPTURE_STORAGE_URI))
        )
    }
}