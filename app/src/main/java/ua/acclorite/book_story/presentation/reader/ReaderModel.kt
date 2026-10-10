/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.R
import ua.acclorite.book_story.core.helpers.coerceAndPreventNaN
import ua.acclorite.book_story.core.ui.UIText
import ua.acclorite.book_story.domain.model.attachment.ImageAttachment
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.domain.model.reader.ReaderText.Chapter
import ua.acclorite.book_story.domain.use_case.attachment.CreateImageAttachmentUseCase
import ua.acclorite.book_story.domain.use_case.attachment.DeleteImageAttachmentUseCase
import ua.acclorite.book_story.domain.use_case.attachment.GenerateImageForAttachmentUseCase
import ua.acclorite.book_story.domain.use_case.attachment.GetImageAttachmentImageUseCase
import ua.acclorite.book_story.domain.use_case.attachment.GetImageAttachmentsUseCase
import ua.acclorite.book_story.domain.use_case.book.GetBookUseCase
import ua.acclorite.book_story.domain.use_case.book.GetChapterProgressUseCase
import ua.acclorite.book_story.domain.use_case.book.GetTextUseCase
import ua.acclorite.book_story.domain.use_case.book.UpdateBookUseCase
import ua.acclorite.book_story.domain.use_case.book.UpdateCoverImageUseCase
import ua.acclorite.book_story.domain.use_case.history.GetHistoryForBookUseCase
import ua.acclorite.book_story.presentation.history.HistoryScreen
import ua.acclorite.book_story.presentation.library.LibraryScreen
import ua.acclorite.book_story.presentation.reader.model.Checkpoint
import javax.inject.Inject
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

