/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.attachment

import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.domain.repository.ImageAttachmentRepository
import javax.inject.Inject

class GetImageAttachmentImageUseCase @Inject constructor(
    private val imageAttachmentRepository: ImageAttachmentRepository
) {

    /**
     * Decodes the saved image file of a COMPLETED attachment.
     */
    suspend operator fun invoke(attachmentId: Int): CoverImage? {
        return imageAttachmentRepository.getImageAttachmentImage(attachmentId).getOrNull()
    }
}
