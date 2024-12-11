package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField084Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "084" }

            addSubfieldIfExists(builder, 'a', it.classificationPortion)
            addSubfieldIfExists(builder, 'b', it.itemPortion)
            addSubfieldIfExists(builder, 'q', it.assigner)
            addSubfieldIfExists(builder, '2', it.source)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<OtherClassificationNumber> {
        val sudocsUri = lookupClassificationSchemeUri("sudocs")
        val cacodocUri = lookupClassificationSchemeUri("cacodoc")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?classificationPortion ?itemPortion ?assigner ?source
            WHERE {
                ?id                 bf:classification           ?classificationId .
                ?classificationId   rdf:type                    bf:Classification ;
                                    bf:classificationPortion    ?classificationPortion ;
                                    bf:source                   ?sourceId .
                ?sourceId           rdf:type                    bf:Source ;
                                    bf:code                     ?source .
                OPTIONAL {
                    ?classificationId bf:itemPortion ?itemPortion .
                }
                OPTIONAL {
                    ?classificationId   bf:assigner ?assignerId .
                    ?assignerId         rdfs:value  ?assigner .
                    FILTER(?assignerId != <http://id.loc.gov/vocabulary/organizations/dnal>)
                }
                FILTER(?sourceId NOT IN (<$sudocsUri>, <$cacodocUri>))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { OtherClassificationNumber(it.getValue("classificationPortion"), it.getValue("itemPortion"), it.getValue("assigner"), it.getValue("source")) }.toList()
        }
    }

    @JvmRecord
    private data class OtherClassificationNumber(val classificationPortion: Value, val itemPortion: Value?, val assigner: Value?, val source: Value)
}