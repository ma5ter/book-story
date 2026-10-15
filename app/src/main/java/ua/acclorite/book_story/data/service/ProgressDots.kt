/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.service

/**
 * Cycles trailing dots (from 1 to 3 and around again) appended to a
 * progress notification text, advanced by download progress events, so
 * the notification keeps visibly moving even when the percentage cannot
 * be estimated.
 */
class ProgressDots {

    /** Current number of dots, 0 before the first advance. */
    private var count = 0

    /**
     * Advances the cycle and returns the next dot sequence.
     */
    fun next(): String {
        count = count % 3 + 1
        return ".".repeat(count)
    }
}
