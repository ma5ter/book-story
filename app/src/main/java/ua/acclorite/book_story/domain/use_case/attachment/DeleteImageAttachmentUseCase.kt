/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.attachment

import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.domain.repository.ImageAttachmentRepository
import javax.inject.Inject

private const val TAG = "DeleteImageAttachment"

class DeleteImageAttachmentUseCase @Inject constructor(
    private val imageAttachmentRepository: ImageAttachmentRepository
) {

    suspend operator fun invoke(attachmentId: Int): Result<Unit> {
        logI(TAG, "Deleting image attachment [$attachmentId].")
        return imageAttachmentRepository.deleteImageAttachment(attachmentId)
    }
}
