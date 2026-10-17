/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.character

import ua.acclorite.book_story.domain.service.CoverImageHandler
import java.io.File
import javax.inject.Inject

class GetCharacterImageUseCase @Inject constructor(
    private val coverImageHandler: CoverImageHandler
) {

    /**
     * Returns the cached portrait file of the character,
     * or null when no picture was loaded yet.
     */
    operator fun invoke(characterId: Int): File? {
        return coverImageHandler.getCharacterImageFile(characterId)
    }
}
