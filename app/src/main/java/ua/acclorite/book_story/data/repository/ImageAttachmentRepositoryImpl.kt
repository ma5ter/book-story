/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.repository

import android.app.Application
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.data.local.dto.BookAttachmentEntity
import ua.acclorite.book_story.data.local.room.BookDatabase
import ua.acclorite.book_story.R
import su.mya.openrouter.OpenRouterClient
import su.mya.openrouter.ImageReference
import su.mya.openrouter.ImageUrl
import su.mya.openrouter.ChatMessage
import su.mya.openrouter.MaxPrice
import su.mya.openrouter.PreferredMaxLatency
import su.mya.openrouter.ProviderRouting
import su.mya.openrouter.ProviderSort
import ua.acclorite.book_story.data.settings.SettingsManager
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
    private val openRouterClient: OpenRouterClient,
    private val settingsManager: SettingsManager,
    application: Application
) : ImageAttachmentRepository {

    /** System prompt of the scene distillation step. */
    private val scenePrompt: String = application.resources
        .openRawResource(R.raw.scene_prompt)
        .bufferedReader().use { it.readText() }
        .trim()

    override suspend fun createImageAttachment(
        bookId: Int,
        paragraphIndex: Int
    ): Result<ImageAttachment> = runCatching {
        withContext(Dispatchers.IO) {
            // Abandoned PENDING rows from previous attempts (process death,
            // dismissed retries) are dropped before a new attempt, so at most
            // one stale row per book accumulates. The older attempt's retry
            // notification is replaced by the new run anyway.
            database.bookAttachmentDao.deletePendingAttachmentsForBook(bookId)
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

    override suspend fun prepareGenerationInput(
        attachmentId: Int,
        selectedText: String
    ): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val current = database.bookAttachmentDao.findAttachmentById(attachmentId)
                ?: throw IOException("Image attachment [$attachmentId] was not found.")

            // The persisted characters catalog gives the text model a
            // visual reference, so recurring characters are reproducible
            // across generations. It is appended to the selected text
            // before the scene description is distilled.
            val charactersDescription = database.bookCharacterDao
                .getCharactersForBook(current.bookId)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(separator = "\n\n") { character ->
                    "# ${character.name}\n\n${character.description}".trim()
                }?.let { catalog ->
                    "$CHARACTERS_ADDENDUM_HEADER\n\n$catalog"
                }

            if (charactersDescription == null) selectedText
            else "$selectedText\n\n$charactersDescription"
        }
    }

    override suspend fun distillSceneDescription(
        preparedInput: String,
        onProgress: () -> Unit
    ): Result<String> =
        runCatching {
            withContext(Dispatchers.IO) {
                openRouterClient.chatCompletion(
                    model = settingsManager.textAssistantModel.lastValue.trim(),
                    messages = listOf(
                        ChatMessage(role = "system", content = scenePrompt),
                        ChatMessage(role = "user", content = preparedInput)
                    ),
                    provider = ProviderRouting(
                        // Prefer the highest-throughput provider within the
                        // configured latency (p90) and hard price limits.
                        sort = ProviderSort.OfObject(by = "throughput"),
                        preferredMaxLatency = PreferredMaxLatency(
                            p90 = settingsManager.textAssistantPreferredLatency.lastValue
                        ),
                        maxPrice = MaxPrice(
                            prompt = settingsManager.textAssistantMaxInputPrice.lastValue,
                            completion = COMPLETION_MAX_PRICE
                        )
                    ),
                    onProgress = onProgress
                ).mapCatching { message ->
                    message.content?.trim()?.takeIf { it.isNotBlank() }
                        ?: throw IOException("Model returned no scene description.")
                }.getOrThrow()
            }
        }

    override suspend fun completeImageGeneration(
        attachmentId: Int,
        sceneDescription: String,
        styleReferencePath: String?,
        onProgress: () -> Unit
    ): Result<Pair<ImageAttachment, CoverImage>> = runCatching {
        withContext(Dispatchers.IO) {
            val current = database.bookAttachmentDao.findAttachmentById(attachmentId)
                ?: throw IOException("Image attachment [$attachmentId] was not found.")

            val inputReferences = encodeStyleReference(styleReferencePath)
                ?.takeIf { settingsManager.imageGenerationStyleReferenceEnabled.lastValue }
                ?.let { url ->
                    listOf(ImageReference(type = "image_url", imageUrl = ImageUrl(url)))
                }
                .orEmpty()

            val styleBlock = settingsManager.imageGenerationStyle.lastValue.trim()
            var prompt = if (styleBlock.isBlank()) {
                sceneDescription
            } else {
                "<STYLE_BLOCK\n$styleBlock\n>\n\n$sceneDescription"
            }
            if (inputReferences.isNotEmpty()) {
                prompt += "\n\nUse attached image for style reference only"
            }

            val bitmap = openRouterClient.generateImage(
                prompt = prompt,
                inputReferences = inputReferences,
                onProgress = onProgress
            ).getOrThrow()
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
    }

    override suspend fun getImageAttachments(bookId: Int): Result<List<ImageAttachment>> =
        runCatching {
            withContext(Dispatchers.IO) {
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

    /**
     * Reads the style reference collage from [path] and encodes it as a
     * base64 data URL for the image generation request. Returns null when
     * the reference is missing or unreadable — the generation then
     * proceeds without it.
     */
    private fun encodeStyleReference(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val bytes = runCatching { File(path).takeIf { it.exists() }?.readBytes() }
            .getOrNull()
            ?: return null
        return "data:image/webp;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
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

    private companion object {

        /** Fixed completion price ceiling ($/1M tokens) of text assistant requests. */
        private const val COMPLETION_MAX_PRICE = 10.0

        /** Header marking the characters catalog as an addendum to the system prompt. */
        private const val CHARACTERS_ADDENDUM_HEADER =
            "Characters of this book, for visual consistency across illustrations, **do not include unrelated to the selected fragment into image generation**:"
    }
}
