package com.puffinsoft.bibframe.conversion.bibframe2marc.util

import org.marc4k.marc.DataFieldBuilder
import java.util.regex.Pattern

internal object MarcKeyUtils {
    private val DATAFIELD_PATTERN: Pattern = Pattern.compile("(?<tag>^\\d{3})(?<indicators>[0-9a-z ]{1,2})(?<subfields>\\$[0-9a-z](.*)+?)")

    // TODO : I don't know if marcKeys and readMarc382s are meant to only have a single character for indicators or if it is just a mistake during output on the LC side.
    //  The code treats the strings as if there are always two indicators to the point of displaying an error on the id.loc.gov pages (see, as of this comment - https://id.loc.gov/resources/works/5733960.html).
    //  If it is fixed we can get rid of the SingleIndicatorConfig class and just parse the strings using the pattern (and expand the pattern to group each indicator separately).
    fun parseMarcKey(marcKey: String, singleIndicatorConfig: SingleIndicatorConfig = SingleIndicatorConfig(SingleIndicatorConfig.IndicatorPlacement.IGNORE)): DataFieldBuilder {
        return parse(DataFieldBuilder(), marcKey, singleIndicatorConfig, false)
    }

    fun parseMarcKey(builder: DataFieldBuilder, marcKey: String, singleIndicatorConfig: SingleIndicatorConfig = SingleIndicatorConfig(SingleIndicatorConfig.IndicatorPlacement.IGNORE)): DataFieldBuilder {
        return parse(builder, marcKey, singleIndicatorConfig, false)
    }

    fun parseReadMarc382(readMarc382: String, singleIndicatorConfig: SingleIndicatorConfig = SingleIndicatorConfig(SingleIndicatorConfig.IndicatorPlacement.IGNORE)): DataFieldBuilder {
        return parse(DataFieldBuilder(), readMarc382, singleIndicatorConfig, true)
    }

    private fun parse(builder: DataFieldBuilder, input: String, singleIndicatorConfig: SingleIndicatorConfig, stripAppliesTo: Boolean): DataFieldBuilder {
        val matcher = DATAFIELD_PATTERN.matcher(input)

        if (matcher.find()) {
            builder.apply { tag = matcher.group("tag") }

            val indicators = matcher.group("indicators")

            if (indicators.length == 1) {
                when (singleIndicatorConfig.indicatorPlacement) {
                    SingleIndicatorConfig.IndicatorPlacement.INDICATOR_1 -> builder.indicator1 = indicators[0]
                    SingleIndicatorConfig.IndicatorPlacement.INDICATOR_2 -> builder.indicator2 = indicators[0]
                    SingleIndicatorConfig.IndicatorPlacement.USE_DEFAULTS -> {
                        if (singleIndicatorConfig.defaultIndicator1 != null) {
                            builder.indicator1 = singleIndicatorConfig.defaultIndicator1
                        }
                        if (singleIndicatorConfig.defaultIndicator2 != null) {
                            builder.indicator2 = singleIndicatorConfig.defaultIndicator2
                        }
                    }
                    SingleIndicatorConfig.IndicatorPlacement.IGNORE -> {}
                }
            } else {
                builder.indicator1 = indicators[0]
                builder.indicator2 = indicators[1]
            }

            matcher.group("subfields").split('$').dropLastWhile { it.isEmpty() }.toTypedArray()
                .filter { it.isNotEmpty() }
                .filter { it[0] != '3' || !(stripAppliesTo && it[0] == '3') }
                .forEach {
                    builder.subfields {
                        subfield {
                            name = it[0]
                            data = if (it.length > 1) it.substring(1).replace(" \\p{Punct}$".toRegex(), "").trimEnd() else ""
                        }
                    }
                }

            return builder
        }

        throw RuntimeException("Could not parse marcKey/readMarc382: $input")
    }
}

internal class SingleIndicatorConfig @JvmOverloads constructor(val indicatorPlacement: IndicatorPlacement, val defaultIndicator1: Char? = null, val defaultIndicator2: Char? = null) {
    enum class IndicatorPlacement {
        INDICATOR_1,
        INDICATOR_2,
        USE_DEFAULTS,
        IGNORE
    }
}