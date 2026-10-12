/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.character

import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.domain.model.character.BookCharacter
import ua.acclorite.book_story.domain.repository.CharacterRepository
import javax.inject.Inject

private const val TAG = "GetCharacters"

class GetCharactersUseCase @Inject constructor(
    private val characterRepository: CharacterRepository
) {

    suspend operator fun invoke(bookId: Int): List<BookCharacter> {
        logI(TAG, "Getting characters catalog for book [$bookId].")
        if (bookId == -1) return emptyList()

        return characterRepository.getCharacters(bookId).fold(
            onSuccess = { characters ->
                logI(TAG, "Successfully loaded ${characters.size} characters for [$bookId].")
                characters
            },
            onFailure = {
                logE(TAG, "Could not load characters catalog with exception: ${it.message}")
                emptyList()
            }
        )
    }
}
