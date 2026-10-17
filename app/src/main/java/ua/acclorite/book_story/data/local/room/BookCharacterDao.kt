/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import ua.acclorite.book_story.data.local.dto.BookCharacterEntity

@Dao
interface BookCharacterDao {

    @Query("SELECT * FROM BookCharacterEntity WHERE bookId = :bookId ORDER BY id ASC")
    suspend fun getCharactersForBook(bookId: Int): List<BookCharacterEntity>

    @Insert
    suspend fun insertCharacters(characters: List<BookCharacterEntity>)

    @Update
    suspend fun updateCharacter(character: BookCharacterEntity)

    @Query("DELETE FROM BookCharacterEntity WHERE bookId = :bookId")
    suspend fun deleteCharactersForBook(bookId: Int)
}
