/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.book

import ua.acclorite.book_story.core.log.logE
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.data.mapper.file.FileMapper
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.repository.BookRepository
import ua.acclorite.book_story.domain.repository.ImageAttachmentRepository
import ua.acclorite.book_story.domain.service.Fb2BookExporter
import java.io.File
import javax.inject.Inject

private const val TAG = "ExportBookWithImages"

class ExportBookWithImagesUseCase @Inject constructor(
    private val bookRepository: BookRepository,
    private val imageAttachmentRepository: ImageAttachmentRepository,
    private val fileMapper: FileMapper,
    private val fb2BookExporter: Fb2BookExporter
) {

    /**
     * Exports the FB2 book as a new file with all completed
     * generated images embedded into the document.
     *
     * @return The exported file, ready for sharing.
     */
    suspend operator fun invoke(book: Book): Result<File> {
        logI(TAG, "Exporting [${book.title}] with generated images.")
        if (!book.filePath.endsWith(".fb2", ignoreCase = true)) {
            return Result.failure(
                IllegalStateException("Only FB2 books can be exported with images.")
            )
        }

        return bookRepository.getFileFromBook(book.id).mapCatching { file ->
            fileMapper.toCachedFile(file)
        }.mapCatching { cachedFile ->
            val attachments = imageAttachmentRepository
                .getImageAttachments(book.id).getOrThrow()
            fb2BookExporter.exportWithImages(cachedFile, attachments).getOrThrow()
        }.fold(
            onSuccess = {
                logI(TAG, "Successfully exported [${book.title}].")
                Result.success(it)
            },
            onFailure = {
                logE(TAG, "Could not export [${book.title}] with error: ${it.message}")
                Result.failure(it)
            }
        )
    }
}
