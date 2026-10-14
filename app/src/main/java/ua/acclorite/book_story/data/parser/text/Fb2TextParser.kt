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
import ua.acclorite.book_story.core.helpers.clearAllMarkdown
import ua.acclorite.book_story.core.helpers.containsVisibleText
import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.parser.document.DocumentParser
import ua.acclorite.book_story.domain.model.reader.ReaderText
import javax.inject.Inject

private const val TAG = "Fb2TextParser"

/** Number of words taken from the first paragraph for unnamed subchapters. */
private const val SUBTITLE_FALLBACK_WORD_COUNT = 5

/**
 * One block of a section's content: an optional <subtitle> chapter title
 * and the elements following it up to the next subtitle.
 */
private data class SubtitleBlock(
    val subtitle: String?,

    /** Clones fed into the parser, they get mutated while parsing. */
    val elements: MutableList<Element>,

    /** Originals of [elements] in the document's DOM, used for DOM matching. */
    val originalElements: MutableList<Element>
)

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
            parseWithElements(document).first
        } catch (e: Exception) {
            logE(TAG, "Could not parse text with message: ${e.message}.")
            emptyList()
        }
    }

    /**
     * Parses the given [document] the same way [parse] does and additionally
     * returns the [Element] of the document that produced every parsed entry,
     * parallel to the text (null for chapters and entries without a matching
     * element). The document itself is not modified: parsing works on clones.
     *
     * Used to locate paragraphs in the original DOM, e.g. to embed
     * generated images on FB2 export.
     */
    suspend fun parseWithElements(
        document: Document?
    ): Pair<List<ReaderText>, List<Element?>> {
        return try {
            // The first body is the main content, the second (notes) body
            // holds footnotes and should only be accessible by hyperlinks.
            val sections = document?.selectFirst("body")
                ?.children()
                ?.filter { it.tagName().equals("section", ignoreCase = true) }
                .orEmpty()

            val parsedSections = sections.map { parseSection(it, emptyList()) }
            val readerText = parsedSections.flatMap { it.first }
            val elementMap = parsedSections.flatMap { it.second }

            yield()

            if (
                readerText.filterIsInstance<ReaderText.Text>().isEmpty() ||
                readerText.filterIsInstance<ReaderText.Chapter>().isEmpty()
            ) {
                logE(TAG, "Could not extract text from FB2.")
                return emptyList<ReaderText>() to emptyList<Element?>()
            }

            logI(TAG, "Successfully finished FB2 parsing.")
            readerText to elementMap
        } catch (e: Exception) {
            logE(TAG, "Could not parse text with message: ${e.message}.")
            emptyList<ReaderText>() to emptyList<Element?>()
        }
    }

    /**
     * Parses a single [section] into its chapter and content, recursively
     * appending nested sections after the parent's content.
     *
     * @param parentTitles Titles of all ancestor sections, used to build the
     * full chapter title, same as in EPUB TOC parsing.
     *
     * @return Parsed text together with the source [Element] of every entry,
     * parallel to the text.
     */
    private suspend fun parseSection(
        section: Element,
        parentTitles: List<String>
    ): Pair<List<ReaderText>, List<Element?>> {
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
        }
        // The clone's children correspond 1:1, in the same order, to the
        // original section's content children. Originals are kept for
        // DOM matching: clones get mutated (markdown markup) while parsing.
        val contentOriginals = section.children().filter { child ->
            !child.tagName().equals("title", ignoreCase = true) &&
                    !child.tagName().equals("section", ignoreCase = true)
        }
        val contentClones = content.children()

        val readerText = mutableListOf<ReaderText>()
        val elementMap = mutableListOf<Element?>()

        val title = (parentTitles + titles).joinToString(" / ").trim()
        if (title.isNotBlank()) {
            readerText.add(
                ReaderText.Chapter(
                    title = title,
                    nested = parentTitles.isNotEmpty()
                )
            )
            elementMap.add(null)
        }
        // Subtitles split the section's content into subchapters: every
        // <subtitle> together with the following elements up to the next
        // <subtitle> (or the end of the section) forms one chapter.
        val blocks = mutableListOf(
            SubtitleBlock(
                subtitle = null,
                elements = mutableListOf(),
                originalElements = mutableListOf()
            )
        )
        contentClones.forEachIndexed { index, child ->
            if (child.tagName().equals("subtitle", ignoreCase = true)) {
                val subtitleText = child.text().trim()
                blocks.add(
                    SubtitleBlock(
                        // Separators like "* * *" carry no readable text,
                        // the chapter title is then derived from the content.
                        subtitle = subtitleText.takeIf { it.any(Char::isLetterOrDigit) }.orEmpty(),
                        elements = mutableListOf(),
                        originalElements = mutableListOf()
                    )
                )
            } else {
                blocks.last().elements.add(child)
                contentOriginals.getOrNull(index)?.let { original ->
                    blocks.last().originalElements.add(original)
                }
            }
        }

        blocks.forEach { block ->
            if (block.subtitle == null && block.elements.isEmpty()) return@forEach

            val parsed = if (block.elements.isEmpty()) {
                emptyList()
            } else {
                val shell = Document.createShell("")
                block.elements.forEach { shell.body().appendChild(it) }
                documentParser.parseDocument(shell, includeChapter = false)
            }
            if (parsed.isEmpty()) return@forEach

            val chapterTitle = when {
                block.subtitle == null -> null
                block.subtitle.isNotBlank() -> block.subtitle
                else -> fallbackChapterTitle(block.elements)
            }
            chapterTitle?.let { chapter ->
                readerText.add(
                    ReaderText.Chapter(
                        title = chapter,
                        nested = true
                    )
                )
                elementMap.add(null)
            }
            elementMap.addAll(matchElements(block.originalElements, parsed))
            readerText.addAll(parsed)
        }

        childSections.forEach { child ->
            val (childText, childMap) = parseSection(child, parentTitles + titles)
            readerText.addAll(childText)
            elementMap.addAll(childMap)
        }

        return readerText to elementMap
    }

    /**
     * Matches parsed [entries] to the original [elements] that produced them,
     * in reading order: first by trimmed text content (in both directions to
     * compensate for the markup transformations), unmatched entries fall back
     * to sequential correspondence. Ambiguous matches (repeated paragraphs)
     * resolve in reading order, same as the reader's paragraph selection.
     *
     * @return Elements parallel to [entries], null for entries
     * without a matching element.
     */
    private fun matchElements(
        elements: List<Element>,
        entries: List<ReaderText>
    ): List<Element?> {
        if (elements.isEmpty()) return List(entries.size) { null }

        val textElements = elements
            .filter { it.text().trim().containsVisibleText() }
        if (textElements.isEmpty()) return List(entries.size) { null }

        val map = arrayOfNulls<Element>(entries.size)
        val consumed = BooleanArray(textElements.size)
        // Element.text() traverses the DOM, precompute once for all entries.
        val elementTexts = textElements.map { it.text().trim() }

        // Matching by trimmed text content.
        entries.forEachIndexed { index, entry ->
            if (entry !is ReaderText.Text) return@forEachIndexed
            val text = entry.line.text.trim().clearAllMarkdown().trim()
            if (!text.containsVisibleText()) return@forEachIndexed

            textElements.indices.firstOrNull { candidate ->
                !consumed[candidate] && elementTexts[candidate].let { elementText ->
                    elementText.contains(text) || text.contains(elementText)
                }
            }?.let { candidate ->
                map[index] = textElements[candidate]
                consumed[candidate] = true
            }
        }

        // Sequential fallback for entries the content match could not find.
        var cursor = 0
        entries.forEachIndexed { index, entry ->
            if (map[index] != null) return@forEachIndexed
            if (entry !is ReaderText.Text || !entry.line.text.trim().containsVisibleText()) {
                return@forEachIndexed
            }

            val candidate = (cursor until textElements.size).firstOrNull { !consumed[it] }
                ?: return@forEachIndexed
            map[index] = textElements[candidate]
            consumed[candidate] = true
            cursor = candidate + 1
        }

        return map.toList()
    }

    /**
     * Builds a chapter title for a subtitle without readable text
     * (e.g. "* * *") from the first words of the block's first paragraph.
     */
    private fun fallbackChapterTitle(elements: List<Element>): String? {
        val firstParagraph = elements
            .firstOrNull { it.tagName().equals("p", ignoreCase = true) }
            ?.text()?.trim().takeIf { !it.isNullOrBlank() } ?: return null
        val words = firstParagraph.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        return words.take(SUBTITLE_FALLBACK_WORD_COUNT).joinToString(" ") + "…"
    }
}
