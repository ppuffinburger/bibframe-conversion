package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._3xx.*
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToPhysicalDescriptionEtcFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        BibframeToField300Converter(),
        BibframeToField306Converter(),
        // 307 Not supported in Bibframe
        BibframeToField310Converter(),
        BibframeToField321Converter(),
        BibframeToField334Converter(),
        // 335 Not supported in Bibframe
        BibframeToField336Converter(),
        BibframeToField337Converter(),
        BibframeToField338Converter(),
        BibframeToField340Converter(),
        BibframeToField341Converter(),
        // 342 Not supported in Bibframe
        // 343 Not supported in Bibframe
        BibframeToField344Converter(),
        BibframeToField345Converter(),
        BibframeToField346Converter(),
        BibframeToField347Converter(),
        BibframeToField348Converter(),
        BibframeToField351Converter(),
        BibframeToField352Converter(),
        BibframeToField353Converter(),
        // 355 Not supported in Bibframe
        // 357 Not supported in Bibframe
        // 361 Not supported in Bibframe
        BibframeToField362Converter(),
        // 363 Not supported in Bibframe
        // 365 Not supported in Bibframe
        // 366 Not supported in Bibframe
        BibframeToField370Converter(),
        // 377 Not supported in Bibframe
        // 380 Not supported in Bibframe
        // 381 Not supported in Bibframe
        BibframeToField382Converter(),
        BibframeToField383Converter(),
        BibframeToField384Converter(),
        BibframeToField385Converter(),
        BibframeToField386Converter()
        // 387 Not supported in Bibframe
        // 388 Not supported in Bibframe
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}