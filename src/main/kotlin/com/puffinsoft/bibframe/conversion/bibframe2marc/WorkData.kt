package com.puffinsoft.bibframe.conversion.bibframe2marc

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import kotlinx.datetime.*
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import kotlin.collections.ArrayList

internal class WorkData(conn: RepositoryConnection, val workId: Value) {
    val primaryInstanceId: Value
    val secondaryInstanceIds: List<Value>
    val itemIds: List<Value>
    val creationDate: LocalDateTime
    val types: Set<Value>
    val workGenreForms: Set<Value>
    val frequencies: Set<Value>
    val supplementaryContent: Set<Value>
    private val alternateGraphicRepresentations = mutableListOf<DataField>()

    private lateinit var allInstanceIds: List<Value>
    private lateinit var allIds: List<Value>
    private lateinit var allIdsAsStrings: List<String>

    init {
        var _primaryInstanceId: Value? = null
        val _secondaryInstanceIds = ArrayList<Value>()
        queryInstanceIds(conn).forEach { data ->
            if (data.isSecondaryInstance) {
                _secondaryInstanceIds.add(data.id)
            } else {
                if (_primaryInstanceId == null) {
                    _primaryInstanceId = data.id
                } else {
                    _secondaryInstanceIds.add(data.id)
                }
            }
        }

        primaryInstanceId = _primaryInstanceId ?: throw RuntimeException("No primary instance found")
        secondaryInstanceIds = _secondaryInstanceIds.toList()

        val allInstanceIds = getAllInstanceIds()
        itemIds = queryItemIds(conn, allInstanceIds)

        val creationDateValue = queryCreationDate(conn)
        creationDate = if (creationDateValue == null) {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        } else {
            LocalDate.parse(creationDateValue.stringValue()).atStartOfDayIn(TimeZone.currentSystemDefault()).toLocalDateTime(TimeZone.currentSystemDefault())
        }

        types = queryTypes(conn)
        workGenreForms = queryGenreForms(conn)
        frequencies = queryFrequencies(conn)
        supplementaryContent = querySupplementaryContent(conn)
    }

    fun getAllInstanceIds(): List<Value> {
        if (!this::allInstanceIds.isInitialized) {
            allInstanceIds = mutableListOf<Value>().apply {
                add(primaryInstanceId)
                addAll(secondaryInstanceIds)
            }
        }
        return allInstanceIds
    }

    fun getAllIdsAsValues(): List<Value> {
        if (!this::allIds.isInitialized) {
            allIds = mutableListOf<Value>().apply {
                add(workId)
                addAll(getAllInstanceIds())
                addAll(itemIds)
            }.toList()
        }
        return allIds
    }

    fun getWorkAndAllInstanceIdsAsStrings(): List<String> {
        if (!this::allIdsAsStrings.isInitialized) {
            allIdsAsStrings = mutableListOf<String>().apply {
                add(workId.stringValue())
                addAll(getAllInstanceIds().map { it.stringValue() })
            }.toList()
        }
        return allIdsAsStrings
    }

    fun getAlternateGraphicRepresentations(): List<DataField> {
        return alternateGraphicRepresentations.toList()
    }

    fun addAlternateGraphicRepresentations(vararg alternateGraphicRepresentations: DataField) {
        this.alternateGraphicRepresentations.addAll(alternateGraphicRepresentations.toList())
    }

    fun getNextAlternateGraphicRepresentationOccurrence(): String {
        return String.format("%02d", alternateGraphicRepresentations.size + 1)
    }

