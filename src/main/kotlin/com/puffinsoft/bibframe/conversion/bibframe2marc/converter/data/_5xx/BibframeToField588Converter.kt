package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._5xx

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataFieldBuilder
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField588Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        queryNoteByType(conn, workData.primaryInstanceId, "descsource").forEach { value ->
            val builder = DataFieldBuilder().apply { tag = "588" }
            val note = value.stringValue()

            if (note.startsWith("Description based on:")) {
                builder.indicator1 = '0'
                addSubfieldIfExists(builder, 'a', note.substringAfter("Description based on:").trim { it <= ' ' })
            } else if (note.startsWith("Latest issue consulted:")) {
                builder.indicator1 = '1'
                addSubfieldIfExists(builder, 'a', note.substringAfter("Latest issue consulted:").trim { it <= ' ' })
            } else {
                addSubfieldIfExists(builder, 'a', note.trim { it <= ' ' })
            }

            record.dataFields.add(builder.build())
        }
    }
}