@HiltViewModel
class ReaderModel @Inject constructor(
    private val updateBookUseCase: UpdateBookUseCase,
    private val getTextUseCase: GetTextUseCase,
    private val getBookUseCase: GetBookUseCase,
    private val getHistoryForBookUseCase: GetHistoryForBookUseCase,
    private val getChapterProgressUseCase: GetChapterProgressUseCase,
    private val createImageAttachmentUseCase: CreateImageAttachmentUseCase,
    private val generateImageForAttachmentUseCase: GenerateImageForAttachmentUseCase,
    private val getImageAttachmentsUseCase: GetImageAttachmentsUseCase,
    private val deleteImageAttachmentUseCase: DeleteImageAttachmentUseCase,
    private val getImageAttachmentImageUseCase: GetImageAttachmentImageUseCase,
    private val updateCoverImageUseCase: UpdateCoverImageUseCase
) : ViewModel() {

    private val mutex = Mutex()

    private val _state = MutableStateFlow(ReaderState())
    val state = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ReaderEffect>()
    val effects = _effects.asSharedFlow()

    private var scrollJob: Job? = null
    private val eventStack = mutableListOf<Job>()

    fun onEvent(event: ReaderEvent) {
        viewModelScope.launch {
            when (event) {
                is ReaderEvent.OnLoadText -> {
                    withContext(Dispatchers.Default) {
                        val text = getTextUseCase(_state.value.book.id)
                        ensureActive()

                        if (text.isEmpty()) {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = UIText.StringResource(
                                        resId = R.string.error_could_not_get_text
                                    )
                                )
                            }
                            _effects.emit(ReaderEffect.OnSystemBarsVisibility(show = true))
                            return@withContext
                        }

                        _effects.emit(ReaderEffect.OnSystemBarsVisibility(show = null))

                        val lastOpened = getHistoryForBookUseCase(_state.value.book.id)?.time
                        val imageAttachments = getImageAttachmentsUseCase(_state.value.book.id)
                        _state.update {
                            it.copy(
                                showMenu = false,
                                book = it.book.copy(
                                    lastOpened = lastOpened
                                ),
                                text = text,
                                imageAttachments = imageAttachments
                            )
                        }
                        ensureActive()

                        updateBookUseCase(_state.value.book)

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)

                        onEvent(ReaderEvent.OnRestoreScroll)
                    }
                }

                is ReaderEvent.OnRestoreScroll -> {
                    snapshotFlow { _state.value.listState.layoutInfo.totalItemsCount }.first { it > 0 }

                    _state.value.listState.requestScrollToItem(
                        index = _state.value.book.scrollIndex,
                        scrollOffset = _state.value.book.scrollOffset
                    )

                    _state.update {
                        val (currentChapter, currentChapterProgress) = getChapterProgressUseCase(
                            it.book.scrollIndex,
                            it.text
                        )
                        it.copy(
                            currentChapter = currentChapter,
                            currentChapterProgress = currentChapterProgress,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }

                is ReaderEvent.OnMenuVisibility -> {
                    withContext(Dispatchers.Default) {
                        if (_state.value.lockMenu) return@withContext

                        _effects.emit(
                            ReaderEffect.OnSystemBarsVisibility(
                                show = if (event.show) true
                                else null
                            )
                        )

                        if (event.saveCheckpoint && event.show) {
                            val checkpoints = _state.value.checkpoints.toMutableList()
                            checkpoints.removeIf {
                                it.index == _state.value.listState.firstVisibleItemIndex
                            }
                            checkpoints.add(
                                Checkpoint(
                                    _state.value.listState.firstVisibleItemIndex,
                                    _state.value.listState.firstVisibleItemScrollOffset
                                )
                            )

                            _state.update {
                                it.copy(checkpoints = checkpoints)
                            }
                        }

                        _state.update {
                            it.copy(showMenu = event.show)
                        }
                    }
                }

                is ReaderEvent.OnChangeProgress -> {
                    withContext(Dispatchers.Default) {
                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    progress = event.progress,
                                    scrollIndex = event.firstVisibleItemIndex,
                                    scrollOffset = event.firstVisibleItemOffset
                                )
                            )
                        }

                        updateBookUseCase(_state.value.book)

                        LibraryScreen.refreshListChannel.trySend(300)
                        HistoryScreen.refreshListChannel.trySend(300)
                    }
                }

                is ReaderEvent.OnUpdateChapter -> {
                    _state.update {
                        val (currentChapter, currentChapterProgress) = getChapterProgressUseCase(
                            event.index,
                            _state.value.text
                        )
                        it.copy(
                            currentChapter = currentChapter,
                            currentChapterProgress = currentChapterProgress
                        )
                    }
                }

                is ReaderEvent.OnScrollToChapter -> {
                    withContext(Dispatchers.Default) {
                        val chapterIndex = _state.value.text
                            .indexOf(event.chapter)
                            .takeIf { it != -1 }
                        if (chapterIndex == null) return@withContext

                        _state.value.listState.requestScrollToItem(
                            index = chapterIndex,
                            scrollOffset = 0
                        )

                        onEvent(ReaderEvent.OnUpdateChapter(chapterIndex))
                        onEvent(
                            ReaderEvent.OnChangeProgress(
                                progress = calculateProgress(chapterIndex),
                                firstVisibleItemIndex = chapterIndex,
                                firstVisibleItemOffset = 0
                            )
                        )
                    }
                }

                is ReaderEvent.OnScroll -> {
                    scrollJob?.cancel()
                    scrollJob = viewModelScope.launch(Dispatchers.IO) {
                        delay(300)

                        val scrollTo = (_state.value.text.lastIndex * event.progress).roundToInt()

                        _state.value.listState.requestScrollToItem(
                            index = scrollTo,
                            scrollOffset = 0
                        )

                        onEvent(ReaderEvent.OnUpdateChapter(scrollTo))
                    }
                }

                is ReaderEvent.OnRestoreCheckpoint -> {
                    withContext(Dispatchers.Default) {
                        _state.update {
                            val checkpoints = it.checkpoints.toMutableList()
                            if (checkpoints.size > 1) checkpoints.remove(event.checkpoint)

                            it.copy(
                                checkpoints = checkpoints
                            )
                        }

                        _state.value.listState.requestScrollToItem(
                            index = event.checkpoint.index,
                            scrollOffset = event.checkpoint.offset
                        )

                        onEvent(ReaderEvent.OnUpdateChapter(event.checkpoint.index))
                        onEvent(
                            ReaderEvent.OnChangeProgress(
                                progress = calculateProgress(event.checkpoint.index),
                                firstVisibleItemIndex = event.checkpoint.index,
                                firstVisibleItemOffset = event.checkpoint.offset,
                            )
                        )
                    }
                }

                is ReaderEvent.OnLeave -> {
                    _state.update {
                        it.copy(
                            lockMenu = true
                        )
                    }

                    if (
                        !_state.value.isLoading &&
                        _state.value.listState.layoutInfo.totalItemsCount > 0 &&
                        _state.value.text.isNotEmpty() &&
                        _state.value.errorMessage != null
                    ) {
                        _state.update {
                            it.copy(
                                book = it.book.copy(
                                    progress = calculateProgress(),
                                    scrollIndex = _state.value.listState.firstVisibleItemIndex,
                                    scrollOffset = _state.value.listState.firstVisibleItemScrollOffset
                                )
                            )
                        }

                        updateBookUseCase(_state.value.book)

                        LibraryScreen.refreshListChannel.trySend(0)
                        HistoryScreen.refreshListChannel.trySend(0)
                    }

                    _effects.emit(
                        ReaderEffect.OnSystemBarsVisibility(
                            show = true
                        )
                    )
                    _effects.emit(ReaderEffect.OnResetBrightness)
                    event.navigate()
                }

                is ReaderEvent.OnOpenTranslator -> {
                    _effects.emit(
                        ReaderEffect.OnOpenTranslator(
                            textToTranslate = event.textToTranslate,
                            translateWholeParagraph = event.translateWholeParagraph
                        )
                    )
                }

                is ReaderEvent.OnOpenShareApp -> {
                    _effects.emit(
                        ReaderEffect.OnOpenShareApp(
                            textToShare = event.textToShare
                        )
                    )
                }

                is ReaderEvent.OnOpenWebBrowser -> {
                    _effects.emit(
                        ReaderEffect.OnOpenWebBrowser(
                            textToSearch = event.textToSearch
                        )
                    )
                }

                is ReaderEvent.OnOpenDictionary -> {
                    _effects.emit(
                        ReaderEffect.OnOpenDictionary(
                            textToDefine = event.textToDefine
                        )
                    )
                }

                is ReaderEvent.OnGenerateImage -> {
                    withContext(Dispatchers.Default) {
                        val bookId = _state.value.book.id
                        if (bookId == -1) return@withContext

                        val selectedParagraphIndex = findSelectedParagraphIndex(
                            selectedText = event.textToIllustrate,
                            text = _state.value.text,
                            visibleItemIndex = event.paragraphIndex
                        )
                        val paragraphIndex = toBaseParagraphIndex(
                            displayedIndex = selectedParagraphIndex,
                            attachments = _state.value.imageAttachments
                        )

                        createImageAttachmentUseCase(
                            bookId = bookId,
                            paragraphIndex = paragraphIndex
                        ).fold(
                            onSuccess = { attachment ->
                                _state.update { state ->
                                    state.copy(
                                        text = state.text.toMutableList().apply {
                                            add(
                                                (selectedParagraphIndex + 1)
                                                    .coerceIn(0, state.text.size),
                                                ReaderText.PendingImage(attachment.id)
                                            )
                                        },
                                        imageAttachments = state.imageAttachments + attachment
                                    )
                                }

                                generateImageForAttachmentUseCase(
                                    attachmentId = attachment.id,
                                    selectedText = event.textToIllustrate
                                ).fold(
                                    onSuccess = { (completedAttachment, bitmap) ->
                                        _state.update { state ->
                                            state.copy(
                                                text = state.text.map { entry ->
                                                    if (
                                                        entry is ReaderText.PendingImage &&
                                                        entry.attachmentId == completedAttachment.id
                                                    ) {
                                                        ReaderText.Image(
                                                            imageBitmap = bitmap.asImageBitmap(),
                                                            generated = true,
                                                            attachmentId = completedAttachment.id
                                                        )
                                                    } else {
                                                        entry
                                                    }
                                                },
                                                imageAttachments = state.imageAttachments.map {
                                                    if (it.id == completedAttachment.id) {
                                                        completedAttachment
                                                    } else {
                                                        it
                                                    }
                                                }
                                            )
                                        }
                                    },
                                    onFailure = {
                                        onImageGenerationFailed(
                                            attachmentId = attachment.id,
                                            message = it.message ?: "Image generation failed."
                                        )
                                    }
                                )
                            },
                            onFailure = {
                                _effects.emit(
                                    ReaderEffect.OnShowToast(
                                        message = it.message
                                            ?: "Could not create image attachment."
                                    )
                                )
                            }
                        )
                    }
                }

                is ReaderEvent.OnShareImage -> {
                    val file = _state.value.imageAttachments
                        .find { it.id == event.attachmentId }
                        ?.filePath?.takeIf { it.isNotBlank() }
                        ?.let(::File)
                        ?.takeIf { it.exists() }

                    if (file != null) {
                        _effects.emit(ReaderEffect.OnShareImage(file))
                    }
                }

                is ReaderEvent.OnDeleteImage -> {
                    deleteImageAttachmentUseCase(event.attachmentId).fold(
                        onSuccess = {
                            _state.update { state ->
                                state.copy(
                                    text = state.text.filterNot { entry ->
                                        entry is ReaderText.Image &&
                                                entry.attachmentId == event.attachmentId
                                    },
                                    imageAttachments = state.imageAttachments.filterNot {
                                        it.id == event.attachmentId
                                    }
                                )
                            }
                        },
                        onFailure = {
                            _effects.emit(
                                ReaderEffect.OnShowToast(
                                    message = it.message ?: "Could not delete image."
                                )
                            )
                        }
                    )
                }

                is ReaderEvent.OnSetCoverImage -> {
                    withContext(Dispatchers.Default) {
                        val cover = getImageAttachmentImageUseCase(event.attachmentId)
                        if (cover == null) {
                            _effects.emit(
                                ReaderEffect.OnShowToast(
                                    message = "Couldn't set image as book cover."
                                )
                            )
                            return@withContext
                        }

                        updateCoverImageUseCase(_state.value.book.id, cover)
                            .fold(
                                onSuccess = {
                                    LibraryScreen.refreshListChannel.trySend(0)
                                    HistoryScreen.refreshListChannel.trySend(0)

                                    _effects.emit(ReaderEffect.OnCoverImageSet)
                                },
                                onFailure = {
                                    _effects.emit(
                                        ReaderEffect.OnShowToast(
                                            message = it.message
                                                ?: "Couldn't set image as book cover."
                                        )
                                    )
                                }
                            )
                    }
                }

                is ReaderEvent.OnShowSettingsBottomSheet -> {
                    _state.update {
                        it.copy(
                            bottomSheet = ReaderScreen.SETTINGS_BOTTOM_SHEET,
                            drawer = null
                        )
                    }
                }

                is ReaderEvent.OnDismissBottomSheet -> {
                    _state.update {
                        it.copy(
                            bottomSheet = null
                        )
                    }
                }

                is ReaderEvent.OnShowChaptersDrawer -> {
                    _state.update {
                        it.copy(
                            drawer = ReaderScreen.CHAPTERS_DRAWER,
                            bottomSheet = null
                        )
                    }
                }

                is ReaderEvent.OnDismissDrawer -> {
                    _state.update {
                        it.copy(
                            drawer = null
                        )
                    }
                }

                is ReaderEvent.OnNavigateBack -> {
                    _effects.emit(ReaderEffect.OnNavigateBack)
                }

                is ReaderEvent.OnNavigateToBookInfo -> {
                    _effects.emit(ReaderEffect.OnNavigateToBookInfo(event.changePath))
                }
            }
        }.also { eventStack.add(it) }
    }

    fun init(bookId: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val book = getBookUseCase(bookId)

            if (book == null) {
                _effects.emit(ReaderEffect.OnNavigateBack)
                return@launch
            }

            clearState()

            _state.update {
                ReaderState(
                    book = book
                )
            }

            onEvent(ReaderEvent.OnLoadText)
        }
    }

    fun clearAsync() {
        viewModelScope.launch {
            eventStack.forEach { job ->
                job.cancel()
            }
            _state.update { ReaderState() }
        }
    }

    suspend fun clearState() {
        eventStack.forEach { job ->
            job.cancel()
            job.join()
        }
        eventStack.clear()
        _state.update { ReaderState() }
    }

    @OptIn(FlowPreview::class)
    suspend fun updateProgress(listState: LazyListState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.distinctUntilChanged().debounce(300).collectLatest { (index, offset) ->
            if (
                _state.value.isLoading ||
                listState.layoutInfo.totalItemsCount == 0 ||
                _state.value.text.isEmpty() ||
                _state.value.errorMessage != null
            ) return@collectLatest

            val progress = calculateProgress(index)
            val (currentChapter, currentChapterProgress) = getChapterProgressUseCase(
                index = index,
                text = _state.value.text
            )

            _state.update {
                it.copy(
                    book = it.book.copy(
                        progress = progress,
                        scrollIndex = index,
                        scrollOffset = offset
                    ),
                    currentChapter = currentChapter,
                    currentChapterProgress = currentChapterProgress
                )
            }

            updateBookUseCase(_state.value.book)

            LibraryScreen.refreshListChannel.trySend(0)
            HistoryScreen.refreshListChannel.trySend(0)
        }
    }

    fun findChapterIndexAndLength(index: Int): Pair<Int, Int> {
        val (chapter, _) = getChapterProgressUseCase(index = index, text = _state.value.text)
        return chapter?.let { chapter ->
            val startIndex = _state.value.text
                .indexOf(chapter)
                .coerceIn(0, _state.value.text.lastIndex)
            val endIndex = (_state.value.text.indexOfFirst {
                it is Chapter && _state.value.text.indexOf(it) > startIndex
            }.takeIf { it != -1 }) ?: (_state.value.text.lastIndex + 1)

            val currentIndexInChapter = (index - startIndex).coerceAtLeast(1)
            val chapterLength = endIndex - (startIndex + 1)
            currentIndexInChapter to chapterLength
        } ?: (-1 to -1)
    }

    /**
     * Converts an index of the displayed reader text (with injected images)
     * into an index of the underlying parsed text (without injected images).
     */
    private fun toBaseParagraphIndex(
        displayedIndex: Int,
        attachments: List<ImageAttachment>
    ): Int {
        if (attachments.isEmpty()) return displayedIndex

        var baseIndex = displayedIndex
        attachments.sortedBy { it.paragraphIndex }.forEachIndexed { order, attachment ->
            val attachmentDisplayIndex = attachment.paragraphIndex + 1 + order
            if (attachmentDisplayIndex > displayedIndex) return baseIndex.coerceAtLeast(0)
            baseIndex--
        }
        return baseIndex.coerceAtLeast(0)
    }

    /**
     * Removes a failed image attachment placeholder from the reader text
     * and reports the failure to the user.
     */
    private suspend fun onImageGenerationFailed(attachmentId: Int, message: String) {
        _state.update { state ->
            state.copy(
                text = state.text.filterNot { entry ->
                    entry is ReaderText.PendingImage && entry.attachmentId == attachmentId
                },
                imageAttachments = state.imageAttachments.filterNot { it.id == attachmentId }
            )
        }
        _effects.emit(ReaderEffect.OnShowToast(message))
    }

    private fun calculateProgress(firstVisibleItemIndex: Int? = null): Float {
        if (
            _state.value.isLoading ||
            _state.value.listState.layoutInfo.totalItemsCount == 0 ||
            _state.value.text.isEmpty() ||
            _state.value.errorMessage != null
        ) return _state.value.book.progress

        if ((firstVisibleItemIndex ?: _state.value.listState.firstVisibleItemIndex) == 0) return 0f

        val lastVisibleItemIndex = _state.value.listState.layoutInfo.visibleItemsInfo.last().index
        if (lastVisibleItemIndex >= _state.value.text.lastIndex) return 1f

        return (firstVisibleItemIndex ?: _state.value.listState.firstVisibleItemIndex)
            .div(_state.value.text.lastIndex.toFloat())
            .coerceAndPreventNaN()
    }

    private suspend inline fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
        mutex.withLock {
            coroutineContext.ensureActive()
            this.value = function(this.value)
        }
    }
}