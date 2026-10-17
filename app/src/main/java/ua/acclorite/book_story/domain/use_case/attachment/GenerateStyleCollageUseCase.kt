/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.attachment

import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.domain.service.CoverImageHandler
import ua.acclorite.book_story.domain.use_case.collage.GenerateBookCollageUseCase
import javax.inject.Inject

private const val TAG = "GenerateStyleCollage"

/**
 * Builds the style reference of the image generation pipeline: renders
 * a collage of the book's existing illustrations and saves it as a
 * reusable reference file. Supplied with the image generation request
 * as an image-to-image reference, so new illustrations keep the overall
 * style of the book.
 */
class GenerateStyleCollageUseCase @Inject constructor(
    private val generateBookCollageUseCase: GenerateBookCollageUseCase,
    private val coverImageHandler: CoverImageHandler
) {

    /**
     * Renders and saves the style collage of [bookId]. Returns the saved
     * reference file path, or null when the book has no illustrations yet
     * (first generation) or the collage could not be built — then the
     * pipeline proceeds without a style reference.
     */
    suspend operator fun invoke(bookId: Int): Result<String?> = runCatching {
        val collage = generateBookCollageUseCase(bookId).getOrNull()
        if (collage == null) {
            logI(TAG, "invoke: no style collage for book [$bookId], continuing without a reference.")
            return@runCatching null
        }

        coverImageHandler.saveStyleCollage(bookId, collage).getOrThrow().absolutePath
            .also { path -> logI(TAG, "invoke: saved style collage of book [$bookId] to $path.") }
    }.onFailure { throwable ->
        logI(TAG, "invoke: style collage failed, continuing without a reference: ${throwable.message}.")
    }
}
