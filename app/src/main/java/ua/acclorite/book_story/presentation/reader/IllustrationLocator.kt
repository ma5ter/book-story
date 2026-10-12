/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import androidx.compose.runtime.Immutable
import ua.acclorite.book_story.domain.model.attachment.ImageAttachment
import ua.acclorite.book_story.domain.model.attachment.ImageAttachmentStatus
import ua.acclorite.book_story.domain.model.reader.ReaderText

/**
 * An AI illustration listed in the reader settings bottom sheet,
 * identified by a short text snippet of the paragraph it illustrates.
 */
@Immutable
data class ReaderIllustration(
    val attachmentId: Int,
    val paragraphIndex: Int,
    val snippet: String
)

/**
 * Maps completed image attachments of the current book to short identifying
 * snippets. The paragraph a generated image is attached to is addressed by
 * its index in the underlying parsed text (without injected images); its
 * displayed index is shifted by every injected image with a smaller
 * paragraph index.
 */
internal fun findIllustrationItems(
    text: List<ReaderText>,
    attachments: List<ImageAttachment>
): List<ReaderIllustration> {
    if (text.isEmpty()) return emptyList()

    val completed = attachments
        .filter { it.status == ImageAttachmentStatus.COMPLETED }
        .sortedBy { it.paragraphIndex }
    if (completed.isEmpty()) return emptyList()

    return completed.map { attachment ->
        val displayedIndex = (attachment.paragraphIndex +
                completed.count { it.paragraphIndex < attachment.paragraphIndex })
            .coerceIn(0, text.lastIndex)

        val snippet = text.take(displayedIndex + 1)
            .lastOrNull { entry ->
                entry is ReaderText.Text && entry.line.text.isNotBlank()
            }?.let { (it as ReaderText.Text).line.text.trim() }
            .orEmpty()

        ReaderIllustration(
            attachmentId = attachment.id,
            paragraphIndex = attachment.paragraphIndex,
            snippet = snippet
        )
    }
}
