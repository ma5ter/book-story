/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.repository

import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.domain.model.attachment.ImageAttachment

interface ImageAttachmentRepository {

    /**
     * Inserts a PENDING attachment row linked to the book paragraph.
     */
    suspend fun createImageAttachment(
        bookId: Int,
        paragraphIndex: Int
    ): Result<ImageAttachment>

    /**
     * Generates an image for the selected text via OpenRouter,
     * saves it to local storage and upgrades the attachment to COMPLETED.
     * Returns the completed attachment together with the generated bitmap.
     */
    suspend fun generateImageForAttachment(
        attachmentId: Int,
        selectedText: String
    ): Result<Pair<ImageAttachment, CoverImage>>

    /**
     * Retrieves persisted attachments for the book,
     * removing stale PENDING rows along the way.
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
