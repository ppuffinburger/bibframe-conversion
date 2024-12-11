package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.*
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField348Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        queryMusicFormat(conn, workData.workId).forEach { record.dataFields.add(buildDataField(it, true)) }
        queryMusicNotation(conn, workData.workId).forEach { record.dataFields.add(buildDataField(it, false)) }

        queryMusicFormat(conn, workData.primaryInstanceId).forEach { record.dataFields.add(buildDataField(it, true)) }
        queryMusicNotation(conn, workData.primaryInstanceId).forEach { record.dataFields.add(buildDataField(it, false)) }
    }

    private fun buildDataField(data: NotatedMusicCharacteristicData, isFormat: Boolean): DataField {
        val builder = DataFieldBuilder().apply { tag = "348" }

        addSubfieldIfExists(builder, '3', data.appliesTo)
        addSubfieldIfExists(builder, if (isFormat) 'a' else 'c', data.characteristicLabel)
        addSubfieldIfExists(builder, '0', data.characteristicId)
        addSourceSubfieldIfExists(builder, data.source)

        return builder.build()
    }

    private fun queryMusicNotation(conn: RepositoryConnection, id: Value): List<NotatedMusicCharacteristicData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?characteristicLabel ?characteristicId ?source ?sourceId ?appliesTo
            WHERE {
                {
                    SELECT DISTINCT ?characteristicId (COALESCE(?authLabel, ?mnLabel) AS ?characteristicLabel)
                    WHERE {
                        ?id                 bf:notation     ?characteristicId .
                        ?characteristicId   rdf:type        bf:MusicNotation .
                        OPTIONAL {
                            GRAPH <$MUSIC_NOTATION_URI> {
                                ?characteristicId   mads:authoritativeLabel ?authLabel .
                            }
                        }
                        OPTIONAL {
                            ?characteristicId   rdfs:label  ?mnLabel .
                        }
                    }
                }
                OPTIONAL {
                    ?characteristicId   bf:source   ?sourceId .
                    ?sourceId           rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?characteristicId   bflc:appliesTo  ?appliesToId .
                    ?appliesToId        rdf:type        bflc:AppliesTo ;
                                        rdfs:label      ?appliesTo .
                }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { NotatedMusicCharacteristicData(it.getValue("characteristicLabel"), it.getValue("characteristicId"), it.getValue("source") ?: it.getValue("sourceId"), it.getValue("appliesTo")) }
                .toList()
        }
    }

    private fun queryMusicFormat(conn: RepositoryConnection, id: Value): List<NotatedMusicCharacteristicData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?characteristicId ?characteristicLabel ?source ?sourceId ?appliesTo
            WHERE {
                {
                    SELECT DISTINCT ?characteristicId (COALESCE(?authLabel, ?mfLabel) AS ?characteristicLabel)
                    WHERE {
                        ?id                 bf:musicFormat  ?characteristicId .
                        ?characteristicId   rdf:type        bf:MusicFormat .
                        OPTIONAL {
                            GRAPH <$MUSIC_FORMAT_URI> {
                                ?characteristicId   mads:authoritativeLabel ?authLabel .
                            }
                        }
                        OPTIONAL {
                            ?characteristicId   rdfs:label  ?mfLabel .
                        }
                    }
                }
                OPTIONAL {
                    ?characteristicId   bf:source   ?sourceId .
                    ?sourceId           rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?characteristicId   bflc:appliesTo  ?appliesToId .
                    ?appliesToId        rdf:type        bflc:AppliesTo ;
                                        rdfs:label      ?appliesTo .
                }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { NotatedMusicCharacteristicData(it.getValue("characteristicLabel"), it.getValue("characteristicId"), it.getValue("source") ?: it.getValue("sourceId"), it.getValue("appliesTo")) }
                .toList()
        }
    }

    @JvmRecord
    private data class NotatedMusicCharacteristicData(val characteristicLabel: Value, val characteristicId: Value, val source: Value?, val appliesTo: Value?)
}