package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._8xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField856Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        workData.getAllInstanceIds().forEach { instanceId ->
            query(conn, instanceId).forEach {
                val builder = DataFieldBuilder().apply {
                    tag = "856"
                    indicator1 = getIndicator1(it)
                    indicator2 = getIndicator2(it)
                }

                addSubfieldIfExists(builder, '3', it.mainTitle)

                val electronicLocator = if (it.electronicLocator.stringValue().startsWith("http://bf2m/")) {
                    it.electronicLocator.stringValue().removePrefix("http://bf2m/")
                } else {
                    it.electronicLocator.stringValue()
                }
                addSubfieldIfExists(builder, 'u', electronicLocator)

                addSubfieldIfExists(builder, 'z', it.note)

                record.dataFields.add(builder.build())
            }
        }
    }

    private fun getIndicator1(data: ElectronicLocationAndAccess): Char {
        return when (data.type.stringValue()) {
            "1" -> {
                val uri = data.electronicLocator.stringValue()
                if (uri.startsWith("mailto")) {
                    '0'
                } else if (uri.startsWith("ftp")) {
                    '1'
                } else if (uri.startsWith("telnet") || uri.startsWith("ssh")) {
                    '2'
                } else if (uri.startsWith("http")) {
                    '4'
                } else {
                    ' '
                }
            }
            "2" -> '4'
            else -> ' '
        }
    }

    private fun getIndicator2(data: ElectronicLocationAndAccess): Char {
        return when (data.type.stringValue()) {
            "1" -> '8'
            "2" -> '2'
            else -> ' '
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<ElectronicLocationAndAccess> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT DISTINCT ?electronicLocator ?type ?mainTitle ?note
            WHERE {
                {
                    ?id         bf:electronicLocator        ?electronicLocator .
                    BIND("1" AS ?type)
                    OPTIONAL {
                        ?id         bf:note     ?noteId .
                        ?noteId     rdf:type    bf:Note ;
                                    rdfs:label  ?note .
                    }
                    OPTIONAL {
                        ?id         bf:title        ?titleId .
                        ?titleId    rdf:type        bf:Title ;
                                    bf:mainTitle    ?mainTitle .
                        FILTER(!CONTAINS(STR(?mainTitle), "resource"))
                    }
                }
                UNION
                {
                    ?id         bf:supplementaryContent     ?contentId .
                    ?contentId  rdf:type                    bf:SupplementaryContent ;
                                bf:electronicLocator        ?electronicLocator .
                    BIND("2" AS ?type)
                    OPTIONAL {
                        ?contentId  bf:note     ?noteId .
                        ?noteId     rdf:type    bf:Note ;
                                    rdfs:label  ?note .
                    }
                    OPTIONAL {
                        ?id         bf:title        ?titleId .
                        ?titleId    rdf:type        bf:Title ;
                                    bf:mainTitle    ?mainTitle .
                        FILTER(!CONTAINS(STR(?mainTitle), "resource"))
                    }
                }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { ElectronicLocationAndAccess(it.getValue("electronicLocator"), it.getValue("type"), it.getValue("mainTitle"), it.getValue("note")) }.toList()
        }
    }

    @JvmRecord
    private data class ElectronicLocationAndAccess(val electronicLocator: Value, val type: Value, val mainTitle: Value?, val note: Value?)
}