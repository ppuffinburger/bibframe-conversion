package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import com.puffinsoft.bibframe.conversion.LOC_DATATYPES_ORGS
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.sail.memory.MemoryStore
import java.io.IOException

internal object EncodingLevelLookup {
    private val MAP: Map<String, EncodingLevel>

    init {
        try {
            val repo = SailRepository(MemoryStore())
            repo.connection.use { conn ->
                javaClass.getResourceAsStream("/menclvl.nt").use {
                    conn.add(it, RDFFormat.NTRIPLES)
                }

                val queryString = """
                    PREFIX mads: <$LOC_MADS_RDF>
                    PREFIX lcdto: <$LOC_DATATYPES_ORGS>
                    SELECT ?uri ?label
                    WHERE {
                        <http://id.loc.gov/vocabulary/menclvl>  mads:hasMADSSchemeMember    ?uri .
                        ?uri                                    rdf:type                    mads:Authority ;
                                                                mads:authoritativeLabel     ?label .
                    }""".trimIndent()

                val query = conn.prepareTupleQuery(queryString)

                query.evaluate().use { result ->
                    MAP = result.map { EncodingLevel(it.getValue("uri").stringValue(), it.getValue("label").stringValue()) }.associateBy { TextUtils.getCodeStringFromUrl(it.uri) }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Unable to process the encoding levels file")
        }
    }

    fun lookup(code: String): EncodingLevel? {
        return MAP[code]
    }

    internal data class EncodingLevel(val uri: String, val label: String)
}