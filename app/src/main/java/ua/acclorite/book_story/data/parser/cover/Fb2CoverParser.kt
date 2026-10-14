/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.cover

import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.data.model.file.CachedFile
import javax.inject.Inject

private const val TAG = "Fb2CoverParser"

internal const val XLINK_NAMESPACE = "http://www.w3.org/1999/xlink"

/**
 * Returns the prefix the document binds the XLink namespace to
 * (e.g. `l:` for `xmlns:l="http://www.w3.org/1999/xlink"`),
 * preferring the prefix already used by href references, or an empty
 * string when the namespace is declared as default.
 * Shared with the FB2 export.
 */
internal fun Document.fb2XlinkPrefix(): String {
    val used = allElements.asSequence()
        .mapNotNull { element ->
            element.attributes().firstOrNull { it.key.endsWith(":href") }?.key
        }
        .firstOrNull()?.substringBefore("href")
    if (!used.isNullOrBlank()) return used

    return children().firstOrNull()?.attributes()
        ?.firstOrNull { attribute ->
            attribute.key.startsWith("xmlns") && attribute.value == XLINK_NAMESPACE
        }?.key?.substringAfter("xmlns:")
        ?.let { "$it:" }
        ?: ""
}

class Fb2CoverParser @Inject constructor() : CoverParser {

    override suspend fun parse(cachedFile: CachedFile): CoverImage? {
        return try {
            withContext(Dispatchers.IO) {
                val document = cachedFile.openInputStream()?.use { stream ->
                    Jsoup.parse(stream, null, "", Parser.xmlParser())
                } ?: return@withContext null

                // The coverpage image is referenced by id from
                // <title-info>/<coverpage> and stored base64-encoded
                // in a top-level <binary> element.
                val imageElement = document
                    .selectFirst("description > title-info > coverpage > image")
                    ?: return@withContext null

                // The XLink namespace may be bound to an arbitrary prefix
                // (e.g. xmlns:l), Jsoup's XML parser does not resolve it,
                // so resolve the prefix from the document itself.
                val href = imageElement.attr("${document.fb2XlinkPrefix()}href")
                    .ifBlank {
                        imageElement.attributes()
                            .firstOrNull { it.key.endsWith(":href") }?.value
                            ?: imageElement.attr("href")
                    }
                    .removePrefix("#")
                if (href.isBlank()) return@withContext null

                val binary = document.selectFirst("binary[id=\"$href\"]")?.text()
                    ?: return@withContext null

                val bytes = Base64.decode(
                    binary.replace(Regex("\\s"), ""),
                    Base64.DEFAULT
                )
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            logE(TAG, "Could not parse cover image with message: ${e.message}.")
            null
        }
    }
}
