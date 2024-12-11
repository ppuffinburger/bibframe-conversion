package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.*
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField340Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        this.workData.forEach { wd: TypeData ->
            query(conn, workData.workId, wd.typePredicate, wd.typeObject, wd.graphName).forEach { record.dataFields.add(buildDataField(it, wd.valueSubfieldName, wd.labelSubfieldName)) }
        }

        instanceData.forEach { id: TypeData ->
            query(conn, workData.primaryInstanceId, id.typePredicate, id.typeObject, id.graphName).forEach { record.dataFields.add(buildDataField(it, id.valueSubfieldName, id.labelSubfieldName)) }
        }
    }

    private fun buildDataField(data: PhysicalMediumData, valueSubfieldName: Char?, labelSubfieldName: Char): DataField {
        val builder = DataFieldBuilder().apply { tag = "340" }

        addSubfieldIfExists(builder, '3', data.appliesTo)
        if (valueSubfieldName != null) {
            addSubfieldIfExists(builder, valueSubfieldName, data.typeValue)
        }

        if (data.uri.isIRI) {
            addSubfieldIfExists(builder, labelSubfieldName, data.typeLabel?.stringValue() ?: TextUtils.getCodeStringFromUrl(data.uri))
            addSubfieldIfExists(builder, '0', data.uri)
        } else {
            addSubfieldIfExists(builder, labelSubfieldName, data.typeLabel)
            addSourceSubfieldIfExists(builder, data.source)
        }

        return builder.build()
    }

    private fun query(conn: RepositoryConnection, id: Value, typePredicate: String, typeObject: String, graphName: String): List<PhysicalMediumData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?typeId ?typeLabel ?typeValue ?source ?sourceId ?appliesTo
            WHERE {
                {
                    SELECT ?typeId ?typeLabel
                    WHERE {
                        ?id             bf:$typePredicate   ?typeId .
                        ?typeId         rdf:type            bf:$typeObject ;
                                        rdfs:label          ?typeLabel .
                        FILTER(ISBLANK(?typeId) || (ISIRI(?typeId) && !CONTAINS(STR(?typeId), "/id.loc.gov/vocabulary/")))
                    }
                }
                UNION
                {
                    SELECT DISTINCT ?typeId (COALESCE(?authLabel, ?pmLabel) AS ?typeLabel)
                    WHERE {
                        ?id             bf:$typePredicate   ?typeId .
                        ?typeId         rdf:type            bf:$typeObject .
                        FILTER(ISIRI(?typeId) && CONTAINS(STR(?typeId), "/id.loc.gov/vocabulary/"))
                        OPTIONAL {
                            GRAPH <$graphName> {
                                ?typeId  mads:authoritativeLabel ?authLabel .
                            }
                        }
                        OPTIONAL {
                            ?typeId     rdfs:label          ?pmLabel .
                        }
                    }
                }
                OPTIONAL {
                    ?typeId rdf:value   ?typeValue .
                }
                OPTIONAL {
                    ?typeId         bf:source   ?sourceId .
                    ?sourceId       rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?typeId         bflc:appliesTo  ?appliesToId .
                    ?appliesToId    rdf:type        bflc:AppliesTo ;
                                    rdfs:label      ?appliesTo .
                }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { PhysicalMediumData(it.getValue("typeId"), it.getValue("typeLabel"), it.getValue("typeValue"), it.getValue("source") ?: it.getValue("sourceId"), it.getValue("appliesTo")) }.toList()
        }
    }

    private val workData = listOf(
        TypeData("colorContent", "ColorContent", COLOR_CONTENT_URI, 'g', null),
        TypeData("illustrativeContent", "Illustration", ILLUSTRATIVE_CONTENT_URI, 'p', null)
    )

    private val instanceData = listOf(
        TypeData("appliedMaterial", "AppliedMaterial", SUPPORT_MATERIAL_URI, 'c', null),
        TypeData("baseMaterial", "BaseMaterial", SUPPORT_MATERIAL_URI, 'a', null),
        TypeData("binding", "Binding", "http://id.loc.gov/vocabulary/nil", 'l', null),
        TypeData("bookFormat", "BookFormat", BOOK_FORMAT_URI, 'm', null),
        TypeData("colorContent", "ColorContent", COLOR_CONTENT_URI, 'g', null),
        TypeData("emulsion", "Emulsion", SUPPORT_MATERIAL_URI, 'c', null),
        TypeData("fontSize", "FontSize", FONT_SIZE_URI, 'n', null),
        TypeData("generation", "Generation", GENERATION_URI, 'j', null),
        TypeData("illustrativeContent", "Illustration", ILLUSTRATIVE_CONTENT_URI, 'p', null),
        TypeData("layout", "Layout", LAYOUT_URI, 'k', null),
        TypeData("mount", "Mount", SUPPORT_MATERIAL_URI, 'e', null),
        TypeData("polarity", "Polarity", POLARITY_URI, 'o', null),
        TypeData("productionMethod", "ProductionMethod", PRODUCTION_METHOD_URI, 'd', null),
        TypeData("reductionRatio", "ReductionRatio", REDUCTION_RATIO_URI, 'q', 'f')
    )

    @JvmRecord
    private data class TypeData(val typePredicate: String, val typeObject: String, val graphName: String, val labelSubfieldName: Char, val valueSubfieldName: Char?)

    @JvmRecord
    private data class PhysicalMediumData(val uri: Value, val typeLabel: Value?, val typeValue: Value?, val source: Value?, val appliesTo: Value?)
}