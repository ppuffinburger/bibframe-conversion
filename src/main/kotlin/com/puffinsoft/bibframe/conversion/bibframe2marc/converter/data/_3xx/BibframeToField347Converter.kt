package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx

import com.puffinsoft.bibframe.conversion.*
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField347Converter : BibframeToMarcConverter {
    // TODO : have example, but some have the only main DigitalCharacteristic type and others only have the sub-types.  Just query sub-types for now like the code.  Code also writes $0's, while spec doesn't mention
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        val digitalCharacteristicMap = DIGITAL_CHARACTERISTICS.flatMap { query(conn, workData.primaryInstanceId, it.first, it.second) }.groupBy { it.source?.stringValue() ?: "" }

        digitalCharacteristicMap.forEach { (type, dcs) ->
            val builder = DataFieldBuilder().apply { tag = "347" }
            var appliesTo: Value? = null
            val subfields = dcs.map { data ->
                if (data.appliesTo != null) {
                    appliesTo = data.appliesTo
                }
                when (data.digitalCharacteristicType.stringValue()) {
                    "http://id.loc.gov/ontologies/bibframe/FileType" -> Subfield('a', data.characteristicLabel.stringValue())
                    "http://id.loc.gov/ontologies/bibframe/EncodingFormat" -> Subfield('b', data.characteristicLabel.stringValue())
                    "http://id.loc.gov/ontologies/bibframe/FileSize" -> Subfield('c', data.characteristicLabel.stringValue())
                    "http://id.loc.gov/ontologies/bibframe/Resolution" -> Subfield('d', data.characteristicLabel.stringValue())
                    "http://id.loc.gov/ontologies/bibframe/RegionalEncoding" -> Subfield('e', data.characteristicLabel.stringValue())
                    "http://id.loc.gov/ontologies/bibframe/EncodedBitrate" -> Subfield('f', data.characteristicLabel.stringValue())
                    else -> throw RuntimeException("Unknown digital characteristic type: $data.digitalCharacteristicType")
                }
            }.toList()

            addSubfieldIfExists(builder, '3', appliesTo)

            subfields.forEach { addSubfieldIfExists(builder, it.name, it.data) }

            addSubfieldIfExists(builder, '2', type)
            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value, digitalCharacteristicType: Value, graphName: Value): List<DigitalCharacteristicData> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?characteristicLabel ?digitalCharacteristicType ?source ?sourceId ?appliesTo
            WHERE {
                {
                    SELECT ?characteristicLabel ?digitalCharacteristicType ?digitalCharacteristicId
                    WHERE {
                        ?id                         bf:digitalCharacteristic    ?digitalCharacteristicId .
                        ?digitalCharacteristicId    rdf:type                    ?digitalCharacteristicType ;
                                                    rdfs:label                  ?characteristicLabel .
                        FILTER(ISBLANK(?digitalCharacteristicId) || (ISIRI(?digitalCharacteristicId) && !CONTAINS(STR(?digitalCharacteristicId), "/id.loc.gov/vocabulary/")))
                    }
                }
                UNION
                {
                    SELECT DISTINCT (COALESCE(?authLabel, ?charLabel) AS ?characteristicLabel) ?digitalCharacteristicType ?digitalCharacteristicId
                    WHERE {
                        ?id                         bf:digitalCharacteristic    ?digitalCharacteristicId .
                        ?digitalCharacteristicId    rdf:type                    ?digitalCharacteristicType .
                        FILTER(ISIRI(?digitalCharacteristicId) && CONTAINS(STR(?digitalCharacteristicId), "/id.loc.gov/vocabulary/"))
                        OPTIONAL {
                            GRAPH ?graphName {
                                ?digitalCharacteristicId    mads:authoritativeLabel ?authLabel .
                            }
                        }
                        OPTIONAL {
                            ?digitalCharacteristicId    rdfs:label  ?charLabel .
                        }
                    }
                }
                OPTIONAL {
                    ?digitalCharacteristicId    bf:source   ?sourceId .
                    ?sourceId                   rdf:type    bf:Source .
                    OPTIONAL {
                        ?sourceId   bf:code ?source .
                    }
                }
                OPTIONAL {
                    ?digitalCharacteristicId    bflc:appliesTo  ?appliesToId .
                    ?appliesToId                rdf:type        bflc:AppliesTo ;
                                                rdfs:label      ?appliesTo .
                }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)
        query.setBinding("digitalCharacteristicType", digitalCharacteristicType)
        query.setBinding("graphName", graphName)

        query.evaluate().use { result ->
            return result.map {
                DigitalCharacteristicData(
                    it.getValue("characteristicLabel"),
                    it.getValue("digitalCharacteristicType"),
                    it.getValue("source") ?: it.getValue("sourceId"),
                    it.getValue("appliesTo")
                )
            }.toList()
        }
    }

    @JvmRecord
    private data class DigitalCharacteristicData(val characteristicLabel: Value, val digitalCharacteristicType: Value, val source: Value?, val appliesTo: Value?)

    companion object {
        private val DIGITAL_CHARACTERISTICS = listOf(
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/FileType"), SimpleValueFactory.getInstance().createIRI(FILE_TYPE_URI)),
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/EncodingFormat"), SimpleValueFactory.getInstance().createIRI(ENCODING_FORMAT_URI)),
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/FileSize"), SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/vocabulary/nil")),
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/Resolution"), SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/vocabulary/nil")),
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/RegionalEncoding"), SimpleValueFactory.getInstance().createIRI(REGION_ENCODING_URI)),
            Pair(SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bibframe/EncodedBitrate"), SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/vocabulary/nil"))
        )
    }
}