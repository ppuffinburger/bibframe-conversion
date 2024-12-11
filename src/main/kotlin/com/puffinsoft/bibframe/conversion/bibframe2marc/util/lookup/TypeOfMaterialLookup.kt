package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import org.marc4k.marc.marc21.bibliographic.BibliographicLeader
import org.marc4k.marc.marc21.bibliographic.BibliographicLevel
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord
import org.marc4k.marc.marc21.bibliographic.TypeOfRecord

internal object TypeOfMaterialLookup {
    fun lookup(record: BibliographicRecord): TypeOfMaterial? {
        if (isBook(record.leader)) {
            return TypeOfMaterial.BOOK
        }
        if (isContinuingResource(record.leader)) {
            return TypeOfMaterial.CONTINUING_RESOURCE
        }
        if (isVisualMaterial(record.leader)) {
            return TypeOfMaterial.VISUAL_MATERIAL
        }
        if (isMusic(record.leader)) {
            return TypeOfMaterial.MUSIC
        }
        if (isMap(record.leader)) {
            return TypeOfMaterial.MAP
        }
        if (isComputerFile(record.leader)) {
            return TypeOfMaterial.COMPUTER_FILE
        }
        if (isMixedMaterial(record.leader)) {
            return TypeOfMaterial.MIXED_MATERIAL
        }
        return null
    }

    private fun isBook(leader: BibliographicLeader): Boolean {
        return when (leader.typeOfRecord) {
            TypeOfRecord.LANGUAGE_MATERIAL, TypeOfRecord.MANUSCRIPT_LANGUAGE_MATERIAL -> {
                when (leader.bibliographicLevel) {
                    BibliographicLevel.MONOGRAPHIC_COMPONENT_PART, BibliographicLevel.COLLECTION, BibliographicLevel.SUBUNIT, BibliographicLevel.MONOGRAPH_ITEM -> { true }
                    else -> { false }
                }
            }
            else -> { false }
        }
    }

    private fun isContinuingResource(leader: BibliographicLeader): Boolean {
        if (leader.typeOfRecord == TypeOfRecord.LANGUAGE_MATERIAL) {
            return when (leader.bibliographicLevel) {
                BibliographicLevel.SERIAL_COMPONENT_PART, BibliographicLevel.INTEGRATING_RESOURCE, BibliographicLevel.SERIAL -> { true }
                else -> { false }
            }
        }
        return false
    }

    private fun isVisualMaterial(leader: BibliographicLeader): Boolean {
        return when (leader.typeOfRecord) {
            TypeOfRecord.PROJECTED_MEDIUM, TypeOfRecord.TWO_DIMENSIONAL_NON_PROJECTABLE_GRAPHIC, TypeOfRecord.KIT, TypeOfRecord.THREE_DIMENSIONAL_ARTIFACT_OR_NATURALLY_OCCURRING_OBJECT -> { true }
            else -> { false }
        }
    }

    private fun isMusic(leader: BibliographicLeader): Boolean {
        return when (leader.typeOfRecord) {
            TypeOfRecord.NOTATED_MUSIC, TypeOfRecord.MANUSCRIPT_NOTATED_MUSIC, TypeOfRecord.NON_MUSICAL_SOUND_RECORDING, TypeOfRecord.MUSICAL_SOUND_RECORDING -> { true }
            else -> { false }
        }
    }

    private fun isMap(leader: BibliographicLeader): Boolean {
        return when (leader.typeOfRecord) {
            TypeOfRecord.CARTOGRAPHIC_MATERIAL, TypeOfRecord.MANUSCRIPT_CARTOGRAPHIC_MATERIAL -> { true }
            else -> { false }
        }
    }

    private fun isComputerFile(leader: BibliographicLeader): Boolean {
        return leader.typeOfRecord == TypeOfRecord.COMPUTER_FILE
    }

    private fun isMixedMaterial(leader: BibliographicLeader): Boolean {
        return leader.typeOfRecord == TypeOfRecord.MIXED_MATERIAL
    }
}

internal enum class TypeOfMaterial {
    BOOK,
    COMPUTER_FILE,
    MAP,
    MUSIC,
    CONTINUING_RESOURCE,
    VISUAL_MATERIAL,
    MIXED_MATERIAL
}