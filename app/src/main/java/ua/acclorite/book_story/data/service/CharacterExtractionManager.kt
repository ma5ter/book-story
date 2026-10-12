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
import ua.acclorite.book_story.domain.model.character.BookCharacter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live state of the character extraction foreground service.
 * [progress] is the processing percentage, null while indeterminate.
 */
@Immutable
data class CharacterExtractionState(
    val bookId: Int? = null,
    val isRunning: Boolean = false,
    val progress: Int? = null
)

/**
 * Result of a finished character extraction, published once
 * when the service stops.
 */
sealed class CharacterExtractionResult {
    abstract val bookId: Int

    data class Success(
        override val bookId: Int,
        val characters: List<BookCharacter>
    ) : CharacterExtractionResult()

    data class Failure(
        override val bookId: Int,
        val message: String
    ) : CharacterExtractionResult()
}

/**
 * Holds the state and results of the character extraction service,
 * bridging it with the reader UI.
 */
@Singleton
class CharacterExtractionManager @Inject constructor() {

    private val _state = MutableStateFlow(CharacterExtractionState())
    val state: StateFlow<CharacterExtractionState> = _state.asStateFlow()

    private val _results =
        MutableSharedFlow<CharacterExtractionResult>(extraBufferCapacity = 8)
    val results: SharedFlow<CharacterExtractionResult> = _results.asSharedFlow()

    /**
     * Marks the extraction of [bookId] as started with indeterminate progress.
     */
    fun start(bookId: Int) {
        _state.value = CharacterExtractionState(bookId = bookId, isRunning = true)
    }

    /**
     * Updates the progress of the running extraction of [bookId],
     * ignored when another extraction is current.
     */
    fun updateProgress(bookId: Int, progress: Int?) {
        _state.update { current ->
            if (current.bookId == bookId && current.isRunning) current.copy(progress = progress)
            else current
        }
    }

    /**
     * Clears the running state and publishes the [result] of [bookId]'s extraction.
     */
    fun finish(bookId: Int, result: Result<List<BookCharacter>>) {
        _state.value = CharacterExtractionState()
        result.fold(
            onSuccess = { characters ->
                _results.tryEmit(CharacterExtractionResult.Success(bookId, characters))
            },
            onFailure = { throwable ->
                _results.tryEmit(
                    CharacterExtractionResult.Failure(
                        bookId = bookId,
                        message = throwable.message ?: "Couldn't generate characters catalog."
                    )
                )
            }
        )
    }
}
