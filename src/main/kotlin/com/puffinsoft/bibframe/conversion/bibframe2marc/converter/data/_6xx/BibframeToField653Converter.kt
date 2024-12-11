package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._6xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField653Converter : BibframeToMarcConverter {
    // TODO : need examples.  Spec has $0, but description says no URI?
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        queryUncontrolledTerm(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "653"
                indicator2 = when(it.typeId.stringValue()) {
                    "http://id.loc.gov/ontologies/bibframe/Topic" -> ' '
                    "http://id.loc.gov/ontologies/bibframe/Person" -> '1'
                    "http://id.loc.gov/ontologies/bibframe/Organization" -> '2'
                    "http://id.loc.gov/ontologies/bibframe/Meeting" -> '3'
                    "http://id.loc.gov/ontologies/bibframe/Temporal" -> '4'
                    "http://id.loc.gov/ontologies/bibframe/Place" -> '5'
                    "http://id.loc.gov/ontologies/bibframe/GenreForm" -> '6'
                    else -> ' '
                }
            }

            addSubfieldIfExists(builder, 'a', it.term)
            addInstitutionSubfieldIfExists(builder, it.institution)

            record.dataFields.add(builder.build())
        }
    }

    private fun queryUncontrolledTerm(conn: RepositoryConnection, id: Value): List<UncontrolledTerm> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            PREFIX mads: <$LOC_MADS_RDF>
            SELECT ?term ?termType ?institution ?institutionId
            WHERE {
                ?id             bf:subject      ?subjectId .
                ?subjectId      rdf:type        bflc:Uncontrolled ;
                                rdf:type        ?termType ;
                                rdfs:label      ?term .
                FILTER(ISBLANK(?subjectId))
                FILTER(?termType IN (bf:Topic, bf:Person, bf:Organization, bf:Meeting, bf:Temporal, bf:Place, bf:GenreForm))
                OPTIONAL {
                    ?subjectId      bflc:applicableInstitution  ?institutionId .
                    ?institutionId  rdf:type                    bf:Agent .
                    OPTIONAL {
                        ?institutionId bf:code ?institution .
                    }
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { UncontrolledTerm(it.getValue("term"), it.getValue("termType"), it.getValue("institution") ?: it.getValue("institutionId")) }.toList()
        }
    }

    @JvmRecord
    private data class UncontrolledTerm(val term: Value, val typeId: Value, val institution: Value?)
}