package com.puffinsoft.bibframe.conversion.bibframe2marc.converter

import com.puffinsoft.bibframe.conversion.BIBFRAME_ONTOLOGY
import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.TextUtils
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.*
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.FontSizeLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.NoteTypeLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.OrganizationCodeLookup
import com.puffinsoft.bibframe.conversion.bibframe2marc.util.lookup.StatusCodeLookup
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.query.Binding
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal interface BibframeToMarcConverter {
    fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord)

    fun getNonFilingCharacters(value: Value?): Char {
        return if (value == null) {
            '0'
        } else {
            value.stringValue()[0]
        }
    }

    fun addSubfieldIfExists(builder: DataFieldBuilder, subfieldName: Char, value: String?) {
        if (!value.isNullOrBlank()) {
            builder.subfields {
                subfield {
                    name = subfieldName
                    data = value
                }
            }
        }
    }

    fun addSubfieldIfExists(builder: DataFieldBuilder, subfieldName: Char, value: Value?) {
        value?.let { addSubfieldIfExists(builder, subfieldName, it.stringValue()) }
    }

    fun addSourceSubfieldIfExists(builder: DataFieldBuilder, value: Value?) {
        value?.let {
            builder.subfields {
                subfield {
                    name = '2'
                    data = if (it.isLiteral) it.stringValue() else OrganizationCodeLookup.lookup(it.stringValue()) ?: TextUtils.getCodeStringFromUrl(it.stringValue())
                }
            }
        }
    }

    fun addInstitutionSubfieldIfExists(builder: DataFieldBuilder, value: Value?) {
        value?.let {
            builder.subfields {
                subfield {
                    name = '5'
                    data = if (it.isLiteral) it.stringValue() else OrganizationCodeLookup.lookup(it.stringValue()) ?: TextUtils.getCodeStringFromUrl(it.stringValue())
                }
            }
        }
    }

    fun insertSubfieldIfExists(dataField: DataField, index: Int, subfieldName: Char, value: Value?) {
        value?.let {
            dataField.subfields.add(index, Subfield(subfieldName, it.stringValue()))
        }
    }

    fun lookupNoteTypeUri(code: String): String {
        return NoteTypeLookup.lookup(code)?.uri ?: throw RuntimeException("Could not find '$code' in note types")
    }

    fun lookupStatusCodeUri(code: String): String {
        return StatusCodeLookup.lookup(code)?.uri ?: throw RuntimeException("Could not find '$code' in status codes")
    }

    fun lookupFontSizeUri(code: String): String {
        return FontSizeLookup.lookup(code)?.uri ?: throw RuntimeException("Could not find '$code' in fonts")
    }

    fun lookupTactileNotationUri(code: String): String {
        return TactileNotationLookup.lookup(code)?.uri ?: throw RuntimeException("Could not find '$code' in tactile notations")
    }

    fun lookupPresentationFormatUri(code: String): String {
        return PresentationFormatLookup.lookup(code)?.uri ?: throw RuntimeException("Could not find '$code' in presentation formats")
    }

    fun lookupEncodingLevelUri(code: String): String {
        return EncodingLevelLookup.lookup(code)?.uri ?: throw RuntimeException("Could not find '$code' in encoding levels")
    }

    fun lookupDescriptionConventionUri(code: String): String {
        return DescriptionConventionLookup.lookup(code)?.uri ?: throw RuntimeException("Could not find '$code' in description conventions")
    }

    fun lookupClassificationSchemeUri(code: String): String {
        return ClassificationSchemeLookup.lookup(code)?.uri ?: throw RuntimeException("Could not find '$code' in classification schemes")
    }

    fun lookupCarrierUri(code: String): String {
        return CarriersLookup.lookup(code)?.uri ?: throw RuntimeException("Could not find '$code' in carriers")
    }

    fun lookupAuthenticationActionUri(code: String): String {
        return AuthenticationActionLookup.lookup(code)?.uri ?: throw RuntimeException("Could not find '$code' in authentication actions")
    }

    fun getSourceIndicatorFromString(data: String): Char {
        return when (data) {
            "http://id.loc.gov/authorities/subjects" -> '0'
            "http://id.loc.gov/authorities/childrensSubjects" -> '1'
            "http://id.loc.gov/vocabulary/subjectSchemes/mesh" -> '2'
            "http://id.loc.gov/vocabulary/subjectSchemes/nal" -> '3'
            "http://id.loc.gov/vocabulary/subjectSchemes/cash" -> '5'
            "http://id.loc.gov/vocabulary/subjectSchemes/rvm" -> '6'
            else -> '7'
        }
    }

    fun queryRoles(conn: RepositoryConnection, queryString: String, bindings: List<Binding>): List<Role> {
        val rolesQuery = conn.prepareTupleQuery(queryString).apply {
            bindings.forEach {
                setBinding(it.name, it.value)
            }
        }

        return rolesQuery.evaluate().use { result ->
            result.map { Role(it.getValue("roleId"), it.getValue("role")) }.toList()
        }
    }

    fun addRoleSubfields(builder: DataFieldBuilder, roles: List<Role>) {
        roles.forEach { role ->
            if (role.uri.isIRI) {
                addSubfieldIfExists(builder, 'e', role.label?.stringValue() ?: TextUtils.getCodeStringFromUrl(role.uri))
                addSubfieldIfExists(builder, '4', role.uri)
            } else {
                addSubfieldIfExists(builder, 'e', role.label)
            }
        }
    }

    fun queryNoteByType(conn: RepositoryConnection, id: Value, noteType: String): List<Value> {
        val queryString = """
            PREFIX bf: <$BIBFRAME_ONTOLOGY>
            SELECT ?note
            WHERE {
                ?id         bf:note     ?noteId .
                ?noteId     rdf:type    bf:Note ;
                            rdf:type    <http://id.loc.gov/vocabulary/mnotetype/${noteType}> ;
                            rdfs:label  ?note .
            }
        """.trimIndent()

        val query = conn.prepareTupleQuery(queryString)
        query.setBinding("id", id)

        query.evaluate().use { result ->
            return result.map { it.getValue("note") }.toList()
        }
    }
}

@JvmRecord
internal data class Role(val uri: Value, val label: Value?)