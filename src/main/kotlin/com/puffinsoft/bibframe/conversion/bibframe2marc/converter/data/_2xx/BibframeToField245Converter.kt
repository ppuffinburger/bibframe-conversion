package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.LanguageScriptLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField245Converter : BibframeToMarcConverter {
    // TODO : specs say instance, but works also have the Title.  Code also deals with Works.  Do part numbers/names have languages?
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.primaryInstanceId).let {
            val builder245 = DataFieldBuilder().apply {
                tag = "245"
                indicator1 = if (workHasPrimaryContribution(conn, workData.workId)) '1' else '0'
                indicator2 = getNonFilingCharacters(it.nonSortNum)
            }

            if (it.hasAlternate()) {
                addSubfieldIfExists(builder245, '6', "880-${workData.getNextAlternateGraphicRepresentationOccurrence()}")
            }

            addSubfieldIfExists(builder245, 'a', it.mainTitle)
            addSubfieldIfExists(builder245, 'b', it.subtitle)
            addSubfieldIfExists(builder245, 'c', it.responsibilityStatement)
            addSubfieldIfExists(builder245, 'n', it.partNumber)
            addSubfieldIfExists(builder245, 'p', it.partName)

            record.dataFields.add(builder245.build())

            if (it.hasAlternate()) {
                val builder880 = DataFieldBuilder().apply {
                    tag = "880"
                    indicator1 = if (workHasPrimaryContribution(conn, workData.workId)) '1' else '0'
                    indicator2 = getNonFilingCharacters(it.nonSortNum)
                }

                addSubfieldIfExists(builder880, '6', "245-${workData.getNextAlternateGraphicRepresentationOccurrence()}/${it.mainTitleWithLang?.let { t -> LanguageScriptLookup.lookup(t) } ?: ""}")
                addSubfieldIfExists(builder880, 'a', it.mainTitleWithLang)
                addSubfieldIfExists(builder880, 'b', it.subtitleWithLang)
                addSubfieldIfExists(builder880, 'c', it.responsibilityStatementWithLang)
                addSubfieldIfExists(builder880, 'n', it.partNumber)
                addSubfieldIfExists(builder880, 'p', it.partName)

                workData.addAlternateGraphicRepresentations(builder880.build())
            }
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): TitleData {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT DISTINCT ?mainTitle ?mainTitleWithLang ?nonSortNum ?subtitle ?subtitleWithLang ?responsibilityStatement ?responsibilityStatementWithLang ?partNumber ?partName
            WHERE {
                ?id         bf:title        ?titleId .
                ?titleId    rdf:type        bf:Title ;
                            bf:mainTitle    ?mainTitle .
                FILTER(LANG(?mainTitle) = "")
                OPTIONAL {
                    ?titleId bflc:nonSortNum ?nonSortNum .
                }
                OPTIONAL {
                    ?titleId bf:subtitle ?subtitle .
                    FILTER(LANG(?subtitle) = "")
                }
                OPTIONAL {
                    ?id bf:responsibilityStatement ?responsibilityStatement .
                    FILTER(LANG(?responsibilityStatement) = "")
                }
                OPTIONAL {
                    ?titleId    rdf:type        bf:Title ;
                                bf:mainTitle    ?mainTitleWithLang .
                    FILTER(LANG(?mainTitleWithLang) != "")
                }
                OPTIONAL {
                    ?titleId    rdf:type    bf:Title ;
                                bf:subtitle ?subtitleWithLang .
                    FILTER(LANG(?subtitleWithLang) != "")
                }
                OPTIONAL {
                    ?id bf:responsibilityStatement ?responsibilityStatementWithLang .
                    FILTER(LANG(?responsibilityStatementWithLang) != "")
                }
                OPTIONAL {
                    ?titleId bf:partNumber ?partNumber .
                }
                OPTIONAL {
                    ?titleId bf:partName ?partName .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { TitleData(it.getValue("mainTitle"), it.getValue("nonSortNum"), it.getValue("subtitle"), it.getValue("responsibilityStatement"), it.getValue("partNumber"), it.getValue("partName"), it.getValue("mainTitleWithLang"), it.getValue("subtitleWithLang"), it.getValue("responsibilityStatementWithLang")) }.first()
        }
    }

    private fun workHasPrimaryContribution(conn: RepositoryConnection, id: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK
            WHERE {
                ?id             bf:contribution ?contributionId .
                ?contributionId rdf:type        bf:PrimaryContribution .
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("id", id)

        return query.evaluate()
    }

    @JvmRecord
    private data class TitleData(val mainTitle: Value, val nonSortNum: Value?, val subtitle: Value?, val responsibilityStatement: Value?, val partNumber: Value?, val partName: Value?, val mainTitleWithLang: Value?, val subtitleWithLang: Value?, val responsibilityStatementWithLang: Value?) {
        fun hasAlternate(): Boolean {
            return mainTitleWithLang != null
        }
    }
}