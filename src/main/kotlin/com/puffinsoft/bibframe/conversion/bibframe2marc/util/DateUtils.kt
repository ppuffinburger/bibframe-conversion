package com.puffinsoft.bibframe.conversion.bibframe2marc.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.*

internal object DateUtils {
    private val FORMATTER_005 = LocalDateTime.Format {
        year()
        monthNumber()
        dayOfMonth()
        hour()
        minute()
        second()
        char('.')
        secondFraction(1)
    }

    private val FORMATTER_DATE_ENTERED_ON_FILE = LocalDate.Format {
        year()
        monthNumber()
        dayOfMonth()
    }

    private val FORMATTER_DATE_YEAR = LocalDate.Format {
        year()
    }

    fun format005(ldt: LocalDateTime): String {
        return ldt.format(FORMATTER_005)
    }

    fun formatDateEnteredOnFile(ld: LocalDate): String {
        return ld.format(FORMATTER_DATE_ENTERED_ON_FILE).takeLast(6)
    }

    fun formatYear(ld: LocalDate): String {
        return ld.format(FORMATTER_DATE_YEAR)
    }
}