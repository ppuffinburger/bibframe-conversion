package com.puffinsoft.bibframe.conversion.bibframe2marc.converter

import com.puffinsoft.bibframe.conversion.*
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.EncodingLevelLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.StatusCodeLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.CharacterCodingScheme
import org.marc4k.marc.marc21.bibliographic.*

internal class BibframeToLeaderConverter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        with(record.leader) {
            recordStatus = getRecordStatus(conn, workData)

            val types: Set<String> = workData.types.map { obj -> obj.stringValue() }.toSet()

            typeOfRecord = getTypeOfRecord(types)
            bibliographicLevel = getBibliographicLevel(types)
            typeOfControl = TypeOfControl.NO_SPECIFIC_TYPE
            characterCodingScheme = CharacterCodingScheme.UNICODE
            encodingLevel = getEncodingLevel(conn, workData)
            descriptiveCatalogingForm = DescriptiveCatalogingForm.ISBD_PUNCTUATION_OMITTED
            multipartResourceRecordLevel = MultipartResourceRecordLevel.NOT_SPECIFIED_OR_NOT_APPLICABLE
        }
    }

    private fun getRecordStatus(conn: RepositoryConnection, workData: WorkData): RecordStatus {
        val recordStatus: Value = queryRecordStatus(conn, workData)
        return RecordStatus.fromValue(if (recordStatus.isLiteral) recordStatus.stringValue()[0] else TextUtils.getCodeCharFromUrl(recordStatus.stringValue()))
    }

    private fun getTypeOfRecord(types: Set<String>): TypeOfRecord {
        if (types.contains(WORK_TYPE_TEXT)) {
            return if (types.contains(WORK_TYPE_MANUSCRIPT)) {
                TypeOfRecord.MANUSCRIPT_LANGUAGE_MATERIAL
            } else {
                TypeOfRecord.LANGUAGE_MATERIAL
            }
        }

        if (types.contains(WORK_TYPE_NOTATED_MUSIC)) {
            return if (types.contains(WORK_TYPE_MANUSCRIPT)) {
                TypeOfRecord.MANUSCRIPT_NOTATED_MUSIC
            } else {
                TypeOfRecord.NOTATED_MUSIC
            }
        }

        if (types.contains(WORK_TYPE_CARTOGRAPHY)) {
            return if (types.contains(WORK_TYPE_MANUSCRIPT)) {
                TypeOfRecord.MANUSCRIPT_CARTOGRAPHIC_MATERIAL
            } else {
                TypeOfRecord.CARTOGRAPHIC_MATERIAL
            }
        }

        if (types.contains(WORK_TYPE_MOVING_IMAGE)) {
            return TypeOfRecord.PROJECTED_MEDIUM
        }

        if (types.contains(WORK_TYPE_NON_MUSIC_AUDIO)) {
            return TypeOfRecord.NON_MUSICAL_SOUND_RECORDING
        }

        if (types.contains(WORK_TYPE_MUSIC_AUDIO)) {
            return TypeOfRecord.MUSICAL_SOUND_RECORDING
        }

        if (types.contains(WORK_TYPE_STILL_IMAGE)) {
            return TypeOfRecord.TWO_DIMENSIONAL_NON_PROJECTABLE_GRAPHIC
        }

        if (types.contains(WORK_TYPE_MULTIMEDIA)) {
            return TypeOfRecord.COMPUTER_FILE
        }

        if (types.contains(WORK_TYPE_MIXED_MATERIAL)) {
            return TypeOfRecord.MIXED_MATERIAL
        }

        if (types.contains(WORK_TYPE_OBJECT)) {
            return TypeOfRecord.THREE_DIMENSIONAL_ARTIFACT_OR_NATURALLY_OCCURRING_OBJECT
        }

        return TypeOfRecord.LANGUAGE_MATERIAL
    }

    private fun getBibliographicLevel(types: Set<String>): BibliographicLevel {
        if (types.contains(WORK_TYPE_COLLECTION)) {
            return BibliographicLevel.COLLECTION
        }

        if (types.contains(WORK_TYPE_INTEGRATING)) {
            return BibliographicLevel.INTEGRATING_RESOURCE
        }

        if (types.contains(WORK_TYPE_MONOGRAPH)) {
            return BibliographicLevel.MONOGRAPH_ITEM
        }

        if (types.contains(WORK_TYPE_SERIAL)) {
            return BibliographicLevel.SERIAL
        }

        return BibliographicLevel.MONOGRAPH_ITEM
    }

    private fun getEncodingLevel(conn: RepositoryConnection, workData: WorkData): EncodingLevel {
        val encodingLevel: Value = queryEncodingLevel(conn, workData)

        val code = if (encodingLevel.isLiteral) encodingLevel.stringValue()[0] else TextUtils.getCodeCharFromUrl(encodingLevel.stringValue())
        return if (code == 'f') {
            EncodingLevel.FULL_LEVEL
        } else {
            EncodingLevel.fromValue(code)
        }
    }

    private fun queryRecordStatus(conn: RepositoryConnection, workData: WorkData): Value {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT (COALESCE(xsd:dateTime(?statusDate), '!') AS ?convertedDate) ?recordStatusId ?recordStatus
            WHERE {
                ?id                 bf:adminMetadata    ?adminMetadataId .
                ?adminMetadataId    rdf:type            bf:AdminMetadata ;
                                    bf:date             ?statusDate ;
                                    bf:status           ?recordStatusId .
                ?recordStatusId     rdf:type            bf:Status .
                OPTIONAL {
                    ?recordStatusId bf:code             ?recordStatus .
                }
                FILTER(?id IN (${workData.getWorkAndAllInstanceIdsAsStrings().joinToString { "<$it>" }}))
            }
            ORDER BY ?convertedDate
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("recordStatusId") }
                .map { it.getValue("recordStatus") ?: it.getValue("recordStatusId") }
                .lastOrNull() ?: NEW_STATUS_VALUE
        }
    }

    private fun queryEncodingLevel(conn: RepositoryConnection, workData: WorkData): Value {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?encodingLevelId ?encodingLevel
            WHERE {
                ?id                 bf:adminMetadata    ?adminMetadataId .
                ?adminMetadataId    bflc:encodingLevel  ?encodingLevelId .
                ?encodingLevelId    rdf:type            bflc:EncodingLevel .
                OPTIONAL {
                    ?encodingLevelId bf:code           ?encodingLevel .
                }
                FILTER(?id IN (${workData.getWorkAndAllInstanceIdsAsStrings().joinToString { "<$it>" }}))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("encodingLevelId") }
                .map { it.getValue("encodingLevel") ?: it.getValue("encodingLevelId") }
                .firstOrNull() ?: FULL_ENCODING_LEVEL_VALUE
        }
    }

    companion object {
        private val NEW_STATUS_VALUE = SimpleValueFactory.getInstance().createIRI(StatusCodeLookup.lookup("n")?.uri ?: throw RuntimeException("Could not find 'n' in status codes"))
        private val FULL_ENCODING_LEVEL_VALUE =  SimpleValueFactory.getInstance().createIRI(EncodingLevelLookup.lookup("f")?.uri ?: throw RuntimeException("Could not find 'f' in encoding levels"))
    }
}