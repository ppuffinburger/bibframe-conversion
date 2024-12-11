package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._0xx

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToField055Converter : AbstractBibframeToClassificationConverter() {
    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        convert(conn, workData, record, "055", "ClassificationLcc", ASSIGNER, true)
    }

    companion object {
        private val ASSIGNER: Value = SimpleValueFactory.getInstance().createIRI("http://id.loc.gov/authorities/names/no2004037399")
    }
}