/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.repository

import android.graphics.BitmapFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.data.local.dto.BookAttachmentEntity
import ua.acclorite.book_story.data.local.room.BookDatabase
import su.mya.openrouter.OpenRouterClient
import ua.acclorite.book_story.domain.model.attachment.ImageAttachment
import ua.acclorite.book_story.domain.model.attachment.ImageAttachmentStatus
import ua.acclorite.book_story.domain.repository.ImageAttachmentRepository
import ua.acclorite.book_story.domain.service.CoverImageHandler
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageAttachmentRepositoryImpl @Inject constructor(
    private val database: BookDatabase,
    private val coverImageHandler: CoverImageHandler,
    private val openRouterClient: OpenRouterClient
) : ImageAttachmentRepository {

    override suspend fun createImageAttachment(
        bookId: Int,
        paragraphIndex: Int
    ): Result<ImageAttachment> = runCatching {
        withContext(Dispatchers.IO) {
            val entity = BookAttachmentEntity(
                bookId = bookId,
                paragraphIndex = paragraphIndex,
                filePath = "",
                status = ImageAttachmentStatus.PENDING.name
            )
            val id = database.bookAttachmentDao.insertAttachment(entity).toInt()
            entity.copy(id = id).toImageAttachment()
        }
    }

    override suspend fun generateImageForAttachment(
        attachmentId: Int,
        selectedText: String
    ): Result<Pair<ImageAttachment, CoverImage>> {
        return try {
            val result = withContext(Dispatchers.IO) {
                val current = database.bookAttachmentDao.findAttachmentById(attachmentId)
                    ?: throw IOException("Image attachment [$attachmentId] was not found.")

                val bitmap = openRouterClient.generateImage(selectedText).getOrThrow()
                val file = coverImageHandler.saveImageAttachment(bitmap).getOrThrow()

                val completedAttachment = current.copy(
                    filePath = file.absolutePath,
                    status = ImageAttachmentStatus.COMPLETED.name
                )
                try {
                    database.bookAttachmentDao.updateAttachment(completedAttachment)
                } catch (e: Exception) {
                    file.delete()
                    throw e
                }

                completedAttachment.toImageAttachment() to bitmap
            }
            Result.success(result)
        } catch (e: CancellationException) {
            deleteAttachmentIgnoringCancellation(attachmentId)
            throw e
        } catch (e: Exception) {
            deleteAttachmentIgnoringCancellation(attachmentId)
            Result.failure(e)
        }
    }

    override suspend fun getImageAttachments(bookId: Int): Result<List<ImageAttachment>> =
        runCatching {
            withContext(Dispatchers.IO) {
                // Pending attachments cannot survive a reader restart,
                // remove stale rows before returning the persisted ones.
                database.bookAttachmentDao.deletePendingAttachmentsForBook(bookId)
                database.bookAttachmentDao.getAttachmentsForBook(bookId)
                    .map { it.toImageAttachment() }
            }
        }

    override suspend fun getImageAttachmentImage(attachmentId: Int): Result<CoverImage> =
        runCatching {
            withContext(Dispatchers.IO) {
                val attachment = getAttachment(attachmentId)
                    ?: throw IOException("Image attachment [$attachmentId] was not found.")

                if (attachment.status != ImageAttachmentStatus.COMPLETED ||
                    attachment.filePath.isBlank()
                ) {
                    throw IOException("Image attachment [$attachmentId] is not completed.")
                }

                BitmapFactory.decodeFile(attachment.filePath)
                    ?: throw IOException("Couldn't read generated image file.")
            }
        }

    override suspend fun deleteImageAttachment(attachmentId: Int): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val attachment = database.bookAttachmentDao.findAttachmentById(attachmentId)
                ?: throw IOException("Image attachment [$attachmentId] was not found.")

            if (attachment.filePath.isNotBlank()) {
                File(attachment.filePath).delete()
            }
            database.bookAttachmentDao.deleteAttachmentById(attachmentId)
        }
    }

    private suspend fun getAttachment(attachmentId: Int): ImageAttachment? {
        return database.bookAttachmentDao.findAttachmentById(attachmentId)?.toImageAttachment()
    }

    private suspend fun deleteAttachmentIgnoringCancellation(attachmentId: Int) {
        withContext(NonCancellable) {
            runCatching { database.bookAttachmentDao.deleteAttachmentById(attachmentId) }
        }
    }

    private fun BookAttachmentEntity.toImageAttachment(): ImageAttachment {
        return ImageAttachment(
            id = id,
            bookId = bookId,
            paragraphIndex = paragraphIndex,
            filePath = filePath,
            status = ImageAttachmentStatus.entries.firstOrNull { it.name == status }
                ?: ImageAttachmentStatus.PENDING
        )
    }
}
