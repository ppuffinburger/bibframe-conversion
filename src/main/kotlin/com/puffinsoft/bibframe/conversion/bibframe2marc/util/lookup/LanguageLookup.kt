package com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup

import com.puffinsoft.bibframe.conversion.LOC_MADS_RDF
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.sail.memory.MemoryStore
import java.io.IOException

internal object LanguageLookup {
    private val MAP: Map<String, Language>

    init {
        try {
            val repo = SailRepository(MemoryStore())
            repo.connection.use { conn ->
                javaClass.getResourceAsStream("/languages.nt").use {
                    conn.add(it, RDFFormat.NTRIPLES)
                }

                val queryString = """
                    PREFIX mads: <$LOC_MADS_RDF>
                    SELECT ?uri ?label
                    WHERE {
                        <http://id.loc.gov/vocabulary/languages>    mads:hasTopMemberOfMADSScheme   ?uri .
                        ?uri                                        rdf:type                        mads:Authority ;
                                                                    mads:authoritativeLabel         ?label .
                        FILTER(LANG(?label) = "en")
                    }""".trimIndent()

                val query = conn.prepareTupleQuery(queryString)

                query.evaluate().use { result ->
                    MAP = result.map { Language(it.getValue("uri").stringValue(), it.getValue("label").stringValue()) }.associateBy { TextUtils.getCodeStringFromUrl(it.uri) }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Unable to process the encoding levels file")
        }
    }

    fun lookup(code: String): Language? {
        return MAP[code]
    }

    internal data class Language(val uri: String, val label: String)
}