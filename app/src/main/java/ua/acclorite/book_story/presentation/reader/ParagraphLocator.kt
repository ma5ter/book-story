/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import ua.acclorite.book_story.domain.model.reader.ReaderText
import kotlin.math.abs

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
