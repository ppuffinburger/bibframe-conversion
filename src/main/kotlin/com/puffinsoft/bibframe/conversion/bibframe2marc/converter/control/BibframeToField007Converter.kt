package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.control

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.*
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.ControlField
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField007Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        workData.getAllInstanceIds().forEach { instanceId ->
            if (isMap(conn, instanceId)) {
                val start = queryMapGenres(conn, instanceId).map { MapGenreFormLookup.lookup(it) }.firstOrNull() ?: "a| "

                record.controlFields.add(ControlField("007", "$start|||||"))

                return@forEach
            }

            if (isGlobe(conn, instanceId)) {
                record.controlFields.add(ControlField("007", "d| |||"))

                return@forEach
            }

            if (isProjectedGraphics(conn, instanceId)) {
                val start = queryProjectedGraphicsCarriers(conn, instanceId).map { ProjectedGraphicCarrierLookup.lookup(it) }.firstOrNull() ?: "g| "

                record.controlFields.add(ControlField("007", "$start||||||"))

                return@forEach
            }

            if (isMicroform(conn, instanceId)) {
                val start = queryMicroformCarriers(conn, instanceId).map { MicroformCarrierLookup.lookup(it) }.firstOrNull() ?: "h| "

                record.controlFields.add(ControlField("007", "$start||||||||||"))

                return@forEach
            }

            if (isNonProjectedGraphic(conn, instanceId)) {
                val start = queryNonProjectedGraphicGenres(conn, instanceId).map { NonProjectedGraphicGenreFormLookup.lookup(it) }.firstOrNull() ?: "k| "

                record.controlFields.add(ControlField("007", "$start|||"))

                return@forEach
            }

            if (isMotionPicture(conn, instanceId)) {
                val start = queryMotionPictureCarriers(conn, instanceId).map { MotionPictureCarrierLookup.lookup(it) }.firstOrNull() ?: "m| "

                record.controlFields.add(ControlField("007", "$start||||||||||||||||||||"))

                return@forEach
            }

            if (isSoundRecording(conn, instanceId)) {
                val start = querySoundRecordingCarriers(conn, instanceId).map { SoundRecordingCarrierLookup.lookup(it) }.firstOrNull() ?: "s| "

                record.controlFields.add(ControlField("007", "$start|||||||||||"))

                return@forEach
            }

            if (isVideoRecording(conn, instanceId)) {
                val start = queryVideoRecordingCarriers(conn, instanceId).map { VideoRecordingCarrierLookup.lookup(it) }.firstOrNull() ?: "v| "

                record.controlFields.add(ControlField("007", "$start||||||"))

                return@forEach
            }

            // TODO : spec is different than code.  Going with spec for now, because the compare util just makes the records look strange
            if (isText(conn, instanceId)) {
                record.controlFields.add(ControlField("007", "t|"))

                return@forEach
            }

            if (isElectronicResource(conn, instanceId)) {
                val start = queryElectronicResourceCarriers(conn, instanceId).map { ElectronicResourceCarrierLookup.lookup(it) }.firstOrNull() ?: "c| "

                record.controlFields.add(ControlField("007", "$start|||||||||||"))
            }
        }
    }

    private fun isMap(conn: RepositoryConnection, instanceId: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                ?instanceId bf:genreForm    ?genreForm .
                ?genreForm  rdf:type        bf:GenreForm . 
                FILTER(?genreForm IN (  <http://id.loc.gov/authorities/genreForms/gf2011026058>, <http://id.loc.gov/authorities/genreForms/gf2014026061>, <http://id.loc.gov/authorities/genreForms/gf2011026387>,
                                        <http://id.loc.gov/authorities/genreForms/gf2011026113>, <http://id.loc.gov/authorities/genreForms/gf2017027245>, <http://id.loc.gov/authorities/genreForms/gf2011026530>,
                                        <http://id.loc.gov/authorities/genreForms/gf2011026295>, <http://id.loc.gov/authorities/genreForms/gf2018026045>))
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("instanceId", instanceId)

        return query.evaluate()
    }

    private fun queryMapGenres(conn: RepositoryConnection, instanceId: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?genreForm
            WHERE {
                ?instanceId bf:genreForm    ?genreForm .
                ?genreForm  rdf:type        bf:GenreForm .
                FILTER(?genreForm IN (  <http://id.loc.gov/authorities/genreForms/gf2011026058>, <http://id.loc.gov/authorities/genreForms/gf2014026061>, <http://id.loc.gov/authorities/genreForms/gf2011026387>,
                                        <http://id.loc.gov/authorities/genreForms/gf2011026113>, <http://id.loc.gov/authorities/genreForms/gf2017027245>, <http://id.loc.gov/authorities/genreForms/gf2011026530>,
                                        <http://id.loc.gov/authorities/genreForms/gf2011026295>, <http://id.loc.gov/authorities/genreForms/gf2018026045>))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("instanceId", instanceId)

        query.evaluate().use { result ->
            return result.map { it.getValue("genreForm") }.toList()
        }
    }

    private fun isGlobe(conn: RepositoryConnection, instanceId: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                {
                    ?instanceId bf:media ?media .
                    ?media      rdf:type bf:Media .
                    FILTER(?media = <http://id.loc.gov/vocabulary/mediaTypes/n>)
                }
                {
                    ?instanceId bf:carrier  ?carrier .
                    ?carrier    rdf:type    bf:Carrier .
                    FILTER(?carrier = <http://id.loc.gov/vocabulary/carriers/nr>)
                }
                {
                    ?instanceId bf:genreForm    ?genreForm .
                    ?genreForm  rdf:type        bf:GenreForm .
                    FILTER(?genreForm IN (<http://id.loc.gov/authorities/genreForms/gf2011026300>, <http://id.loc.gov/authorities/genreForms/gf2011026117>))
                }
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("instanceId", instanceId)

        return query.evaluate()
    }

    private fun isProjectedGraphics(conn: RepositoryConnection, instanceId: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                {
                    ?instanceId bf:media    ?media .
                    ?media      rdf:type    bf:Media .
                    FILTER(?media = <http://id.loc.gov/vocabulary/mediaTypes/g>)
                }
                {
                    ?instanceId bf:carrier  ?carrier .
                    ?carrier    rdf:type    bf:Carrier .
                    FILTER(?carrier IN (<http://id.loc.gov/vocabulary/carriers/gc>, <http://id.loc.gov/vocabulary/carriers/gd>, <http://id.loc.gov/vocabulary/carriers/gf>,
                                        <http://id.loc.gov/vocabulary/carriers/mo>, <http://id.loc.gov/vocabulary/carriers/gs>, <http://id.loc.gov/vocabulary/carriers/gt>))
                }
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("instanceId", instanceId)

        return query.evaluate()
    }

    private fun queryProjectedGraphicsCarriers(conn: RepositoryConnection, instanceId: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?carrier
            WHERE {
                ?instanceId bf:carrier  ?carrier .
                ?carrier    rdf:type    bf:Carrier .
                FILTER(?carrier IN (<http://id.loc.gov/vocabulary/carriers/gc>, <http://id.loc.gov/vocabulary/carriers/gd>, <http://id.loc.gov/vocabulary/carriers/gf>,
                                    <http://id.loc.gov/vocabulary/carriers/mo>, <http://id.loc.gov/vocabulary/carriers/gs>, <http://id.loc.gov/vocabulary/carriers/gt>))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("instanceId", instanceId)

        query.evaluate().use { result ->
            return result.map { it.getValue("carrier") }.toList()
        }
    }

    private fun isMicroform(conn: RepositoryConnection, instanceId: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                ?instanceId bf:media    ?media .
                ?media      rdf:type    bf:Media .
                FILTER(?media = <http://id.loc.gov/vocabulary/mediaTypes/h>)
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("instanceId", instanceId)

        return query.evaluate()
    }

    private fun queryMicroformCarriers(conn: RepositoryConnection, instanceId: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?carrier
            WHERE {
                ?instanceId bf:carrier  ?carrier .
                ?carrier    rdf:type    bf:Carrier .
                FILTER(?carrier IN (<http://id.loc.gov/vocabulary/carriers/ha>, <http://id.loc.gov/vocabulary/carriers/hb>, <http://id.loc.gov/vocabulary/carriers/hc>,
                                    <http://id.loc.gov/vocabulary/carriers/hd>, <http://id.loc.gov/vocabulary/carriers/he>, <http://id.loc.gov/vocabulary/carriers/hf>,
                                    <http://id.loc.gov/vocabulary/carriers/hg>, <http://id.loc.gov/vocabulary/carriers/hh>, <http://id.loc.gov/vocabulary/carriers/hj>))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("instanceId", instanceId)

        query.evaluate().use { result ->
            return result.map { bs: BindingSet -> bs.getValue("carrier") }.toList()
        }
    }

    private fun isNonProjectedGraphic(conn: RepositoryConnection, instanceId: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                {
                    ?instanceId bf:media    ?media .
                    ?media      rdf:type    bf:Media .
                    FILTER(?media = <http://id.loc.gov/vocabulary/mediaTypes/n>)
                }
                {
                    ?instanceId bf:genreForm    ?genreForm .
                    ?genreForm  rdf:type        bf:GenreForm .
                    FILTER(?carrier IN (<http://id.loc.gov/authorities/genreForms/gf2017027227>, <http://id.loc.gov/authorities/genreForms/gf2017027231>, <http://id.loc.gov/authorities/genreForms/gf2017027246>,
                                        <http://id.loc.gov/vocabulary/graphicMaterials/tgm007730>, <http://id.loc.gov/authorities/genreForms/gf2019026026>, <http://id.loc.gov/vocabulary/graphicMaterials/tgm007718>,
                                        <http://id.loc.gov/authorities/genreForms/gf2017027251>, <http://id.loc.gov/authorities/genreForms/gf2017027255>, <http://id.loc.gov/authorities/genreForms/gf2014026152>,
                                        <http://id.loc.gov/vocabulary/graphicMaterials/tgm009250>, <http://id.loc.gov/authorities/genreForms/gf2016026011>, <http://id.loc.gov/authorities/genreForms/gf2014026151>,
                                        <http://id.loc.gov/authorities/genreForms/gf2017027249>))
                }
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("instanceId", instanceId)

        return query.evaluate()
    }

    private fun queryNonProjectedGraphicGenres(conn: RepositoryConnection, instanceId: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?genreForm
            WHERE {
                ?instanceId bf:genreForm    ?genreForm .
                ?genreForm  rdf:type        bf:GenreForm .
                FILTER(?carrier IN (<http://id.loc.gov/authorities/genreForms/gf2017027227>, <http://id.loc.gov/authorities/genreForms/gf2017027231>, <http://id.loc.gov/authorities/genreForms/gf2017027246>,
                                    <http://id.loc.gov/vocabulary/graphicMaterials/tgm007730>, <http://id.loc.gov/authorities/genreForms/gf2019026026>, <http://id.loc.gov/vocabulary/graphicMaterials/tgm007718>,
                                    <http://id.loc.gov/authorities/genreForms/gf2017027251>, <http://id.loc.gov/authorities/genreForms/gf2017027255>, <http://id.loc.gov/authorities/genreForms/gf2014026152>,
                                    <http://id.loc.gov/vocabulary/graphicMaterials/tgm009250>, <http://id.loc.gov/authorities/genreForms/gf2016026011>, <http://id.loc.gov/authorities/genreForms/gf2014026151>,
                                    <http://id.loc.gov/authorities/genreForms/gf2017027249>))
                }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("instanceId", instanceId)

        query.evaluate().use { result ->
            return result.map { it.getValue("genreForm") }.toList()
        }
    }

    private fun isMotionPicture(conn: RepositoryConnection, instanceId: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                {
                    ?instanceId bf:media    ?media .
                    ?media      rdf:type    bf:Media .
                    FILTER(?media = <http://id.loc.gov/vocabulary/mediaTypes/g>)
                }
                {
                    ?instanceId bf:carrier  ?carrier .
                    ?carrier    rdf:type    bf:Carrier .
                    FILTER(?carrier IN (<http://id.loc.gov/vocabulary/carriers/mc>, <http://id.loc.gov/vocabulary/carriers/mf>, <http://id.loc.gov/vocabulary/carriers/mr>, <http://id.loc.gov/vocabulary/carriers/mz>))
                }
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("instanceId", instanceId)

        return query.evaluate()
    }

    private fun queryMotionPictureCarriers(conn: RepositoryConnection, instanceId: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?carrier
            WHERE {
                ?instanceId bf:carrier  ?carrier .
                ?carrier    rdf:type    bf:Carrier .
                FILTER(?carrier IN (<http://id.loc.gov/vocabulary/carriers/mc>, <http://id.loc.gov/vocabulary/carriers/mf>, <http://id.loc.gov/vocabulary/carriers/mr>, <http://id.loc.gov/vocabulary/carriers/mz>))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("instanceId", instanceId)

        query.evaluate().use { result ->
            return result.map { it.getValue("carrier") }.toList()
        }
    }

    private fun isSoundRecording(conn: RepositoryConnection, instanceId: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                ?instanceId bf:media    ?media .
                ?media      rdf:type    bf:Media .
                FILTER(?media = <http://id.loc.gov/vocabulary/mediaTypes/s>)
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("instanceId", instanceId)

        return query.evaluate()
    }

    private fun querySoundRecordingCarriers(conn: RepositoryConnection, instanceId: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?carrier
            WHERE {
                ?instanceId bf:carrier  ?carrier .
                ?carrier    rdf:type    bf:Carrier .
                FILTER(?carrier IN (<http://id.loc.gov/vocabulary/carriers/sd>, <http://id.loc.gov/vocabulary/carriers/se>, <http://id.loc.gov/vocabulary/carriers/sg>,
                                    <http://id.loc.gov/vocabulary/carriers/si>, <http://id.loc.gov/vocabulary/carriers/sq>, <http://id.loc.gov/vocabulary/carriers/cr>,
                                    <http://id.loc.gov/vocabulary/carriers/ss>, <http://id.loc.gov/vocabulary/carriers/st>, <http://id.loc.gov/vocabulary/carriers/sw>))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("instanceId", instanceId)

        query.evaluate().use { result ->
            return result.map { it.getValue("carrier") }.toList()
        }
    }

    private fun isVideoRecording(conn: RepositoryConnection, instanceId: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                ?instanceId bf:media    ?media .
                ?media      rdf:type    bf:Media .
                FILTER(?media = <http://id.loc.gov/vocabulary/mediaTypes/v>)
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("instanceId", instanceId)

        return query.evaluate()
    }

    private fun queryVideoRecordingCarriers(conn: RepositoryConnection, instanceId: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?carrier
            WHERE {
                ?instanceId bf:carrier  ?carrier .
                ?carrier    rdf:type    bf:Carrier .
                FILTER(?carrier IN (<http://id.loc.gov/vocabulary/carriers/vc>, <http://id.loc.gov/vocabulary/carriers/vd>, <http://id.loc.gov/vocabulary/carriers/vf>, <http://id.loc.gov/vocabulary/carriers/vr>))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("instanceId", instanceId)

        query.evaluate().use { result ->
            return result.map { bs: BindingSet -> bs.getValue("carrier") }.toList()
        }
    }

    private fun isText(conn: RepositoryConnection, instanceId: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            ASK WHERE {
                ?instanceId rdf:type    bflc:SecondaryInstance ;
                            bf:media    ?media .
                ?media      rdf:type    bf:Media .
                FILTER(?media = <http://id.loc.gov/vocabulary/mediaTypes/n>)
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("instanceId", instanceId)

        return query.evaluate()
    }

    private fun isElectronicResource(conn: RepositoryConnection, instanceId: Value): Boolean {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            ASK WHERE {
                ?instanceId bf:media    ?media .
                ?media      rdf:type    bf:Media .
                FILTER(?media = <http://id.loc.gov/vocabulary/mediaTypes/c>)
            }""".trimIndent()

        val query = conn.prepareBooleanQuery(queryString)
        query.setBinding("instanceId", instanceId)

        return query.evaluate()
    }

    private fun queryElectronicResourceCarriers(conn: RepositoryConnection, instanceId: Value): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?carrier
            WHERE {
                ?instanceId bf:carrier  ?carrier .
                ?carrier    rdf:type    bf:Carrier .
                FILTER(?carrier IN (<http://id.loc.gov/vocabulary/carriers/ca>, <http://id.loc.gov/vocabulary/carriers/cb>, <http://id.loc.gov/vocabulary/carriers/cd>,
                                    <http://id.loc.gov/vocabulary/carriers/ce>, <http://id.loc.gov/vocabulary/carriers/cf>, <http://id.loc.gov/vocabulary/carriers/ch>,
                                    <http://id.loc.gov/vocabulary/carriers/ck>, <http://id.loc.gov/vocabulary/carriers/cr>, <http://id.loc.gov/vocabulary/carriers/cz>))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("instanceId", instanceId)

        query.evaluate().use { result ->
            return result.map { it.getValue("carrier") }.toList()
        }
    }
}