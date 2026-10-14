/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.service

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.domain.model.attachment.ImageAttachment
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sequential steps of the AI image generation, shown in the
 * progress notification one by one.
 */
enum class ImageGenerationStep {

    /** Validates the attachment and appends the characters catalog to the selected text. */
    PREPARE,

    /** The text model distills the prepared input into a scene description. */
    DISTILL,

    /** The image model renders the illustration, which is saved and persisted. */
    GENERATE
}

/**
 * Live state of the image generation foreground service.
 */
@Immutable
data class ImageGenerationState(
    val bookId: Int? = null,
    val attachmentId: Int? = null,
    val isRunning: Boolean = false,
    val step: ImageGenerationStep? = null
)

/**
 * Result of a finished image generation, published once when the
 * service stops.
 */
sealed class ImageGenerationResult {
    abstract val attachmentId: Int

    data class Success(
        override val attachmentId: Int,
        val attachment: ImageAttachment,
        val image: CoverImage
    ) : ImageGenerationResult()

    data class Failure(
        override val attachmentId: Int,
        val step: ImageGenerationStep,
        val message: String
    ) : ImageGenerationResult()

    /**
     * The generation was cancelled by the user after a failed step.
     * The PENDING attachment row has already been dropped by the service.
     */
    data class Cancelled(
        override val attachmentId: Int
    ) : ImageGenerationResult()
}

/**
 * Holds the state and results of the image generation service,
 * bridging it with the reader UI.
 */
@Singleton
class ImageGenerationManager @Inject constructor() {

    private val _state = MutableStateFlow(ImageGenerationState())
    val state: StateFlow<ImageGenerationState> = _state.asStateFlow()

    private val _results =
        MutableSharedFlow<ImageGenerationResult>(extraBufferCapacity = 8)
    val results: SharedFlow<ImageGenerationResult> = _results.asSharedFlow()

    /**
     * Marks the generation of [attachmentId] as started.
     */
    fun start(bookId: Int, attachmentId: Int) {
        _state.value = ImageGenerationState(
            bookId = bookId,
            attachmentId = attachmentId,
            isRunning = true
        )
    }

    /**
     * Updates the current step of the running generation,
     * ignored when no generation is running.
     */
    fun updateStep(step: ImageGenerationStep) {
        _state.update { current ->
            if (current.isRunning) current.copy(step = step)
            else current
        }
    }

    /**
     * Clears the running state and publishes the [result].
     */
    fun finish(result: ImageGenerationResult) {
        // Idempotent: ignores a second finish from a racing caller
        // (e.g. onTimeout and a concurrently failing pipeline step).
        if (!_state.value.isRunning) return
        _state.value = ImageGenerationState()
        _results.tryEmit(result)
    }

    /**
     * Publishes the user's cancellation of [attachmentId]'s failed
     * generation. Unlike [finish], no running state is required, because
     * the generation has already failed by the time it is cancelled.
     */
    fun cancel(attachmentId: Int) {
        _results.tryEmit(ImageGenerationResult.Cancelled(attachmentId))
    }
}
