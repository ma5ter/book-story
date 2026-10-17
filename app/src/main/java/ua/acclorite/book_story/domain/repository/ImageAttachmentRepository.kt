/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.repository

import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.domain.model.attachment.ImageAttachment

interface ImageAttachmentRepository {

    /**
     * Inserts a PENDING attachment row linked to the book paragraph,
     * dropping abandoned PENDING rows of previous attempts for the book.
     */
    suspend fun createImageAttachment(
        bookId: Int,
        paragraphIndex: Int
    ): Result<ImageAttachment>

    /**
     * Prepares the generation input of [attachmentId]: verifies the
     * attachment exists and appends the persisted characters catalog
     * to the [selectedText].
     */
    suspend fun prepareGenerationInput(
        attachmentId: Int,
        selectedText: String
    ): Result<String>

    /**
     * Distills the [preparedInput] into a compact English scene description
     * with the configured text assistant model. [onProgress] is called each
     * time the server emits a keep-alive blank line while processing.
     */
    suspend fun distillSceneDescription(
        preparedInput: String,
        onProgress: () -> Unit = {}
    ): Result<String>

    /**
     * Renders the [sceneDescription] with the configured image model,
     * with the optional [styleReferencePath] style collage attached as an
     * image-to-image reference to keep the overall style. Saves the image
     * and upgrades the attachment to COMPLETED.
     * Returns the completed attachment together with the generated bitmap.
     * [onProgress] is called each time the server emits a keep-alive
     * blank line while processing.
     */
    suspend fun completeImageGeneration(
        attachmentId: Int,
        sceneDescription: String,
        styleReferencePath: String?,
        onProgress: () -> Unit = {}
    ): Result<Pair<ImageAttachment, CoverImage>>

    /**
     * Retrieves persisted attachments for the book,
     */
    suspend fun getImageAttachments(bookId: Int): Result<List<ImageAttachment>>

    /**
     * Decodes the saved image file of a COMPLETED attachment.
     */
    suspend fun getImageAttachmentImage(attachmentId: Int): Result<CoverImage>

    /**
     * Removes the attachment row together with its saved image file.
     */
    suspend fun deleteImageAttachment(attachmentId: Int): Result<Unit>
}
