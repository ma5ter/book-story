/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.model.character

import androidx.compose.runtime.Immutable

/**
 * A single entry of the book's characters catalog,
 * persisted as local book metadata.
 */
@Immutable
data class BookCharacter(
    val id: Int = 0,
    val bookId: Int = 0,
    val name: String,
    val description: String
)
