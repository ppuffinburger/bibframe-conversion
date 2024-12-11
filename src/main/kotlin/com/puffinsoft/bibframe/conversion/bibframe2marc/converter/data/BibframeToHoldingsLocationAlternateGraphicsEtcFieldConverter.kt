package com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data

import com.puffinsoft.bibframe.conversion.bibframe2marc.WorkData
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.BibframeToMarcConverter
import com.puffinsoft.bibframe.conversion.bibframe2marc.converter.data._8xx.*
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.marc4k.marc.marc21.bibliographic.BibliographicRecord

internal class BibframeToHoldingsLocationAlternateGraphicsEtcFieldConverter : BibframeToMarcConverter {
    private val converters = listOf(
        // 841 Not supported in Bibframe
        // 842 Not supported in Bibframe
        // 843 Not supported in Bibframe
        // 844 Not supported in Bibframe
        // 845 Not supported in Bibframe
        // 850 Not supported in Bibframe
        // 852 Not supported in Bibframe
        // 853 Not supported in Bibframe
        // 854 Not supported in Bibframe
        // 855 Not supported in Bibframe
        BibframeToField856Converter(),
        // 857 Not supported in Bibframe
        // 863 Not supported in Bibframe
        // 864 Not supported in Bibframe
        // 865 Not supported in Bibframe
        // 866 Not supported in Bibframe
        // 867 Not supported in Bibframe
        // 868 Not supported in Bibframe
        // 876 Not supported in Bibframe
        // 877 Not supported in Bibframe
        // 878 Not supported in Bibframe
        BibframeToField880Comverter()
        // 881 Not supported in Bibframe
        // 882 Not supported in Bibframe
        // 883 Not supported in Bibframe
        // 884 Not supported in Bibframe
        // 885 Not supported in Bibframe
        // 886 Not supported in Bibframe
        // 887 Not supported in Bibframe
    )

    override fun convert(conn: RepositoryConnection, workData: WorkData, record: BibliographicRecord) {
        converters.forEach { it.convert(conn, workData, record) }
    }
}