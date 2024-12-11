package com.puffinsoft.bibframe.conversion.bibframe2marc

import com.puffinsoft.bibframe.conversion.*
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.ntriples.NTriplesParserSettings
import org.eclipse.rdf4j.sail.nativerdf.NativeStore

class BibframeRepository : AutoCloseable {
    private val repo: Repository = SailRepository(NativeStore())
    val conn: RepositoryConnection = repo.connection.apply {
        parserConfig.set(NTriplesParserSettings.FAIL_ON_INVALID_LINES, false)
    }

    init {
        listOf(
            Pair("bookformat.nt", BOOK_FORMAT_URI),
            Pair("maudience.nt", INTENDED_AUDIENCE_URI),
            Pair("mbroadstd.nt", BROADCAST_STANDARD_URI),
            Pair("mcapturestorage.nt", CAPTURE_STORAGE_URI),
            Pair("mcolor.nt", COLOR_CONTENT_URI),
            Pair("mencformat.nt", ENCODING_FORMAT_URI),
            Pair("mfiletype.nt", FILE_TYPE_URI),
            Pair("mfont.nt", FONT_SIZE_URI),
            Pair("mgeneration.nt", GENERATION_URI),
            Pair("mgroove.nt", GROOVE_URI),
            Pair("millus.nt", ILLUSTRATIVE_CONTENT_URI),
            Pair("mlayout.nt", LAYOUT_URI),
            Pair("mmaterial.nt", SUPPORT_MATERIAL_URI),
            Pair("mmusicformat.nt", MUSIC_FORMAT_URI),
            Pair("mmusnotation.nt", MUSIC_NOTATION_URI),
            Pair("mnotetype.nt", NOTE_TYPE_URI),
            Pair("mplayback.nt", PLAYBACK_URI),
            Pair("mplayspeed.nt", PLAY_SPEED_URI),
            Pair("mpolarity.nt", POLARITY_URI),
            Pair("mpresformat.nt", PRESENTATION_FORMAT_URI),
            Pair("mproduction.nt", PRODUCTION_METHOD_URI),
            Pair("mrecmedium.nt", RECORDING_MEDIUM_URI),
            Pair("mrectype.nt", RECORDING_TYPE_URI),
            Pair("mreductionratio.nt", REDUCTION_RATIO_URI),
            Pair("mregencoding.nt", REGION_ENCODING_URI),
            Pair("mspecplayback.nt", SPECIAL_PLAYBACK_URI),
            Pair("mtapeconfig.nt", TAPE_CONFIG_URI),
            Pair("mvidformat.nt", VIDEO_FORMAT_URI),
            Pair("relators.nt", RELATORS_URI)
        ).forEach { pair ->
            javaClass.getResourceAsStream("/${pair.first}").use {
                conn.add(it, RDFFormat.NTRIPLES, SimpleValueFactory.getInstance().createIRI(pair.second))
            }
        }
    }

    override fun close() {
        conn.close()
        repo.shutDown()
    }
}