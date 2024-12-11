package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.DataField
import org.marc4k.marc.Subfield
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField758Converter : BibframeToMarcConverter {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        record.dataFields.add(DataField("758", subfields = mutableListOf(Subfield('1', workData.primaryInstanceId.stringValue()))))
        record.dataFields.add(DataField("758", subfields = mutableListOf(Subfield('4', "http://id.loc.gov/ontologies/bibframe/instanceOf"), Subfield('1', workData.workId.stringValue()))))
    }
}