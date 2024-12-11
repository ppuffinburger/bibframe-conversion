package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._4xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.LanguageScriptLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField490Converter : BibframeToMarcConverter {
    // TODO : do parallel titles also come with alternate languages?
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        val seriesCount = querySeriesCount(conn, workData.workId)

        if (seriesCount > 0) {
            val cancelledOrInvalidUri = lookupStatusCodeUri("cancinv")
            val incorrectUri = lookupStatusCodeUri("incorrect")

            query(conn, workData.workId).forEach {
                val builder = DataFieldBuilder().apply {
                    tag = "490"
                    indicator1 = if (it.hasTraceStatus) '1' else '0'
                }

                if (it.hasAlternate()) {
                    addSubfieldIfExists(builder, '6', "880-${workData.getNextAlternateGraphicRepresentationOccurrence()}")
                }

                addSubfieldIfExists(builder, '3', it.appliesTo)

                if (it.parallelTitle != null) {
                    addSubfieldIfExists(builder, 'a', it.mainTitle.stringValue() + " =")
                    addSubfieldIfExists(builder, 'a', it.parallelTitle)
                } else {
                    addSubfieldIfExists(builder, 'a', it.mainTitle)
                }

                if (it.issn != null) {
                    if (it.issnStatus != null) {
                        when (it.issnStatus.stringValue()) {
                            incorrectUri -> addSubfieldIfExists(builder, 'y', it.issn)
                            cancelledOrInvalidUri -> addSubfieldIfExists(builder, 'z', it.issn)
                        }
                    } else {
                        addSubfieldIfExists(builder, 'x', it.issn)
                    }
                }

                addSubfieldIfExists(builder, 'v', it.seriesEnumeration)

                record.dataFields.add(builder.build())

                if (it.hasAlternate()) {
                    val builder880 = DataFieldBuilder().apply {
                        tag = "880"
                        indicator1 = if (it.hasTraceStatus) '1' else '0'
                    }

                    addSubfieldIfExists(builder880, '6', "490-${workData.getNextAlternateGraphicRepresentationOccurrence()}/${it.mainTitleWithLang?.let { t -> LanguageScriptLookup.lookup(t) } ?: ""}")

                    addSubfieldIfExists(builder880, '3', it.appliesTo)

                    if (it.parallelTitle != null) {
                        addSubfieldIfExists(builder880, 'a', it.mainTitleWithLang?.stringValue() + " =")
                        addSubfieldIfExists(builder880, 'a', it.parallelTitle)
                    } else {
                        addSubfieldIfExists(builder880, 'a', it.mainTitleWithLang)
                    }

                    if (it.issn != null) {
                        if (it.issnStatus != null) {
                            when (it.issnStatus.stringValue()) {
                                incorrectUri -> addSubfieldIfExists(builder880, 'y', it.issn)
                                cancelledOrInvalidUri -> addSubfieldIfExists(builder880, 'z', it.issn)
                            }
                        } else {
                            addSubfieldIfExists(builder880, 'x', it.issn)
                        }
                    }

                    addSubfieldIfExists(builder880, 'v', it.seriesEnumeration)

                    workData.addAlternateGraphicRepresentations(builder880.build())
                }
            }
        }
    }

    private fun querySeriesCount(conn: RepositoryConnection, id: Value): Int {
        val transcribedUri = lookupStatusCodeUri("t")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT (COUNT(*) AS ?count)
            WHERE {
                {
                    ?id                     bf:relation             ?relationId .
                    ?relationId             rdf:type                bf:Relation ;
                                            bf:relationship         bf:hasSeries ;
                                            bf:associatedResource   ?associatedResourceId .
                    ?associatedResourceId   rdf:type                bf:Series ;
                                            rdf:type                bflc:Uncontrolled ;
                                            bf:status               <$transcribedUri> ;
                }
                UNION
                {
                    ?id             bflc:relationship   ?relationshipId .
                    ?relationshipId rdf:type            bflc:Relationship ;
                                    bflc:relation       <http://id.loc.gov/ontologies/bibframe/hasSeries> ;
                                    bf:relatedTo        ?relatedToId .
                    ?relatedToId    rdf:type            <http://id.loc.gov/ontologies/bibframe/Series> ;
                                    rdf:type            <http://id.loc.gov/ontologies/bflc/Uncontrolled> ;
                                    bf:status           ?statusId .
                    ?statusId       rdf:type            bf:Status .
                    FILTER(?statusId = <$transcribedUri>)
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.stream()
                .map { bs: BindingSet -> bs.getValue("count") }
                .map { `val`: Value -> `val`.stringValue().toInt() }
                .findFirst().orElse(0)
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<SeriesStatementData> {
        val transcribedUri = lookupStatusCodeUri("t")
        val tracedUri = lookupStatusCodeUri("tr")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT DISTINCT ?mainTitle ?mainTitleWithLang ?parallelTitle ?issn ?issnStatus ?seriesEnumeration ?appliesTo ?traceStatus
            WHERE {
                {
                    ?id                     bf:relation             ?relationId .
                    ?relationId             rdf:type                bf:Relation ;
                                            bf:relationship         bf:hasSeries ;
                                            bf:associatedResource   ?associatedResourceId .
                    ?associatedResourceId   rdf:type                bf:Series ;
                                            rdf:type                bflc:Uncontrolled ;
                                            bf:status               <$transcribedUri> ;
                                            bf:title                ?titleId .
                    ?titleId                rdf:type                bf:Title ;
                                            bf:mainTitle            ?mainTitle .
                    FILTER(LANG(?mainTitle) = "")
                    OPTIONAL {
                        ?associatedResourceId   bf:title        ?titleId .
                        ?titleId                rdf:type        bf:ParallelTitle ;
                                                bf:mainTitle    ?parallelTitle .
                    }
                    OPTIONAL {
                        ?associatedResourceId   bf:identifiedBy ?identifiedById .
                        ?identifiedById         rdf:type        bf:ISSN ;
                                                rdf:value       ?issn .
                        OPTIONAL {
                            ?identifiedById bf:status   ?issnStatus .
                            ?issnStatus     rdf:type    bf:Status .
                        }
                    }
                    OPTIONAL {
                        ?relationId   bf:seriesEnumeration    ?seriesEnumeration .
                    }
                    OPTIONAL {
                        ?associatedResourceId   bflc:appliesTo  ?appliesToId .
                        ?appliesToId            rdf:type        bflc:AppliesTo ;
                                                rdfs:label      ?appliesTo .
                    }
                    OPTIONAL {
                        ?titleId    rdf:type                bf:Title ;
                                    bf:mainTitle            ?mainTitleWithLang .
                        FILTER(LANG(?mainTitleWithLang) != "")
                    }
                    OPTIONAL {
                        ?associatedResourceId   bf:status   ?traceStatus .
                        FILTER(?traceStatus = <$tracedUri>)
                    }
                }
                UNION
                {
                    ?id             bflc:relationship   ?relationshipId .
                    ?relationshipId rdf:type            bflc:Relationship ;
                                    bflc:relation       <http://id.loc.gov/ontologies/bibframe/hasSeries> ;
                                    bf:relatedTo        ?relatedToId .
                    ?relatedToId    rdf:type            <http://id.loc.gov/ontologies/bibframe/Series> ;
                                    rdf:type            <http://id.loc.gov/ontologies/bflc/Uncontrolled> ;
                                    bf:status           <$transcribedUri> .
                    ?statusId       rdf:type            bf:Status .
                    ?relatedToId    bf:title            ?titleId .
                    ?titleId        rdf:type            bf:Title ;
                                    bf:mainTitle        ?mainTitle .
                    FILTER(LANG(?mainTitle) = "")
                    OPTIONAL {
                        ?relatedToId    bf:title        ?titleId .
                        ?titleId        rdf:type        bf:ParallelTitle ;
                                        bf:mainTitle    ?parallelTitle .
                    }
                    OPTIONAL {
                        ?relatedToId    bf:identifiedBy ?identifiedById .
                        ?identifiedById rdf:type        bf:ISSN ;
                                        rdf:value       ?issn .
                        OPTIONAL {
                            ?identifiedById bf:status   ?issnStatus .
                            ?issnStatus     rdf:type    bf:Status .
                        }
                    }
                    OPTIONAL {
                        ?relatedToId    bf:seriesEnumeration    ?seriesEnumeration .
                    }
                    OPTIONAL {
                        ?relatedToId    bflc:appliesTo  ?appliesToId .
                        ?appliesToId    rdf:type        bflc:AppliesTo ;
                                        rdfs:label      ?appliesTo .
                    }
                    OPTIONAL {
                        ?titleId    rdf:type                bf:Title ;
                                    bf:mainTitle            ?mainTitleWithLang .
                        FILTER(LANG(?mainTitleWithLang) != "")
                    }
                    OPTIONAL {
                        ?relatedToId    bf:status   ?traceStatus .
                        FILTER(?traceStatus = <$tracedUri>)
                    }
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map {
                SeriesStatementData(
                    it.getValue("mainTitle"),
                    it.getValue("parallelTitle"),
                    it.getValue("issn"),
                    it.getValue("issnStatus"),
                    it.getValue("seriesEnumeration"),
                    it.getValue("appliesTo"),
                    it.getValue("mainTitleWithLang"),
                    it.hasBinding("traceStatus")
                )
            }.toList()
        }
    }

    @JvmRecord
    private data class SeriesStatementData(val mainTitle: Value, val parallelTitle: Value?, val issn: Value?, val issnStatus: Value?, val seriesEnumeration: Value?, val appliesTo: Value?, val mainTitleWithLang: Value?, val hasTraceStatus: Boolean) {
        fun hasAlternate(): Boolean {
            return mainTitleWithLang != null
        }
    }
}