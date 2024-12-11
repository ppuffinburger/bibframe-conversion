package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.LanguageLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal abstract class AbstractBibframeToLinkingEntryConverter : BibframeToMarcConverter {
    protected fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord, tag: String, indicator2: Char, linkingType: String) {
        query(conn, workData.workId, linkingType).forEach {
            val builder = DataFieldBuilder().apply {
                this.tag = tag
                indicator1 = '0'
                this.indicator2 = indicator2
            }

            addSubfieldIfExists(builder, '3', it.appliesTo)
            addSubfieldIfExists(builder, 'a', it.mainEntryHeading)
            addSubfieldIfExists(builder, 'b', it.edition)
            addSubfieldIfExists(builder, 'c', it.qualifier)
            addSubfieldIfExists(builder, 'd', it.provisionActivityStatement)

            if (it.languageId != null) {
                addSubfieldIfExists(builder, 'e', LanguageLookup.lookup(TextUtils.getCodeStringFromUrl(it.languageId.stringValue()))?.label)
            }

            addSubfieldIfExists(builder, 'f', it.place)
            addSubfieldIfExists(builder, 'g', it.part)
            addSubfieldIfExists(builder, 'h', it.physicalDescription)
            addSubfieldIfExists(builder, 'i', it.relationshipInformation)
            addSubfieldIfExists(builder, 'k', it.seriesStatement)
            addSubfieldIfExists(builder, 'n', it.note)
            addSubfieldIfExists(builder, 'o', it.otherItemIdentifier)
            addSubfieldIfExists(builder, 'p', it.abbreviatedTitle)
            addSubfieldIfExists(builder, 'r', it.reportNumber)

            if (it.instanceTitle == null) {
                addSubfieldIfExists(builder, 's', it.workTitle)
            } else {
                addSubfieldIfExists(builder, 't', it.instanceTitle)
            }

            addSubfieldIfExists(builder, 'u', it.standardTechnicalReportNumber)

            if (it.lccn != null) {
                addSubfieldIfExists(builder, 'w', "(DLC)${it.lccn.stringValue()}")
            }

            if (it.otherControlNumber != null) {
                addSubfieldIfExists(builder, 'w', "(${it.otherControlNumberAssigner?.stringValue()})${it.otherControlNumber.stringValue()}")
            }

            addSubfieldIfExists(builder, 'x', it.issn)
            addSubfieldIfExists(builder, 'y', it.coden)
            addSubfieldIfExists(builder, 'z', it.isbn)

            addSubfieldIfExists(builder, '4', it.role)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value, linkingType: String): List<LinkingEntry> {
        val workDataQuery = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?associatedResourceWorkId ?relationship ?mainEntryHeading ?languageId ?abbreviatedTitle ?workTitle ?qualifier ?issn ?appliesTo ?roleId ?role
            WHERE {
                ?id                         bf:relation             ?relationId .
                ?relationId                 rdf:type                bf:Relation ;
                                            bf:relationship         ?relationshipId .
                ?relationshipId             rdf:type                bf:Relationship .
                ?relationId                 bf:associatedResource   ?associatedResourceWorkId .
                ?associatedResourceWorkId   rdf:type                bf:Work .
                FILTER(?relationshipId = <$linkingType>)
                OPTIONAL {
                    ?relationId             bf:relationship         ?otherRelationshipId .
                    ?otherRelationshipId    rdf:type                bf:Relationship ;
                                            rdfs:label              ?relationship .
                    FILTER(?otherRelationshipId != <$linkingType>)
                }
                OPTIONAL {
                    ?associatedResourceWorkId   bf:contribution     ?contributionId .
                    ?contributionId             rdf:type            bf:PrimaryContribution ;
                                                bf:agent            ?agentId .
                    ?agentId                    rdf:type            bf:Agent ;
                                                rdfs:label          ?mainEntryHeading .
                }
                OPTIONAL {
                    ?associatedResourceWorkId   bf:language         ?languageId .
                    ?languageId                 rdf:type            bf:Language .
                }
                OPTIONAL {
                    ?associatedResourceWorkId   bf:title            ?abbrTitleId .
                    ?abbrTitleId                rdf:type            bf:AbbreviatedTitle ;
                                                bf:mainTitle        ?abbreviatedTitle .
                }
                OPTIONAL {
                    ?associatedResourceWorkId   bf:title            ?workTitleId .
                    ?workTitleId                rdf:type            bf:Title ;
                                                bf:mainTitle        ?workTitle .
                    OPTIONAL {
                        ?workTitleId    bf:qualifier    ?qualifier .
                    }
                }
                OPTIONAL {
                    ?associatedResourceWorkId   bf:identifiedBy     ?identifiedById .
                    ?identifiedById             rdf:type            bf:Issn ;
                                                rdf:value           ?issn .
                }
                OPTIONAL {
                    ?associatedResourceWorkId   bflc:appliesTo      ?appliesToId .
                    ?appliesToId                rdf:type            bflc:AppliesTo ;
                                                rdfs:label          ?appliesTo .
                }
                OPTIONAL {
                    ?associatedResourceWorkId   bf:role             ?roleId .
                    ?roleId                     rdf:type            bf:Role ;
                                                rdfs:label          ?role .
                }
            }
        """

        val workQuery = conn.prepareTupleQuery(workDataQuery)
        workQuery.setBinding("id", id)

        workQuery.evaluate().use { result ->
            return result.map { workResult ->
                val associatedResourceWorkId = workResult.getValue("associatedResourceWorkId")
                val appliesTo = workResult.getValue("appliesTo")
                val mainEntryHeading = workResult.getValue("mainEntryHeading")
                val workTitle = workResult.getValue("workTitle")
                val qualifier = workResult.getValue("qualifier")
                val languageId = workResult.getValue("languageId")
                val relationshipId = workResult.getValue(linkingType)
                val relationship = workResult.getValue("relationship")
                val abbreviatedTitle = workResult.getValue("abbreviatedTitle")
                val issn = workResult.getValue("issn")
                val roleId = workResult.getValue("roleId")
                val role = workResult.getValue("role")

                val instanceQueryString = """
                    PREFIX bf: <$BIBFRAME_ONTOLOGY>
                    PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
                    SELECT DISTINCT ?associatedResourceInstanceId ?editionStatement ?provisionActivityStatement ?placeId ?part ?physicalDescription ?seriesStatement ?note ?otherItemIdentifier ?reportNumber ?instanceTitle ?strn ?lccn ?ocn ?assignerId ?assigner ?coden ?isbn
                    WHERE {
                        ?id                             bf:relation             ?relationId .
                        ?relationId                     rdf:type                bf:Relation ;
                                                        bf:relationship         ?relationshipId .
                        ?relationshipId                 rdf:type                bf:Relationship .
                        ?relationId                     bf:associatedResource   ?associatedResourceWorkId .
                        ?associatedResourceWorkId       rdf:type                bf:Work ;
                                                        bf:hasInstance          ?associatedResourceInstanceId .
                        ?associatedResourceInstanceId   rdf:type                bf:Instance .
                        FILTER(?relationshipId = <$linkingType>)
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:editionStatement     ?editionStatement .
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:provisionActivityStatement ?provisionActivityStatement .
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:provisionActivity    ?paId .
                            ?paId                           rdf:type                bf:ProvisionActivity ;
                                                            bf:place                ?placeId .
                            ?placeId                        rdf:type                bf:Place .
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:part                 ?part .
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:extant               ?extentId .
                            ?extentId                       rdf:type                bf:Extent ;
                                                            rdfs:label              ?physicalDescription .
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:seriesStatement      ?seriesStatement .
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:note                 ?noteId .
                            ?noteId                         rdf:type                bf:Note ;
                                                            rdfs:label              ?note .
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:identifiedBy         ?localId .
                            ?localId                        rdf:type                bf:Local ;
                                                            rdf:value               ?otherItemIdentifier .
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:identifiedBy         ?reportNumberId .
                            ?reportNumberId                 rdf:type                bf:ReportNumber ;
                                                            rdf:value               ?reportNumber .
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:title                ?titleId .
                            ?titleId                        rdf:type                bf:Title ;
                                                            bf:mainTitle            ?instanceTitle .
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:identifiedBy         ?strnId .
                            ?strnId                         rdf:type                bf:Strn ;
                                                            rdf:value               ?strn .
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:identifiedBy         ?lccnId .
                            ?lccnId                         rdf:type                bf:Lccn ;
                                                            rdf:value               ?lccn .
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:identifiedBy         ?ocnId .
                            ?ocnId                          rdf:type                bf:Identifier ;
                                                            rdf:value               ?ocn .
                            OPTIONAL {
                                ?ocnId          bf:assigner     ?assignerId .
                                ?assignerId     rdf:type        bf:Agent ;
                                                bf:code         ?assigner .
                            }
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:identifiedBy         ?codenId .
                            ?codenId                        rdf:type                bf:Coden ;
                                                            rdf:value               ?coden .
                        }
                        OPTIONAL {
                            ?associatedResourceInstanceId   bf:identifiedBy         ?isbnId .
                            ?isbnId                         rdf:type                bf:Isbn ;
                                                            rdf:value               ?isbn .
                        }
                    }
                """.trimIndent()

                val instanceQuery = conn.prepareTupleQuery(instanceQueryString)
                instanceQuery.setBinding("id", id)
                instanceQuery.setBinding("associatedResourceWorkId", associatedResourceWorkId)

                return instanceQuery.evaluate().use { instanceResult ->
                    instanceResult.map {
                        val edition = it.getValue("editionStatement")
                        val provisionActivityStatement = it.getValue("provisionActivityStatement")
                        val placeId = it.getValue("placeId")
                        val part = it.getValue("part")
                        val physicalDescription = it.getValue("physicalDescription")
                        val seriesStatement = it.getValue("seriesStatement")
                        val note = it.getValue("note")
                        val otherItemIdentifier = it.getValue("otherItemIdentifier")
                        val reportNumber = it.getValue("reportNumber")
                        val instanceTitle = it.getValue("instanceTitle")
                        val strn = it.getValue("strn")
                        val lccn = it.getValue("lccn")
                        val otherControlNumber = it.getValue("ocn")
                        val otherControlNumberAssigner = it.getValue("assigner")
                        val coden = it.getValue("coden")
                        val isbn = it.getValue("isbn")

                        LinkingEntry(
                            appliesTo,
                            mainEntryHeading,
                            edition,
                            qualifier,
                            provisionActivityStatement,
                            languageId,
                            placeId,
                            part,
                            physicalDescription,
                            relationship ?: relationshipId,
                            seriesStatement,
                            note,
                            otherItemIdentifier,
                            abbreviatedTitle,
                            reportNumber,
                            workTitle,
                            instanceTitle,
                            strn,
                            lccn,
                            otherControlNumber,
                            otherControlNumberAssigner,
                            issn,
                            coden,
                            isbn,
                            roleId,
                            role
                        )
                    }.toList()
                }
            }.toList()
        }
    }

    // TODO :   $c (qualifier) doesn't exist in the MARC21 standard?
    //          $e (language) doesn't exist in the MARC21 standard?
    //          $f (place) doesn't exist in the MARC21 standard?
    //          Are roles (and a few others possibly lists?)
    @JvmRecord
    private data class LinkingEntry(
        val appliesTo: Value?,
        val mainEntryHeading: Value?,
        val edition: Value?,
        val qualifier: Value?,
        val provisionActivityStatement: Value?,
        val languageId: Value?,
        val place: Value?,
        val part: Value?, // TODO : split out?
        val physicalDescription: Value?,
        val relationshipInformation: Value?,
        val seriesStatement: Value?,
        val note: Value?,
        val otherItemIdentifier: Value?,
        val abbreviatedTitle: Value?,
        val reportNumber: Value?,
        val workTitle: Value?,
        val instanceTitle: Value?,
        val standardTechnicalReportNumber: Value?,
        val lccn: Value?,
        val otherControlNumber: Value?,
        val otherControlNumberAssigner: Value?,
        val issn: Value?,
        val coden: Value?,
        val isbn: Value?,
        val roleId: Value?,
        val role: Value?
    )
}