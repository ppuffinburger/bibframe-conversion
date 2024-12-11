package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.DateUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.StatusCodeLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.TypeOfMaterial.*
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.TypeOfMaterialLookup
import kotlinx.datetime.LocalDate
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.edtf4k.*
import org.marc4k.marc.ControlField
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField008Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        with(StringBuilder(getDateEnteredOnFile(workData.creationDate.date))) {
            val marcDates = queryMarcDates(conn, workData.primaryInstanceId)?.marcDateString
            if (marcDates == null) {
                append('s').append(DateUtils.formatYear(workData.creationDate.date)).append("    ")
            } else {
                append(marcDates)
            }

            val publicationPlace = queryPublicationPlace(conn, workData.primaryInstanceId)
            if (publicationPlace == null) {
                append("xx ")
            } else {
                append(TextUtils.getCodeStringFromUrl(publicationPlace).padEnd(3, ' '))
            }

            when (TypeOfMaterialLookup.lookup(record)) {
                BOOK -> append(BibframeToField008ElementsBookConverter.convert(conn, workData))
                COMPUTER_FILE -> append(BibframeToField008ElementsComputerFileConverter.convert(conn, workData))
                MAP -> append(BibframeToField008ElementsMapConverter.convert(conn, workData))
                MUSIC -> append(BibframeToField008ElementsMusicConverter.convert(conn, workData))
                CONTINUING_RESOURCE -> append(BibframeToField008ElementsContinuingResourceConverter.convert(conn, workData))
                VISUAL_MATERIAL -> append(BibframeToField008ElementsVisualMaterialConverter.convert(conn, workData))
                MIXED_MATERIAL -> append(BibframeToField008ElementsMixedMaterialConverter.convert(conn, workData))
                null -> throw RuntimeException("Could not figure out the type of material")
            }

            val language = queryLanguage(conn, workData.workId)
            if (language == null) {
                append("   ")
            } else {
                append(TextUtils.getCodeStringFromUrl(language))
            }

            append(" ")

            append(getCatalogingSource(conn, workData))

            record.controlFields.add(ControlField("008", toString()))
        }
    }

    private fun queryPublicationPlace(conn: RepositoryConnection, instanceId: Value): Value? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?publicationPlace
            WHERE {
                ?instanceId bf:provisionActivity    ?pa .
                ?pa         rdf:type                bf:ProvisionActivity ;
                            rdf:type                bf:Publication ;
                            bf:place                ?publicationPlace .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("instanceId", instanceId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("publicationPlace") }.map { it.getValue("publicationPlace") }.firstOrNull()
        }
    }

    private fun queryLanguage(conn: RepositoryConnection, workId: Value): Value? {
        val languageQueryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?language
            WHERE {
                ?workId     bf:language ?language .
                ?language   rdf:type    bf:Language .
            }""".trimIndent()

        val languageQuery = conn.prepareTupleQuery(languageQueryString)
        languageQuery.setBinding("workId", workId)

        languageQuery.evaluate().use { result ->
            return result.filter { it.hasBinding("language") }.map { it.getValue("language") }.firstOrNull()
        }
    }

    private fun getDateEnteredOnFile(creationDate: LocalDate): String {
        return DateUtils.formatDateEnteredOnFile(creationDate)
    }

    private fun queryMarcDates(conn: RepositoryConnection, id: Value): MarcDates? {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?publicationDate ?publicationStatus ?productionDate ?distributionDate ?copyrightDate ?note
            WHERE {
                OPTIONAL {
                    ?id         bf:provisionActivity    ?pubId .
                    ?pubId      rdf:type                bf:ProvisionActivity ;
                                rdf:type                bf:Publication ;
                                bf:date                 ?publicationDate .
                    OPTIONAL {
                        ?pubId bf:status ?publicationStatus .
                    }
                }
                OPTIONAL {
                    ?id         bf:provisionActivity    ?prodId .
                    ?prodId     rdf:type                bf:ProvisionActivity ;
                                rdf:type                bf:Production ;
                                bf:date                 ?productionDate .
                }
                OPTIONAL {
                    ?id         bf:provisionActivity    ?distId .
                    ?distId     rdf:type                bf:ProvisionActivity ;
                                rdf:type                bf:Distribution ;
                                bf:date                 ?distributionDate .
                }
                OPTIONAL {
                    ?id         bf:copyrightDate        ?copyrightDate .
                    FILTER(DATATYPE(?copyrightDate) = <http://id.loc.gov/datatypes/edtf>)
                }
                OPTIONAL {
                    ?id         bf:note     ?noteId .
                    ?noteId     rdf:type    bf:Note ;
                                rdfs:label  ?note .
                    FILTER(LCASE(STR(?note)) IN ("bulk collection dates", "inclusive collection dates"))
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { MarcDates(it.getValue("publicationDate"), it.getValue("publicationStatus"), it.getValue("productionDate"), it.getValue("distributionDate"), it.getValue("copyrightDate"), it.getValue("note")) }.firstOrNull()
        }
    }

    private fun getCatalogingSource(conn: RepositoryConnection, workData: WorkData): Char {
        val catalogingSourceData = queryCatalogingSource(conn, workData)

        if (catalogingSourceData != null) {
            if (catalogingSourceData.dlcAgent != null) {
                return ' '
            }
            if (catalogingSourceData.pccAuthentication != null) {
                return 'c'
            }
            if (catalogingSourceData.lccopycatAuthentication != null) {
                return 'd'
            }
        }

        return ' '
    }

    private fun queryCatalogingSource(conn: RepositoryConnection, workData: WorkData): CatalogingSourceData? {
        val ids = workData.getWorkAndAllInstanceIdsAsStrings().joinToString { "<$it>" }
        val pccUri = lookupAuthenticationActionUri("pcc")
        val lccopycatUri = lookupAuthenticationActionUri("lccopycat")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?dlcAgent ?pccAuthentication ?lccopycatAuthentication
            WHERE {
                {
                    SELECT ?dlcAgent
                    WHERE {
                        ?id                 bf:adminMetadata    ?adminMetadataId .
                        ?adminMetadataId    bf:status           <${StatusCodeLookup.lookup("n")?.uri}> ;
                                            bf:agent            ?dlcAgent .
                        ?dlcAgent           rdf:type            bf:Agent .
                        FILTER(?dlcAgent = <http://id.loc.gov/vocabulary/organizations/dlc>)
                        FILTER(?id IN ($ids))
                    }
                    LIMIT 1
                }
                {
                    SELECT ?pccAuthentication
                    WHERE {
                        ?id                         bf:adminMetadata                ?adminMetadataId .
                        ?adminMetadataId            bf:descriptionAuthentication    ?pccAuthentication .
                        ?pccAuthentication          rdf:type                        bf:DescriptionAuthentication .
                        FILTER(?pccAuthentication = <$pccUri>)
                        FILTER(?id IN ($ids))
                    }
                    LIMIT 1
                }
                {
                    SELECT ?lccopycatAuthentication
                    WHERE {
                        ?id                         bf:adminMetadata                ?adminMetadataId .
                        ?adminMetadataId            bf:descriptionAuthentication    ?lccopycatAuthentication .
                        ?lccopycatAuthentication    rdf:type                        bf:DescriptionAuthentication .
                        FILTER(?lccopycatAuthentication = <$lccopycatUri>)
                        FILTER(?id IN ($ids))
                    }
                    LIMIT 1
                }
            }
            """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)

        query.evaluate().use { result ->
            return result.map { CatalogingSourceData(it.getValue("dlcAgent"), it.getValue("pccAuthentication"), it.getValue("lccopycatAuthentication")) }.firstOrNull()
        }
    }

    @JvmRecord
    private data class CatalogingSourceData(val dlcAgent: Value?, val pccAuthentication: Value?, val lccopycatAuthentication: Value?)

    @JvmRecord
    private data class MarcDates(val publicationDate: Value?, val publicationStatus: Value?, val productionDate: Value?, val distributionDate: Value?, val copyrightDate: Value?, val note: Value?) {
        val marcDateString: String?
            // X p - Distribution and Production
            // X i, k - Production based on Note
            // X t - Publication and Copyright
            // X c - Publication and Status current
            // X d - Publication and Status ceased
            // X q - Publication and dates are approximate, uncertain, or both
            // X m - Publication and with both dates and not approximate, uncertain, or both
            // X e - Publication and "detailed" date
            // X s - Publication with no End date
            get() {
                if (distributionDate != null && productionDate != null) {
                    val distribution: EdtfDate = EdtfDateFactory.parse(distributionDate.stringValue()) as EdtfDate
                    val production: EdtfDate = EdtfDateFactory.parse(productionDate.stringValue()) as EdtfDate
                    return "p${encodeMarcYear(distribution.year.toString())}${encodeMarcYear(production.year.toString())}"
                }

                if (productionDate != null && note != null) {
                    val production: EdtfDate = EdtfDateFactory.parse(productionDate.stringValue()) as EdtfDate
                    return parseProductionNotes(note).toString() + encodeMarcYear(production.year.toString()) + "    "
                }

                if (publicationDate != null && copyrightDate != null) {
                    val edtfPublicationDate = EdtfDateFactory.parse(publicationDate.stringValue())
                    val publicationYearStart: EdtfDateComponent = if (edtfPublicationDate is EdtfDate) edtfPublicationDate.year else (edtfPublicationDate as EdtfDatePair).start.year

                    val copyright: EdtfDate = EdtfDateFactory.parse(copyrightDate.stringValue()) as EdtfDate
                    return "t${encodeMarcYear(publicationYearStart.toString())}${encodeMarcYear(copyright.year.toString())}"
                }

                if (publicationDate != null) {
                    val edtfPublicationDate = EdtfDateFactory.parse(publicationDate.stringValue())
                    val publicationYearStart: EdtfDateComponent = if (edtfPublicationDate is EdtfDate) edtfPublicationDate.year else (edtfPublicationDate as EdtfDatePair).start.year

                    if (isValidYear(publicationYearStart)) {
                        val publicationEnd: EdtfDate? = if (edtfPublicationDate is EdtfDate) null else (edtfPublicationDate as EdtfDatePair).end

                        if (publicationEnd == null || publicationEnd.status == EdtfDateStatus.UNUSED || publicationEnd.status == EdtfDateStatus.UNKNOWN) {
                            // if start contains month then we are a "detailed" date
                            if (edtfPublicationDate is EdtfDate) {
                                val month = edtfPublicationDate.month

                                if (month.hasValue()) {
                                    return "e${encodeMarcYear(publicationYearStart.toString())}${encodeMarcMonth(month.toString())}${encodeMarcDay(edtfPublicationDate.day)}"
                                }
                            }

                            return "s${encodeMarcYear(publicationYearStart.toString())}    "
                        } else {
                            // TODO : what about long years or exponents?
                            val publicationYearEndString = getPublicationYearEndString(publicationEnd)

                            val publicationStatusValue = parsePublicationStatus(publicationStatus)
                            if (publicationStatusValue != null) {
                                return "$publicationStatusValue${encodeMarcYear(publicationYearStart.toString())}$publicationYearEndString"
                            } else {
                                val publicationYearEnd: EdtfDateComponent = publicationEnd.year

                                return if (publicationYearEnd.isUncertain || publicationYearEnd.isApproximate) {
                                    "q${encodeMarcYear(publicationYearStart.toString())}${encodeMarcYear(publicationYearEndString)}"
                                } else {
                                    "m${encodeMarcYear(publicationYearStart.toString())}${encodeMarcYear(publicationYearEndString)}"
                                }
                            }
                        }
                    }
                }

                return null
            }

        fun getPublicationYearEndString(publicationEnd: EdtfDate): String {
            val publicationYearEndString = when (publicationEnd.status) {
                EdtfDateStatus.NORMAL -> encodeMarcYear(publicationEnd.year.toString())
                EdtfDateStatus.OPEN -> "9999"
                else -> "    "
            }
            return publicationYearEndString
        }

        fun encodeMarcYear(year: String): String {
            return year.replace('X', 'u')
        }

        fun encodeMarcMonth(month: String): String {
            return month.replace('X', 'u')
        }

        fun encodeMarcDay(day: EdtfDateComponent): String {
            if (day.hasValue()) {
                return day.toString().replace('X', 'u')
            }
            return "  "
        }

        fun parseProductionNotes(note: Value): Char {
            return when (note.stringValue()) {
                "inclusive collection dates" -> 'i'
                "bulk collection dates" -> 'k'
                else -> ' '
            }
        }

        fun parsePublicationStatus(publicationStatus: Value?): Char? {
            val currentUri = StatusCodeLookup.lookup("current")?.uri ?: throw RuntimeException("Could not find 'current' in status codes")
            val ceasedUri = StatusCodeLookup.lookup("ceased")?.uri ?: throw RuntimeException("Could not find 'ceased' in status codes")

            if (publicationStatus != null) {
                return when (publicationStatus.stringValue()) {
                    currentUri -> 'c'
                    ceasedUri -> 'd'
                    else -> null
                }
            }
            return null
        }

        fun isValidYear(year: EdtfDateComponent): Boolean {
            if (year.isInvalid) {
                return false
            }

            if (year.isApproximate || year.isUncertain) {
                return false
            }

            return year.hasValue()
        }
    }
}