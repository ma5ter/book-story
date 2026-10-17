/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.collage

import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.core.collage.CollageRenderer
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.domain.use_case.book.GetTextUseCase
import javax.inject.Inject

private const val TAG = "GenerateBookCollage"

/**
 * Builds a square collage from the illustrations of a single book.
 *
 * Image source (current book only): illustrations of the parsed book
 * text — both embedded images ([ReaderText.Image] with
 * generated = false) and generated attachments (injected by
 * [GetTextUseCase] with generated = true, already decoded from their
 * saved files).
 */
class GenerateBookCollageUseCase @Inject constructor(
    private val getTextUseCase: GetTextUseCase,
) {

    /**
     * Renders a [size] x [size] collage from up to [maxImages] images
     * randomly chosen from the book's illustrations. Slots are filled in
     * shuffled order with scale-to-fill, center-crop cells, so the canvas
     * is always fully covered. Unused sources are ignored; images smaller
     * than the cell size are upscaled by the crop logic.
     *
     * @return The rendered collage bitmap.
     */
    suspend operator fun invoke(
        bookId: Int,
        size: Int = DEFAULT_SIZE,
        maxImages: Int = DEFAULT_MAX_IMAGES
    ): Result<CoverImage> = runCatching {
        withContext(Dispatchers.IO) {
            // GetTextUseCase already decodes completed attachments into
            // the parsed text (generated = true), so both image sources
            // come from this single pass.
            val images = getTextUseCase(bookId)
                .filterIsInstance<ReaderText.Image>()
                .map { it.imageBitmap.asAndroidBitmap() }
                .filter { it.width > 0 && it.height > 0 }

            if (images.isEmpty()) {
                throw IllegalStateException(
                    "No illustrations found for book [$bookId], collage is not possible."
                )
            }

            logI(
                TAG,
                "invoke: rendering ${size}x${size} collage from up to $maxImages " +
                        "of ${images.size} illustrations of book [$bookId]."
            )
            CollageRenderer.render(
                images = images,
                width = size,
                height = size,
                maxImages = maxImages
            )
        }
    }.onSuccess { collage ->
        logI(TAG, "invoke: succeeded, ${collage.width}x${collage.height}.")
    }.onFailure { throwable ->
        logI(TAG, "invoke: failed with ${throwable::class.java.simpleName}: ${throwable.message}.")
    }

    private companion object {

        /** Default collage side in pixels. */
        const val DEFAULT_SIZE = 1024

        /** Default maximum number of images in a collage. */
        const val DEFAULT_MAX_IMAGES = 9
    }
}
