package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField300Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        workData.getAllInstanceIds().forEach { instanceId ->
            query(conn, instanceId)?.let {
                val builder = DataFieldBuilder().apply { tag = "300" }
                addSubfieldIfExists(builder, '3', it.appliesTo)
                addSubfieldIfExists(builder, 'a', it.extent)
                addSubfieldIfExists(builder, 'b', it.physicalNote)
                addSubfieldIfExists(builder, 'c', it.dimensions)
                addSubfieldIfExists(builder, 'e', it.accompanyingMaterialNote)
                record.dataFields.add(builder.build())
            }
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): PhysicalDescriptionData? {
        val physicalDetailsNoteUri = lookupNoteTypeUri("physical")
        val accompanyingMaterialNoteUri = lookupNoteTypeUri("accmat")

        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?extent ?physicalNote ?dimensions ?accompanyingMaterialNote ?appliesTo
            WHERE {
                ?id         bf:extent   ?extentId .
                ?extentId   rdf:type    bf:Extent ;
                            rdfs:label  ?extent .
                OPTIONAL {
                    ?extentId       bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
                OPTIONAL {
                    ?id   bf:dimensions   ?dimensions .
                } OPTIONAL {
                    ?id     bf:note     ?pnId .
                    ?pnId   rdf:type    bf:Note ;
                            rdf:type    <$physicalDetailsNoteUri> ;
                            rdfs:label  ?physicalNote .
                }
                OPTIONAL {
                    ?id     bf:note     ?amId .
                    ?amId   rdf:type    bf:Note ;
                            rdf:type    <$accompanyingMaterialNoteUri> ;
                            rdfs:label  ?accompanyingMaterialNote .
                    }
                }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { PhysicalDescriptionData(it.getValue("extent"), it.getValue("physicalNote"), it.getValue("dimensions"), it.getValue("accompanyingMaterialNote"), it.getValue("appliesTo")) }.firstOrNull()
        }
    }

    @JvmRecord
    private data class PhysicalDescriptionData(val extent: Value, val physicalNote: Value?, val dimensions: Value?, val accompanyingMaterialNote: Value?, val appliesTo: Value?)
}