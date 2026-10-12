/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.character

import ua.acclorite.book_story.domain.model.character.BookCharacter
import ua.acclorite.book_story.domain.repository.CharacterRepository
import javax.inject.Inject

class UpdateCharacterUseCase @Inject constructor(
    private val characterRepository: CharacterRepository
) {

    /**
     * Persists the edited character entry and returns the updated one.
     */
    suspend operator fun invoke(character: BookCharacter): Result<BookCharacter> {
        return characterRepository.updateCharacter(character)
    }
}
