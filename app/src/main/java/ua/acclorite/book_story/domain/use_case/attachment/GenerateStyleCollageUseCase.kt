/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.attachment

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.core.collage.CollageRenderer
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.domain.model.attachment.ImageAttachmentStatus
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.domain.repository.ImageAttachmentRepository
import ua.acclorite.book_story.domain.service.CoverImageHandler
import ua.acclorite.book_story.domain.use_case.book.GetTextUseCase
import ua.acclorite.book_story.domain.use_case.character.GetCharacterImageUseCase
import javax.inject.Inject

private const val TAG = "GenerateStyleCollage"

/**
 * Builds the style reference of the image generation pipeline: renders a
 * collage and saves it as a reusable reference file. Supplied with the
 * image generation request as an image-to-image reference, so new
 * illustrations keep the overall style of the book and the designs of the
 * involved characters.
 *
 * Image selection, in priority order:
 * 1. Portraits of the characters included in the scene, resolved from the
 *    prompt's identity blocks with the illustration-character binding.
 * 2. The two nearest generated illustrations before and the two after the
 *    paragraph being illustrated.
 * 3. Random illustrations of the book to fill the remaining slots.
 */
class GenerateStyleCollageUseCase @Inject constructor(
    private val getTextUseCase: GetTextUseCase,
    private val imageAttachmentRepository: ImageAttachmentRepository,
    private val getCharacterImageUseCase: GetCharacterImageUseCase,
    private val coverImageHandler: CoverImageHandler
) {

    /**
     * Renders and saves the style collage of [bookId]. [attachmentId] is the
     * attachment being generated, its paragraph anchors the neighboring
     * illustrations; [sceneDescription] provides the identity blocks used
     * to resolve the involved characters. Returns the saved reference file
     * path, or null when the book has no illustrations yet (first
     * generation) or the collage could not be built — then the pipeline
     * proceeds without a style reference.
     */
    suspend operator fun invoke(
        bookId: Int,
        attachmentId: Int = -1,
        sceneDescription: String? = null,
        size: Int = DEFAULT_SIZE,
        maxImages: Int = DEFAULT_MAX_IMAGES
    ): Result<String?> = runCatching {
        val selected = selectImages(bookId, attachmentId, sceneDescription, maxImages)
        if (selected.isEmpty()) {
            logI(TAG, "invoke: no style collage for book [$bookId], continuing without a reference.")
            return@runCatching null
        }

        logI(
            TAG,
            "invoke: rendering ${size}x${size} collage from ${selected.size} " +
                    "sources of book [$bookId]."
        )
        val collage = withContext(Dispatchers.IO) {
            CollageRenderer.render(
                images = selected,
                width = size,
                height = size,
                maxImages = maxImages
            )
        }

        coverImageHandler.saveStyleCollage(bookId, collage).getOrThrow().absolutePath
            .also { path -> logI(TAG, "invoke: saved style collage of book [$bookId] to $path.") }
    }.onFailure { throwable ->
        logI(TAG, "invoke: style collage failed, continuing without a reference: ${throwable.message}.")
    }

    /**
     * Selects the collage sources in priority order, capped at [maxImages]:
     * involved character portraits, the neighboring illustrations of the
     * attachment's paragraph, then random illustrations of the book.
     */
    private suspend fun selectImages(
        bookId: Int,
        attachmentId: Int,
        sceneDescription: String?,
        maxImages: Int
    ): List<CoverImage> {
        val selected = mutableListOf<CoverImage>()

        // 1. Portraits of the characters included in the scene, resolved
        // from the prompt's identity blocks.
        val involvedCharacterIds = sceneDescription?.takeIf { it.isNotBlank() }
            ?.let { description ->
                imageAttachmentRepository
                    .getInvolvedCharacterIds(bookId, description)
                    .getOrNull()
                    .orEmpty()
            }
            .orEmpty()
        involvedCharacterIds.forEach { characterId ->
            if (selected.size >= maxImages) return@forEach
            getCharacterImageUseCase(characterId)?.let { portrait ->
                BitmapFactory.decodeFile(portrait.absolutePath)?.let(selected::add)
            }
        }
        logI(TAG, "selectImages: ${selected.size} involved character portraits.")

        val attachments = imageAttachmentRepository.getImageAttachments(bookId)
            .getOrNull().orEmpty()
            .filter {
                it.status == ImageAttachmentStatus.COMPLETED && it.filePath.isNotBlank()
            }
            .sortedBy { it.paragraphIndex }
        val paragraphIndex = attachments.find { it.id == attachmentId }?.paragraphIndex

        // 2. The two nearest generated illustrations before and the two
        // after the paragraph being illustrated.
        val neighborIds = mutableSetOf<Int>()
        if (paragraphIndex != null) {
            attachments.filter { it.paragraphIndex < paragraphIndex }
                .takeLast(NEIGHBOR_COUNT)
                .plus(
                    attachments.filter { it.paragraphIndex > paragraphIndex }
                        .take(NEIGHBOR_COUNT)
                )
                .forEach { attachment ->
                    if (selected.size >= maxImages) return@forEach
                    BitmapFactory.decodeFile(attachment.filePath)?.let { bitmap ->
                        selected.add(bitmap)
                        neighborIds.add(attachment.id)
                    }
                }
        }
        logI(TAG, "selectImages: ${neighborIds.size} neighboring illustrations.")

        // 3. Random illustrations of the book fill the remaining slots.
        // Generated illustrations already added as neighbors are excluded.
        getTextUseCase(bookId)
            .filterIsInstance<ReaderText.Image>()
            .filter { it.attachmentId == null || it.attachmentId !in neighborIds }
            .filter { it.imageBitmap.width > 0 && it.imageBitmap.height > 0 }
            .shuffled()
            .forEach { image ->
                if (selected.size >= maxImages) return@forEach
                selected.add(image.imageBitmap.asAndroidBitmap())
            }

        return selected
    }

    private companion object {

        /** Default collage side in pixels. */
        private const val DEFAULT_SIZE = 1024

        /** Default maximum number of images in a collage. */
        private const val DEFAULT_MAX_IMAGES = 9

        /** Nearest illustrations taken before and after the paragraph. */
        private const val NEIGHBOR_COUNT = 2
    }
}
