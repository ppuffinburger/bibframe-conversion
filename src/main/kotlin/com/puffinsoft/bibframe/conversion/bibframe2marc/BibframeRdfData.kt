package com.puffinsoft.bibframe.conversion.bibframe2marc

import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import java.io.FileInputStream
import java.io.InputStream

@JvmRecord
data class BibframeRdfData(val context: String, val inputStream: InputStream, val rdfFormat: RDFFormat) {
    constructor(context:String, filename: String, rdfFormat: RDFFormat? = null) : this(context, FileInputStream(filename), rdfFormat ?: Rio.getParserFormatForFileName(filename).orElse(RDFFormat.NTRIPLES))
}