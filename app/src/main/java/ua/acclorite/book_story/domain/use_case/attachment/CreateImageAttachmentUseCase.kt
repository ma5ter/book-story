/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.attachment

import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.domain.model.attachment.ImageAttachment
import ua.acclorite.book_story.domain.repository.ImageAttachmentRepository
import javax.inject.Inject

private const val TAG = "CreateImageAttachment"

class CreateImageAttachmentUseCase @Inject constructor(
    private val imageAttachmentRepository: ImageAttachmentRepository
) {

    suspend operator fun invoke(
        bookId: Int,
        paragraphIndex: Int
    ): Result<ImageAttachment> {
        logI(TAG, "Creating pending image attachment for book [$bookId], paragraph [$paragraphIndex].")
        if (bookId == -1) {
            return Result.failure(IllegalStateException("Book id is invalid."))
        }

        return imageAttachmentRepository.createImageAttachment(
            bookId = bookId,
            paragraphIndex = paragraphIndex
        )
    }
}
