package com.puffinsoft.bibframe.conversion.bibframe2marc

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToControlFieldConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToDataFieldConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToLeaderConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.query.TupleQuery
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord
import java.util.function.Consumer

class Marc21RecordIterator(private val repo: BibframeRepository, bibframeRdfData: List<BibframeRdfData>) : Iterator<BibliographicRecord> {
    private var currentWorkIdIndex = 0
    private val workIds: List<Value>

    init {
        bibframeRdfData.forEach { repo.conn.add(it.inputStream, "http://bf2m", it.rdfFormat) }
        workIds = queryWorkIds(repo.conn)
    }

    override fun hasNext(): Boolean {
        return workIds.size > currentWorkIdIndex
    }

    override fun next(): BibliographicRecord {
        val workData = WorkData(repo.conn, workIds[currentWorkIdIndex++])

        return BibliographicRecord().apply {
            BibframeToLeaderConverter().convert(repo.conn, workData, this)
            BibframeToControlFieldConverter().convert(repo.conn, workData, this)
            BibframeToDataFieldConverter().convert(repo.conn, workData, this)
        }
    }

    override fun forEachRemaining(action: Consumer<in BibliographicRecord>) {
        while (hasNext()) action.accept(next())
    }

    private fun queryWorkIds(conn: RepositoryConnection): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT DISTINCT ?workId
            WHERE {
                {
                    ?workId     rdf:type        bf:Work .
                }
                UNION  
                {
                    ?instanceId bf:instanceOf   ?workId .
                }
                FILTER(CONTAINS(STR(?workId), "id.loc.gov/resources/works/")) 
                FILTER NOT EXISTS { [] bf:associatedResource|bf:partOf ?workId }
            }
        """.trimIndent()

        val query: TupleQuery = conn.prepareTupleQuery(queryString)

        query.evaluate().use { result ->
            return result.filter { bs -> bs.hasBinding("workId") }
                .map { bs -> bs.getValue("workId") }
                .toList()
        }
    }
}