    private fun queryInstanceIds(conn: RepositoryConnection): List<InstanceId> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?instanceId ?secondaryInstance
            WHERE {
                {
                    SELECT DISTINCT ?instanceId
                    WHERE {
                        { 
                            ?workId     bf:hasInstance  ?instanceId .
                            ?instanceId rdf:type        bf:Instance . 
                        } 
                        UNION 
                        { 
                            ?instanceId bf:instanceOf   ?workId . 
                            ?workId     rdf:type        bf:Work . 
                            ?instanceId rdf:type        bf:Instance . 
                        } 
                    }
                }
                OPTIONAL {
                    ?instanceId rdf:type    bflc:SecondaryInstance .
                    BIND("1" AS ?secondaryInstance)
                }
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.map { InstanceId(it.getValue("instanceId"), it.hasBinding("secondaryInstance")) }.toList()
        }
    }


    private fun queryItemIds(conn: RepositoryConnection, instanceIds: List<Value>): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT DISTINCT ?itemId
            WHERE {
                {
                    ?instanceId bf:hasItem  ?itemId .
                    ?itemId     rdf:type    bf:Item .
                }
                UNION
                {
                    ?itemId     bf:itemOf   ?instanceId .
                    ?instanceId rdf:type    bf:Instance .
                }
            }""".trimIndent()

        val itemIds = ArrayList<Value>()

        for (instanceId in instanceIds) {
            val query = conn.prepareTupleQuery(queryString)
            query.setBinding("instanceId", instanceId)

            query.evaluate().use { result ->
                itemIds.addAll(result.map { it.getValue("itemId") }.toList())
            }
        }

        return itemIds.toList()
    }

    private fun queryCreationDate(conn: RepositoryConnection): Value? {
        val idFilter = getWorkAndAllInstanceIdsAsStrings().joinToString { "<$it>" }
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?creationDate ?newStatusDate
            WHERE {
                {
                    ?id                 bf:adminMetadata    ?adminMetadataId .
                    ?adminMetadataId    bf:creationDate     ?creationDate .
                    FILTER(?id IN ($idFilter))
                }
                UNION
                {
                    ?id                                         bf:adminMetadata    ?adminMetadataId .
                    ?adminMetadataId                            bf:status           <http://id.loc.gov/vocabulary/mstatus/n> .
                    <http://id.loc.gov/vocabulary/mstatus/n>    rdf:type            bf:Status .
                    ?adminMetadataId                            bf:date             ?newStatusDate .
                    FILTER(?id IN ($idFilter))
                }
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("creationDate") || it.hasBinding("newStatusDate") }
                .map { if (it.hasBinding("creationDate")) it.getValue("creationDate") else it.getValue("newStatusDate") }
                .firstOrNull()
        }
    }

    private fun queryTypes(conn: RepositoryConnection): Set<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?type
            WHERE {
                ?workId rdf:type ?type .
                FILTER(!STRENDS(STR(?type), "Work"))
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("type") }
                .map { it.getValue("type") }
                .toSet()
        }
    }

    private fun queryGenreForms(conn: RepositoryConnection): Set<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?genreForm
            WHERE {
                ?workId     bf:genreForm    ?genreForm .
                ?genreForm  rdf:type        bf:GenreForm .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("genreForm") }
                .map { it.getValue("genreForm") }
                .toSet()
        }
    }

    private fun queryFrequencies(conn: RepositoryConnection): Set<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?frequency
            WHERE {
                ?instanceId bf:frequency    ?frequency .
                ?frequency  rdf:type        bf:Frequency .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("instanceId", primaryInstanceId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("frequency") }
                .map { it.getValue("frequency") }
                .toSet()
        }
    }

    private fun querySupplementaryContent(conn: RepositoryConnection): Set<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?supplementaryContent
            WHERE {
                ?workId                 bf:supplementaryContent ?supplementaryContent .
                ?supplementaryContent   rdf:type                bf:SupplementaryContent .
            }""".trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("workId", workId)

        query.evaluate().use { result ->
            return result.filter { it.hasBinding("supplementaryContent") }
                .map { it.getValue("supplementaryContent") }
                .toSet()
        }
    }

    @JvmRecord
    private data class InstanceId(val id: Value, val isSecondaryInstance: Boolean)
}