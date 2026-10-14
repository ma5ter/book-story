/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.service

import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.domain.model.attachment.ImageAttachment
import java.io.File

interface Fb2BookExporter {

    /**
     * Exports the given FB2 file as a new file with all completed
     * generated images embedded into the document.
     *
     * @return The exported file, ready for sharing.
     */
    suspend fun exportWithImages(
        cachedFile: CachedFile,
        attachments: List<ImageAttachment>
    ): Result<File>
}
