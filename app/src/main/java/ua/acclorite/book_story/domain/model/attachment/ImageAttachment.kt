/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.model.attachment

import androidx.compose.runtime.Immutable

@Immutable
data class ImageAttachment(
    val id: Int,
    val bookId: Int,
    val paragraphIndex: Int,
    val filePath: String,
    val status: ImageAttachmentStatus
)

enum class ImageAttachmentStatus {
    PENDING,
    COMPLETED
}
