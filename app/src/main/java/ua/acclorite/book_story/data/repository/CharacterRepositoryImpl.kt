/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.data.local.dto.BookCharacterEntity
import ua.acclorite.book_story.data.local.room.BookDatabase
import ua.acclorite.book_story.domain.model.character.BookCharacter
import ua.acclorite.book_story.domain.repository.CharacterRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepositoryImpl @Inject constructor(
    private val database: BookDatabase
) : CharacterRepository {

    override suspend fun getCharacters(bookId: Int): Result<List<BookCharacter>> = runCatching {
        withContext(Dispatchers.IO) {
            database.bookCharacterDao.getCharactersForBook(bookId).map { it.toBookCharacter() }
        }
    }

    override suspend fun saveCharacters(
        bookId: Int,
        characters: List<BookCharacter>
    ): Result<List<BookCharacter>> = runCatching {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                database.bookCharacterDao.deleteCharactersForBook(bookId)
                database.bookCharacterDao.insertCharacters(
                    characters.map { character ->
                        BookCharacterEntity(
                            bookId = bookId,
                            name = character.name,
                            description = character.description
                        )
                    }
                )
            }
            database.bookCharacterDao.getCharactersForBook(bookId).map { it.toBookCharacter() }
        }
    }

    override suspend fun updateCharacter(character: BookCharacter): Result<BookCharacter> = runCatching {
        withContext(Dispatchers.IO) {
            val entity = BookCharacterEntity(
                id = character.id,
                bookId = character.bookId,
                name = character.name,
                description = character.description
            )
            database.bookCharacterDao.updateCharacter(entity)
            entity.toBookCharacter()
        }
    }

    private fun BookCharacterEntity.toBookCharacter(): BookCharacter {
        return BookCharacter(
            id = id,
            bookId = bookId,
            name = name,
            description = description
        )
    }
}
