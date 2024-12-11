package com.puffinsoft.bibframe.conversion.bibframe2marc.util

import org.eclipse.rdf4j.model.Value

internal object TextUtils {
    fun getCodeCharFromUrl(url: String): Char {
        val lastSlashPosition = url.lastIndexOf('/')
        return url[lastSlashPosition + 1]
    }

    fun getCodeStringFromUrl(url: String, stripIdFromUrl: Boolean = false): String {
        val range = if (stripIdFromUrl) {
            IntRange(url.substringBeforeLast('/').lastIndexOf('/') + 1, url.lastIndexOf('/') - 1)
        } else {
            IntRange(url.lastIndexOf('/') + 1, url.length - 1)
        }
        return url.substring(range)
    }

    fun getCodeCharFromUrl(url: Value?): Char {
        if (url != null) {
            return getCodeCharFromUrl(url.stringValue())
        }
        return ' '
    }

    fun getCodeStringFromUrl(url: Value?, stripIdFromUrl: Boolean = false): String {
        if (url != null) {
            return getCodeStringFromUrl(url.stringValue(), stripIdFromUrl)
        }
        return ""
    }

    fun getCodeStringsFromUrls(urls: Set<Value>): Set<String> {
        return urls.map { getCodeStringFromUrl(it.stringValue()) }.toSet()
    }
}