/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.attachment

import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.domain.model.attachment.ImageAttachment
import ua.acclorite.book_story.domain.repository.ImageAttachmentRepository
import javax.inject.Inject

private const val TAG = "GenerateImageForAttachment"

/**
 * Stepwise AI image generation pipeline, used by the image generation
 * foreground service. Steps are exposed individually, so a failed step
 * can be retried without repeating the preceding ones.
 */
class GenerateImageForAttachmentUseCase @Inject constructor(
    private val imageAttachmentRepository: ImageAttachmentRepository
) {

    /**
     * Prepares the generation input: verifies the attachment and appends
     * the persisted characters catalog to the selected text.
     */
    suspend fun prepare(attachmentId: Int, selectedText: String): Result<String> {
        logI(TAG, "Preparing image generation input for attachment [$attachmentId].")
        if (selectedText.isBlank()) {
            return Result.failure(IllegalStateException("Selected text is empty."))
        }

        return imageAttachmentRepository.prepareGenerationInput(
            attachmentId = attachmentId,
            selectedText = selectedText
        )
    }

    /**
     * Distills the prepared input into a compact English scene description
     * with the configured text assistant model.
     * [onProgress] is called each time the server emits a keep-alive
     * blank line while processing.
     */
    suspend fun distill(
        preparedInput: String,
        onProgress: () -> Unit = {}
    ): Result<String> {
        logI(TAG, "Distilling scene description, input length: ${preparedInput.length}.")
        return imageAttachmentRepository.distillSceneDescription(preparedInput, onProgress)
    }

    /**
     * Renders the scene with the configured image model, saves the image
     * and upgrades the attachment to COMPLETED.
     * [onProgress] is called each time the server emits a keep-alive
     * blank line while processing.
     */
    suspend fun complete(
        attachmentId: Int,
        sceneDescription: String,
        styleReferencePath: String?,
        onProgress: () -> Unit = {}
    ): Result<Pair<ImageAttachment, CoverImage>> {
        logI(TAG, "Rendering image for attachment [$attachmentId].")
        return imageAttachmentRepository.completeImageGeneration(
            attachmentId = attachmentId,
            sceneDescription = sceneDescription,
            styleReferencePath = styleReferencePath,
            onProgress = onProgress
        )
    }
}
