package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx

import com.puffinsoft.bibframe.conversion.BIBFRAME_LC_EXTENSION_ONTOLOGY
import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField753Converter : BibframeToMarcConverter {
    // these don't link in any way to put things into a single field.  Attempting to put them together.
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        workData.getAllInstanceIds().forEach { instanceId ->
            SystemRequirements().apply { addAll(query(conn, instanceId)) }.systemRequirements.forEach {
                val builder = DataFieldBuilder().apply { tag = "753" }

                addSubfieldIfExists(builder, 'a', it.machineModel)
                addSubfieldIfExists(builder, 'b', it.programmingLanguage)
                addSubfieldIfExists(builder, 'c', it.operatingSystem)

                record.dataFields.add(builder.build())
            }
        }
    }

    private fun query(conn: RepositoryConnection, id: Value): List<SystemRequirementTypeAndText> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            PREFIX bflc: <$BIBFRAME_LC_EXTENSION_ONTOLOGY>
            SELECT ?typeId ?text
            WHERE {
                ?id             bf:systemRequirement    ?reqId .
                ?reqId          rdf:type                ?typeId ;
                                rdfs:label              ?text .
                FILTER(?typeId IN (bflc:MachineModel, bflc:ProgrammingLanguage, bflc:OperatingSystem))
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { SystemRequirementTypeAndText(it.getValue("typeId"), it.getValue("text")) }.toList()
        }
    }

    @JvmRecord
    private data class SystemRequirementTypeAndText(val typeId: Value, val text: Value)

    private class SystemRequirements {
        private val _systemRequirements = mutableListOf<SystemRequirement>()
        private val workList = mutableMapOf<Value, Value>()

        val systemRequirements: List<SystemRequirement>
            get() = _systemRequirements.toList()

        fun addAll(systemRequirementTypeAndTexts: List<SystemRequirementTypeAndText>) {
            systemRequirementTypeAndTexts.forEach {
                val typeId = it.typeId
                when (typeId) {
                    MACHINE_MODEL_VALUE -> {
                        if (workList.isNotEmpty()) {
                            generateSystemRequirement()
                        }
                    }
                    PROGRAMMING_LANGUAGE_VALUE -> {
                        if (workList.containsKey(PROGRAMMING_LANGUAGE_VALUE) || workList.containsKey(OPERATING_SYSTEM_VALUE)) {
                            generateSystemRequirement()
                        }
                    }
                    OPERATING_SYSTEM_VALUE -> {
                        if (workList.containsKey(OPERATING_SYSTEM_VALUE)) {
                            generateSystemRequirement()
                        }
                    }
                }
                workList[typeId] = it.text
            }

            if (workList.isNotEmpty()) {
                generateSystemRequirement()
            }
        }

        private fun generateSystemRequirement() {
            _systemRequirements.add(SystemRequirement(workList[MACHINE_MODEL_VALUE], workList[PROGRAMMING_LANGUAGE_VALUE], workList[OPERATING_SYSTEM_VALUE]))
            workList.clear()
        }

        companion object {
            private val MACHINE_MODEL_VALUE = SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bflc/MachineModel")
            private val PROGRAMMING_LANGUAGE_VALUE = SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bflc/ProgrammingLanguage")
            private val OPERATING_SYSTEM_VALUE = SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/ontologies/bflc/OperatingSystem")
        }
    }

    @JvmRecord
    private data class SystemRequirement(val machineModel: Value?, val programmingLanguage: Value?, val operatingSystem: Value?)
}