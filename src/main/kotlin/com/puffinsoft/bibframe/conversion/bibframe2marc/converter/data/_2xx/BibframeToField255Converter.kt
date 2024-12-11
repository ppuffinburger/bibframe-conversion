package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._2xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField255Converter : BibframeToMarcConverter {
    // TODO : this works like the code, but it seems like these objects should be linked
    // MARC catalog
    //255	__	 |a Scale [ca. 1:62,500,000]. At equator ; |b Mercator proj. |c (W 180⁰--E 180⁰/N 80⁰--S 68⁰).
    //255	__	 |a Scale [ca. 1:60,000,000] |c (W 180⁰--E 180⁰/N 90⁰--S 90⁰).

    // Bibframe
    //255    ‡aScale [ca. 1:62,500,000]. At equator
    //255    ‡aScale [ca. 1:60,000,000]
    //255    ‡bMercator
    //255    ‡cW1800000 E1800000 N0800000 S0680000
    //255    ‡cW1800000 E1800000 N0900000 S0900000
    //255    ‡bMercator proj‡cW 180⁰--E 180⁰/N 80⁰--S 68⁰
    //255    ‡cW 180⁰--E 180⁰/N 90⁰--S 90⁰

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        queryScaleValue(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "255" }
            addSubfieldIfExists(builder, 'a', it)
            record.dataFields.add(builder.build())
        }

        queryNoteOnScale(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply { tag = "255" }
            addSubfieldIfExists(builder, 'a', it)
            record.dataFields.add(builder.build())
        }

        queryCartographicAttributes(conn, workData.workId).forEach {
            val builder: DataFieldBuilder = DataFieldBuilder().apply { tag = "255" }
            addSubfieldIfExists(builder, 'b', it.projection)
            addSubfieldIfExists(builder, 'c', it.coordinates)
            addSubfieldIfExists(builder, 'd', it.ascensionAndDeclination)
            addSubfieldIfExists(builder, 'e', it.equinox)
            addSubfieldIfExists(builder, 'f', it.outerGRing)
            addSubfieldIfExists(builder, 'g', it.exclusionGRing)
            record.dataFields.add(builder.build())
        }
    }

    private fun queryCartographicAttributes(conn: RepositoryConnection, id: Value): List<CartographicMathematicalData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?projection ?coordinates ?ascensionAndDeclination ?equinox ?outerGRing ?exclusionGRing
            WHERE {
                ?id bf:cartographicAttributes ?attributeId .
                OPTIONAL {
                    ?attributeId    bf:projection   ?projectionId .
                    ?projectionId   rdf:type        bf:Projection ;
                                    rdfs:label      ?projection .
                }
                OPTIONAL {
                    ?attributeId    bf:coordinates  ?coordinates .
                }
                OPTIONAL {
                    ?attributeId    bf:ascensionAndDeclination  ?ascensionAndDeclination .
                }
                OPTIONAL {
                    ?attributeId    bf:equinox  ?equinox .
                }
                OPTIONAL {
                    ?attributeId    bf:outerGRing   ?outerGRing .
                }
                OPTIONAL {
                    ?attributeId    bf:exclusionGRing   ?exclusionGRing .
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.filter { !it.isEmpty }
                .map { CartographicMathematicalData(it.getValue("projection"), it.getValue("coordinates"), it.getValue("ascensionAndDeclination"), it.getValue("equinox"), it.getValue("outerGRing"), it.getValue("exclusionGRing")) }.toList()
        }
    }

    private fun queryNoteOnScale(conn: RepositoryConnection, id: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?scale
            WHERE {
                ?id         bf:scale    ?scaleId .
                ?scaleId    rdf:type    bf:Scale ;
                            rdfs:label  ?category ;
                            bf:note     ?noteId .
                ?noteId     rdf:type    bf:Note ;
                            rdfs:label  ?scale .
                FILTER(REGEX(?category, "^linear|^angular"))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { it.getValue("scale") }.toList()
        }
    }

    private fun queryScaleValue(conn: RepositoryConnection, id: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?scale
            WHERE {
                ?id         bf:scale    ?scaleId .
                ?scaleId    rdf:type    bf:Scale ;
                            rdfs:label  ?scale .
                FILTER(REGEX(?scale, "^(?!linear|angular)"))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { it.getValue("scale") }.toList()
        }
    }

    @JvmRecord
    private data class CartographicMathematicalData(val projection: Value?, val coordinates: Value?, val ascensionAndDeclination: Value?, val equinox: Value?, val outerGRing: Value?, val exclusionGRing: Value?)
}