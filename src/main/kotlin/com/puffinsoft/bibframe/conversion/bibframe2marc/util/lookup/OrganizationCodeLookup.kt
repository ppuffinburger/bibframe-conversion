package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import com.puffinsoft.bibframe.conversion.LOC_DATATYPES_ORGS
import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.sail.memory.MemoryStore
import java.io.IOException
import java.util.zip.GZIPInputStream

internal object OrganizationCodeLookup {
    private val MAP: Map<String, String>

    init {
        try {
            val repo = SailRepository(MemoryStore())
            repo.connection.use { conn ->
                GZIPInputStream(javaClass.getResourceAsStream("/organizations.madsrdf.ttl.gz")).use { gis ->
                    conn.add(gis, RDFFormat.TURTLE)
                }

                val queryString = """
                    PREFIX mads: <$LOC_MADS_RDF>
                    PREFIX lcdto: <$LOC_DATATYPES_ORGS>
                    SELECT ?name ?code
                    WHERE {
                        ?name   rdf:type    mads:CorporateName ;
                                mads:code   ?code              .
                        FILTER(DATATYPE(?code) = lcdto:code)
                    }""".trimIndent()

                val query = conn.prepareTupleQuery(queryString)

                query.evaluate().use { result ->
                    MAP = result.associateBy({ it.getValue("name").stringValue() }, { it.getValue("code").stringValue() })
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Unable to process the organizations file")
        }
    }

    fun lookup(code: String): String? {
        return MAP[code]
    }
}