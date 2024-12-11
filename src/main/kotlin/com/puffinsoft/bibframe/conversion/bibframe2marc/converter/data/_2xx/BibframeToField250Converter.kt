package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.LanguageScriptLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.sail.nativerdf.model.NativeLiteral
import org.marc4k.marc.DataField
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField250Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        val editionStatementData = query(conn, workData.primaryInstanceId)

        when (editionStatementData.size) {
            1 -> record.dataFields.add(build250DataField(workData, editionStatementData[false]!!, false))
            2 -> {
                record.dataFields.add(build250DataField(workData, editionStatementData[false]!!, true))
                workData.addAlternateGraphicRepresentations(build880DataField(workData, editionStatementData[true]!!))
            }
        }
    }

    private fun build250DataField(workData: WorkData, data: Value, hasAlternate: Boolean): DataField {
        val builder = DataFieldBuilder().apply { tag = "250" }

        if (hasAlternate) {
            addSubfieldIfExists(builder, '6', "880-${workData.getNextAlternateGraphicRepresentationOccurrence()}")
        }

        addSubfieldIfExists(builder, 'a', data)

        return builder.build()
    }

    private fun build880DataField(workData: WorkData, data: Value): DataField {
        val builder = DataFieldBuilder().apply { tag = "880" }

        addSubfieldIfExists(builder, '6', "250-${workData.getNextAlternateGraphicRepresentationOccurrence()}/${LanguageScriptLookup.lookup(data) ?: ""}")
        addSubfieldIfExists(builder, 'a', data)

        return builder.build()
    }

    private fun query(conn: RepositoryConnection, id: Value): Map<Boolean, Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT DISTINCT ?editionStatement
            WHERE {
                ?id         bf:editionStatement        ?editionStatement .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { it.getValue("editionStatement") }.associateBy { (it as NativeLiteral).language.isPresent }
        }
    }
}