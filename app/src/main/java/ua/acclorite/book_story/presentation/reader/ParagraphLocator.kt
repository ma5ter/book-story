/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import ua.acclorite.book_story.domain.model.reader.ReaderText
import kotlin.math.abs

/** Fraction of the text model's max input tokens used for image context. */
private const val CONTEXT_TOKEN_FRACTION = 10

/** Conservative characters-per-token ratio, same as the characters extraction. */
private const val CHARS_PER_TOKEN = 3

/** Absolute context cap in characters, so a large user-set max tokens
 *  value cannot produce an unbounded chapter context. */
private const val MAX_CONTEXT_CHARS = 60_000

/**
 * Finds the displayed index of the paragraph the selected text belongs to.
 *
 * Matching strategies, from strongest to weakest:
 * 1. The selection is fully contained in a single paragraph
 *    (or equals it). When several paragraphs match — e.g. a repeated
 *    phrase — the paragraph closest to the currently visible item wins.
 * 2. The selection spans several paragraphs: it either starts with a
 *    whole paragraph or ends inside one (then the paragraph's beginning
 *    is the tail of the selected text).
 * 3. Nothing matched — fall back to the visible item index.
 */
internal fun findSelectedParagraphIndex(
    selectedText: String,
    text: List<ReaderText>,
    visibleItemIndex: Int
): Int {
    val selected = selectedText.trim()
    if (selected.isEmpty()) return visibleItemIndex

    val fullMatches = mutableListOf<Int>()
    val partialMatches = mutableListOf<Int>()

    text.forEachIndexed { index, entry ->
        if (entry !is ReaderText.Text) return@forEachIndexed
        val paragraph = entry.line.text.trim()
        if (paragraph.isEmpty()) return@forEachIndexed

        when {
            // Selection is entirely within this paragraph
            paragraph.contains(selected) -> fullMatches.add(index)
            // Selection starts with this whole paragraph
            paragraph.length > 20 && selected.startsWith(paragraph) ->
                partialMatches.add(index)
            // Selection ends inside this paragraph: then the
            // paragraph's beginning is the tail of the selected text
            paragraph.length > 20 && selected.endsWith(paragraph.take(20)) ->
                partialMatches.add(index)
        }
    }

    fun byProximity(candidates: List<Int>): Int? =
        candidates.minByOrNull { abs(it - visibleItemIndex) }

    return byProximity(fullMatches)
        ?: byProximity(partialMatches)
        ?: visibleItemIndex
}

/**
 * Extracts the local context of the paragraph at [index] as plain text for
 * AI image generation. The extraction starts at the nearest chapter heading
 * before the paragraph — a nested one, when the paragraph lies inside a
 * subchapter — and ends at the next chapter heading, so only the containing
 * subchapter is included; when the paragraph is not inside a subchapter, the
 * whole chapter is used as a fallback. The heading title is prepended and
 * injected images and other non-text entries are skipped. The context is
 * limited to roughly a tenth of the text model's max input tokens, centered
 * on the selected text. Returns null when the context contains no usable
 * text.
 *
 * @param selectedText Selected fragment used to center the trimmed context.
 * @param textModelMaxTokens Max input tokens of the text assistant model.
 */
internal fun findChapterText(
    index: Int,
    text: List<ReaderText>,
    selectedText: String,
    textModelMaxTokens: Int
): String? {
    val chapterStart = text.take(index + 1)
        .indexOfLast { it is ReaderText.Chapter }
        .let { if (it == -1) 0 else it }
    val chapterEnd = text.drop(chapterStart + 1)
        .indexOfFirst { it is ReaderText.Chapter }
        .let { if (it == -1) text.size else it + chapterStart + 1 }

    val chapterText = buildString {
        (text.getOrNull(chapterStart) as? ReaderText.Chapter)
            ?.let { chapter -> append(chapter.title.trim()) }
        for (i in chapterStart + 1 until chapterEnd) {
            val entry = text[i]
            if (entry is ReaderText.Text && entry.line.text.isNotBlank()) {
                if (isNotEmpty()) append('\n')
                append(entry.line.text.trim())
            }
        }
    }.takeIf { it.isNotBlank() } ?: return null

    return limitChapterContext(
        context = chapterText,
        selectedText = selectedText,
        index = index,
        text = text,
        textModelMaxTokens = textModelMaxTokens
    )
}

/**
 * Trims the context to roughly a tenth of the text model's max input tokens,
 * centered on the selected text (or on the selected paragraph when the
 * selection is not found inside the context, e.g. it spans several
 * paragraphs). Boundaries snap to line breaks, so words are not cut in
 * half; trimmed edges are marked with an ellipsis.
 */
private fun limitChapterContext(
    context: String,
    selectedText: String,
    index: Int,
    text: List<ReaderText>,
    textModelMaxTokens: Int
): String {
    val budgetChars = minOf(
        textModelMaxTokens / CONTEXT_TOKEN_FRACTION * CHARS_PER_TOKEN,
        MAX_CONTEXT_CHARS
    )
    if (budgetChars <= 0 || context.length <= budgetChars) return context

    val trimmedSelection = selectedText.trim()
    val paragraphText = (text.getOrNull(index) as? ReaderText.Text)
        ?.line?.text?.trim().orEmpty()
    var anchor = if (trimmedSelection.isBlank()) -1 else context.indexOf(trimmedSelection)
    if (anchor == -1 && paragraphText.isNotBlank()) anchor = context.indexOf(paragraphText)
    if (anchor == -1) anchor = context.length / 2

    var start = (anchor - budgetChars / 2).coerceAtLeast(0)
    var end = (start + budgetChars).coerceAtMost(context.length)
    start = (end - budgetChars).coerceAtLeast(0)

    if (start > 0) {
        context.lastIndexOf('\n', start).takeIf { it != -1 }?.let { start = it + 1 }
    }
    if (end < context.length) {
        context.indexOf('\n', end).let { end = if (it == -1) context.length else it + 1 }
    }

    return buildString {
        if (start > 0) append('…')
        append(context, start, end)
        if (end < context.length) append('…')
    }
}
