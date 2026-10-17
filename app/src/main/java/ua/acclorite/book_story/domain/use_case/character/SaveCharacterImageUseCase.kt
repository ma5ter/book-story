/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.character

import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.domain.service.CoverImageHandler
import java.io.File
import javax.inject.Inject

private const val TAG = "SaveCharacterImage"

class SaveCharacterImageUseCase @Inject constructor(
    private val coverImageHandler: CoverImageHandler
) {

    /**
     * Caches the adjusted square [coverImage] as the portrait of the
     * character with [characterId], overwriting the previous portrait.
     */
    suspend operator fun invoke(characterId: Int, coverImage: CoverImage): Result<File> {
        logI(TAG, "Saving portrait of character [$characterId].")
        return coverImageHandler.saveCharacterImage(characterId, coverImage)
    }
}
