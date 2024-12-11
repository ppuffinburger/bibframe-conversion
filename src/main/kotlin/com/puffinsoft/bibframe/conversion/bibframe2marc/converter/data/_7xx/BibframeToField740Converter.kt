package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField740Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        query(conn, workData.workId).forEach {
            val builder = DataFieldBuilder().apply {
                tag = "740"
                indicator1 = getNonFilingCharacters(it.nonSortNum)
            }

            addSubfieldIfExists(builder, 'a', it.title)
            addSubfieldIfExists(builder, 'n', it.partNumber)
            addSubfieldIfExists(builder, 'p', it.partName)
            addInstitutionSubfieldIfExists(builder, it.institution)

            record.dataFields.add(builder.build())
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<UncontrolledRelatedTitle> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?title ?nonSortNum ?partNumber ?partName ?institutionId ?institution
            WHERE {
                {
                    ?id             bf:hasPart              ?partId .
                    ?partId         rdf:type                bf:Work ;
                                    rdf:type                bflc:Uncontrolled ;
                                    bf:title                ?titleId .
                    ?titleId        rdf:type                bf:Title ;
                                    bf:mainTitle            ?title .
                    OPTIONAL {
                        ?titleId    bflc:nonSortNum         ?nonSortNum .
                    }
                    OPTIONAL {
                        ?titleId    bf:partNumber           ?partNumber .
                    }
                    OPTIONAL {
                        ?titleId    bf:partName             ?partName .
                    }
                    OPTIONAL {
                        ?partId         bflc:applicableInstitution  ?institutionId .
                        ?institutionId  rdf:type                    bf:Agent ;
                        OPTIONAL {
                            ?institutionId bf:code ?institution .
                        }
                    }
                }
                UNION
                {
                    ?id             bf:relation             ?relationId .
                    ?relationId     rdf:type                bf:Relation ;
                                    bf:relationship         <http://id.loc.gov/vocabulary/relationship/part> ;
                                    bf:associatedResource   ?resourceId .
                    ?resourceId     rdf:type                bf:Work ;
                                    bf:title                ?titleId .
                    ?titleId        rdf:type                bf:Title ;
                                    bf:mainTitle            ?title .
                    OPTIONAL {
                        ?titleId    bflc:nonSortNum         ?nonSortNum .
                    }
                    OPTIONAL {
                        ?titleId    bf:partNumber           ?partNumber .
                    }
                    OPTIONAL {
                        ?titleId    bf:partName             ?partName .
                    }
                    OPTIONAL {
                        ?resourceId     bflc:applicableInstitution  ?institutionId .
                        ?institutionId  rdf:type                    bf:Agent ;
                        OPTIONAL {
                            ?institutionId bf:code ?institution .
                        }
                    }
                }
                UNION
                {
                    ?id             bf:relation             ?relationId .
                    ?relationId     rdf:type                bf:Relation ;
                                    bf:relationship         <http://id.loc.gov/vocabulary/relationship/relatedWork> ;
                                    bf:associatedResource   ?resourceId .
                    ?resourceId     rdf:type                bf:Work ;
                                    rdf:type                bflc:Uncontrolled ;
                                    bf:title                ?titleId .
                    ?titleId        rdf:type                bf:Title ;
                                    bf:mainTitle            ?title .
                    OPTIONAL {
                        ?titleId    bflc:nonSortNum         ?nonSortNum .
                    }
                    OPTIONAL {
                        ?titleId    bf:partNumber           ?partNumber .
                    }
                    OPTIONAL {
                        ?titleId    bf:partName             ?partName .
                    }
                    OPTIONAL {
                        ?resourceId     bflc:applicableInstitution  ?institutionId .
                        ?institutionId  rdf:type                    bf:Agent ;
                        OPTIONAL {
                            ?institutionId bf:code ?institution .
                        }
                    }
                }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { UncontrolledRelatedTitle(it.getValue("title"), it.getValue("nonSortNum"), it.getValue("partNumber"), it.getValue("partName"), if (it.hasBinding("institution")) it.getValue("institution") else it.getValue("institutionId")) }
        }
    }

    @JvmRecord
    private data class UncontrolledRelatedTitle(val title: Value, val nonSortNum: Value?, val partNumber: Value?, val partName: Value?, val institution: Value?)
}