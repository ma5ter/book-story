/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.text

import kotlinx.coroutines.yield
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import ua.acclorite.book_story.core.helpers.containsVisibleText
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.parser.document.DocumentParser
import ua.acclorite.book_story.domain.model.reader.ReaderText
import javax.inject.Inject

private const val TAG = "Fb2TextParser"

/**
 * Parses FictionBook 2 files.
 * Unlike the generic XML parsing, sections of the main body are split into
 * chapters: every [section]'s title becomes a chapter and nested sections
 * are appended after their parent's content.
 */
class Fb2TextParser @Inject constructor(
    private val documentParser: DocumentParser
) : TextParser {

    override suspend fun parse(cachedFile: CachedFile): List<ReaderText> {
        logI(TAG, "Started FB2 parsing: ${cachedFile.name}.")

        return try {
            val document = cachedFile.openInputStream()?.use { stream ->
                Jsoup.parse(stream, null, "", Parser.xmlParser())
            }

            // The first body is the main content, the second (notes) body
            // holds footnotes and should only be accessible by hyperlinks.
            val sections = document?.selectFirst("body")
                ?.children()
                ?.filter { it.tagName().equals("section", ignoreCase = true) }
                .orEmpty()

            val readerText = sections.flatMap { parseSection(it, emptyList()) }

            yield()

            if (
                readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
                readerText.filterIsInstance<ReaderText.Chapter>().isEmpty()
            ) {
                logE(TAG, "Could not extract text from FB2.")
                return emptyList()
            }

            logI(TAG, "Successfully finished FB2 parsing.")
            readerText
        } catch (e: Exception) {
            logE(TAG, "Could not parse text with message: ${e.message}.")
            emptyList()
        }
    }

    /**
     * Parses a single [section] into its chapter and content, recursively
     * appending nested sections after the parent's content.
     *
     * @param parentTitles Titles of all ancestor sections, used to build the
     * full chapter title, same as in EPUB TOC parsing.
     */
    private suspend fun parseSection(
        section: Element,
        parentTitles: List<String>
    ): List<ReaderText> {
        yield()

        val titles = section.select("> title > p")
            .map { it.text().trim() }
            .filter { it.containsVisibleText() }
        val childSections = section.children()
            .filter { it.tagName().equals("section", ignoreCase = true) }

        // Section's own content without the title and nested sections.
        val content = section.clone().apply {
            children().forEach { child ->
                if (child.tagName().equals("title", ignoreCase = true) ||
                    child.tagName().equals("section", ignoreCase = true)
                ) {
                    child.remove()
                }
            }
        }.let { contentElement ->
            val shell = Document.createShell("")
            shell.body().appendChild(contentElement)
            documentParser.parseDocument(shell, includeChapter = false)
        }

        val readerText = mutableListOf<ReaderText>()

        val title = (parentTitles + titles).joinToString(" / ").trim()
        if (title.isNotBlank()) {
            readerText.add(
                ReaderText.Chapter(
                    title = title,
                    nested = parentTitles.isNotEmpty()
                )
            )
        }
        readerText.addAll(content)

        childSections.forEach { child ->
            readerText.addAll(parseSection(child, parentTitles + titles))
        }

        return readerText
    }
}
