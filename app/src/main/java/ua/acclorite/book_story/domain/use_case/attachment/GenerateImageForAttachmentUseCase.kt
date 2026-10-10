/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.attachment

import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.domain.model.attachment.ImageAttachment
import ua.acclorite.book_story.domain.repository.ImageAttachmentRepository
import javax.inject.Inject

private const val TAG = "GenerateImageForAttachment"

class GenerateImageForAttachmentUseCase @Inject constructor(
    private val imageAttachmentRepository: ImageAttachmentRepository
) {

    suspend operator fun invoke(
        attachmentId: Int,
        selectedText: String
    ): Result<Pair<ImageAttachment, CoverImage>> {
        logI(TAG, "Generating image for attachment [$attachmentId].")
        if (selectedText.isBlank()) {
            return Result.failure(IllegalStateException("Selected text is empty."))
        }

        return imageAttachmentRepository.generateImageForAttachment(
            attachmentId = attachmentId,
            selectedText = selectedText
        )
    }
}
