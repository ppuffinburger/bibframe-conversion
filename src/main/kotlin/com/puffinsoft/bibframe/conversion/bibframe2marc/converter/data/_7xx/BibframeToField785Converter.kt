package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._7xx

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField785Converter : AbstractBibframeToLinkingEntryConverter() {
    // TODO : no examples
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        convert(conn, workData, record, "785", '0', "http://id.loc.gov/vocabulary/relationship/continuedby")
        convert(conn, workData, record, "785", '1', "http://id.loc.gov/vocabulary/relationship/continuedinpartby")
        convert(conn, workData, record, "785", '2', "http://id.loc.gov/vocabulary/relationship/succeededby")
        convert(conn, workData, record, "785", '4', "http://id.loc.gov/vocabulary/relationship/absorbedby")
        convert(conn, workData, record, "785", '5', "http://id.loc.gov/vocabulary/relationship/absorbedinpartby")
        convert(conn, workData, record, "785", '6', "http://id.loc.gov/vocabulary/relationship/splitinto")
        convert(conn, workData, record, "785", '7', "http://id.loc.gov/vocabulary/relationship/mergedToForm")
    }
}