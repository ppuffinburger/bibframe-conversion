package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx

import com.puffinsoft.bibframe.conversion.*
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField246Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        queryOtherTitleTypes(conn, workData.workId, workData.primaryInstanceId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "246"
                indicator1 = getNoteEntryController(it.titleType, it.variantType)
                indicator2 = getTypeOfTitle(it.titleType, it.variantType)
            }

            addSubfieldIfExists(builder, 'i', it.note)
            addSubfieldIfExists(builder, 'a', it.mainTitle)
            addSubfieldIfExists(builder, 'b', it.subtitle)
            addSubfieldIfExists(builder, 'f', it.date)
            addSubfieldIfExists(builder, 'n', it.partNumber)
            addSubfieldIfExists(builder, 'p', it.partName)
            addInstitutionSubfieldIfExists(builder, it.institution)

            record.dataFields.add(builder.build())
        }

        // data I have duplicates what would be in the 245 and code doesn't look at bf:Title
//        queryInstanceTitleType(conn, workData.primaryInstanceId).forEach {
//            val builder = DataFieldBuilder().apply {
//                tag = "246"
//                indicator1 = '1'
//                indicator2 = getTypeOfTitle(it.titleType, it.variantType)
//            }
//
//            addSubfieldIfExists(builder, 'i', it.note)
//            addSubfieldIfExists(builder, 'a', it.mainTitle)
//            addSubfieldIfExists(builder, 'b', it.subtitle)
//            addSubfieldIfExists(builder, 'f', it.date)
//            addSubfieldIfExists(builder, 'n', it.partNumber)
//            addSubfieldIfExists(builder, 'p', it.partName)
//            addInstitutionSubfieldIfExists(builder, it.institution)
//
//            record.dataFields.add(builder.build())
//        }
    }

    private fun getNoteEntryController(titleType: Value, variantType: Value?): Char {
        if (titleType.stringValue().endsWith("ParallelTitle")) {
            return '3'
        }

        if (variantType != null) {
            return when (variantType.stringValue()) {
                "portion" -> '1'
                "distinctive" -> '3'
//                "cover" -> '4'
                "added title page" -> '1'
                "caption" -> '1'
                "running" -> '1'
//                "spine" -> '8'
                else -> '3'
            }
        }

        return '3'
    }

    private fun getTypeOfTitle(titleType: Value, variantType: Value?): Char {
        if (titleType.stringValue().endsWith("ParallelTitle")) {
            return '1'
        }

        if (variantType != null) {
            return when (variantType.stringValue()) {
                "portion" -> '0'
                "distinctive" -> '2'
                "cover" -> '4'
                "added title page" -> '5'
                "caption" -> '6'
                "running" -> '7'
                "spine" -> '8'
                else -> ' '
            }
        }

        return ' '
    }

    private fun queryOtherTitleTypes(conn: RepositoryConnection, workId: Value, instanceId: Value): List<TitleData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT DISTINCT ?titleType ?mainTitle ?variantType ?subtitle ?date ?note ?partNumber ?partName ?institution ?institutionId
            WHERE {
                {
                    ?workId     bf:title        ?titleId .
                    ?titleId    rdf:type        ?titleType ;
                                bf:mainTitle    ?mainTitle .
                    OPTIONAL {
                        ?titleId bf:variantType ?variantType .
                    }
                    OPTIONAL {
                        ?titleId bf:subtitle ?subtitle .
                    }
                    OPTIONAL {
                        ?titleId bf:date ?date .
                    }
                    OPTIONAL {
                        ?titleId    bf:note     ?noteId .
                        ?noteId     rdf:type    bf:Note ;
                                    rdfs:label  ?note .
                    }
                    OPTIONAL {
                        ?titleId bf:partNumber ?partNumber .
                    }
                    OPTIONAL {
                        ?titleId bf:partName ?partName .
                    }
                    OPTIONAL {
                        ?titleId        bflc:applicableInstitution  ?institutionId .
                        ?institutionId  bf:Agent                    ?agentId .
                        ?agentId        bf:code                     ?institution .
                    }
                }
                UNION
                {
                    ?instanceId bf:title        ?titleId .
                    ?titleId    rdf:type        ?titleType ;
                                bf:mainTitle    ?mainTitle .
                    OPTIONAL {
                        ?titleId bf:variantType ?variantType .
                    }
                    OPTIONAL {
                        ?titleId bf:subtitle ?subtitle .
                    }
                    OPTIONAL {
                        ?titleId bf:date ?date .
                    }
                    OPTIONAL {
                        ?titleId    bf:Note     ?noteId .
                        ?noteId     rdf:type    bf:Note ;
                                    rdfs:label  ?note .
                    }
                    OPTIONAL {
                        ?titleId bf:partNumber ?partNumber .
                    }
                    OPTIONAL {
                        ?titleId bf:partName ?partName .
                    }
                    OPTIONAL {
                        ?titleId        bflc:applicableInstitution  ?institutionId .
                        ?institutionId  bf:Agent                    ?agentId .
                        ?agentId        bf:code                     ?institution .
                    }
                }
                FILTER(?titleType IN (<$VARIANT_TITLE>, <$PARALLEL_TITLE>, <$TRANSCRIBED_TITLE>) && (!BOUND(?variantType) || ?variantType NOT IN ("translated", "former")))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)
        query.setBinding("instanceId", instanceId)

        query.evaluate().use { result ->
            return result.map {
                TitleData(
                    it.getValue("titleType"),
                    it.getValue("mainTitle"),
                    it.getValue("variantType"),
                    it.getValue("subtitle"),
                    it.getValue("date"),
                    it.getValue("note"),
                    it.getValue("partNumber"),
                    it.getValue("partName"),
                    it.getValue("institution") ?: it.getValue("institutionId")
                )
            }.toList()
        }
    }

    private fun queryInstanceTitleType(conn: RepositoryConnection, instanceId: Value): List<TitleData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT DISTINCT ?titleType ?mainTitle ?variantType ?subtitle ?date ?note ?partNumber ?partName ?institution ?institutionId
            WHERE {
                ?instanceId bf:title        ?titleId .
                ?titleId    rdf:type        ?titleType ;
                            bf:mainTitle    ?mainTitle .
                OPTIONAL {
                    ?titleId bf:variantType ?variantType .
                }
                OPTIONAL {
                    ?titleId bf:subtitle ?subtitle .
                }
                OPTIONAL {
                    ?titleId bf:date ?date .
                }
                OPTIONAL {
                    ?titleId    bf:Note     ?noteId .
                    ?noteId     rdf:type    bf:Note ;
                                rdfs:label  ?note .
                }
                OPTIONAL {
                    ?titleId bf:partNumber ?partNumber .
                }
                OPTIONAL {
                    ?titleId bf:partName ?partName .
                }
                OPTIONAL {
                    ?titleId        bflc:applicableInstitution  ?institutionId .
                    ?institutionId  bf:Agent                    ?agentId .
                    ?agentId        bf:code                     ?institution .
                }
                FILTER(?titleType = <$TITLE> && (!BOUND(?variantType) || ?variantType NOT IN ("translated", "former")))
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("instanceId", instanceId)

        query.evaluate().use { result ->
            return result.map {
                TitleData(
                    it.getValue("titleType"),
                    it.getValue("mainTitle"),
                    it.getValue("variantType"),
                    it.getValue("subtitle"),
                    it.getValue("date"),
                    it.getValue("note"),
                    it.getValue("partNumber"),
                    it.getValue("partName"),
                    it.getValue("institution") ?: it.getValue("institutionId")
                )
            }.toList()
        }
    }

    @JvmRecord
    private data class TitleData(val titleType: Value, val mainTitle: Value, val variantType: Value?, val subtitle: Value?, val date: Value?, val note: Value?, val partNumber: Value?, val partName: Value?, val institution: Value?)
}