/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.repository

import ua.acclorite.book_story.domain.model.character.BookCharacter

interface CharacterRepository {

    /**
     * Retrieves the persisted characters catalog of the book.
     */
    suspend fun getCharacters(bookId: Int): Result<List<BookCharacter>>

    /**
     * Replaces the characters catalog of the book and
     * returns the persisted entries.
     */
    suspend fun saveCharacters(
        bookId: Int,
        characters: List<BookCharacter>
    ): Result<List<BookCharacter>>

    /**
     * Persists the edited character entry and returns the updated one.
     */
    suspend fun updateCharacter(character: BookCharacter): Result<BookCharacter>
}
