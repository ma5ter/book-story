/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.attachment

import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.domain.model.attachment.ImageAttachment
import ua.acclorite.book_story.domain.repository.ImageAttachmentRepository
import javax.inject.Inject

private const val TAG = "GetImageAttachments"

class GetImageAttachmentsUseCase @Inject constructor(
    private val imageAttachmentRepository: ImageAttachmentRepository
) {

    suspend operator fun invoke(bookId: Int): List<ImageAttachment> {
        logI(TAG, "Getting image attachments for book [$bookId].")
        if (bookId == -1) return emptyList()

        return imageAttachmentRepository.getImageAttachments(bookId).fold(
            onSuccess = { attachments ->
                logI(TAG, "Successfully loaded ${attachments.size} image attachments for [$bookId].")
                attachments
            },
            onFailure = {
                logE(TAG, "Could not load image attachments with exception: ${it.message}")
                emptyList()
            }
        )
    }
}
