/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import android.net.Uri
import androidx.compose.runtime.Immutable
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.domain.model.character.BookCharacter
import ua.acclorite.book_story.domain.model.reader.ReaderText.Chapter
import ua.acclorite.book_story.presentation.reader.model.Checkpoint

@Immutable
sealed class ReaderEvent {

    data object OnLoadText : ReaderEvent()

    data object OnRestoreScroll : ReaderEvent()

    data class OnMenuVisibility(
        val show: Boolean,
        val saveCheckpoint: Boolean
    ) : ReaderEvent()

    data class OnChangeProgress(
        val progress: Float,
        val firstVisibleItemIndex: Int,
        val firstVisibleItemOffset: Int
    ) : ReaderEvent()

    data class OnUpdateChapter(
        val index: Int
    ) : ReaderEvent()

    data class OnScrollToChapter(
        val chapter: Chapter
    ) : ReaderEvent()

    data class OnScrollToImage(
        val attachmentId: Int
    ) : ReaderEvent()

    data class OnScroll(
        val progress: Float
    ) : ReaderEvent()

    data class OnRestoreCheckpoint(
        val checkpoint: Checkpoint
    ) : ReaderEvent()

    data class OnLeave(
        val navigate: () -> Unit
    ) : ReaderEvent()

    data class OnOpenTranslator(
        val textToTranslate: String,
        val translateWholeParagraph: Boolean
    ) : ReaderEvent()

    data class OnOpenShareApp(
        val textToShare: String
    ) : ReaderEvent()

    data class OnOpenWebBrowser(
        val textToSearch: String
    ) : ReaderEvent()

    data class OnOpenDictionary(
        val textToDefine: String
    ) : ReaderEvent()

    data class OnGenerateImage(
        val textToIllustrate: String,
        val paragraphIndex: Int
    ) : ReaderEvent()

    /**
     * Analyzes the whole book content and rebuilds the characters catalog.
     * [systemPrompt] is the localized analysis prompt from resources.
     */
    data class OnGenerateCharacters(
        val systemPrompt: String
    ) : ReaderEvent()

    /**
     * Persists an edited characters catalog entry.
     */
    data class OnEditCharacter(
        val character: BookCharacter
    ) : ReaderEvent()

    /**
     * Imports the [image] picked by the user and caches it as the
     * portrait of the character with [characterId].
     */
    data class OnLoadCharacterImage(
        val characterId: Int,
        val image: Uri
    ) : ReaderEvent()

    /**
     * Pastes an image from the clipboard and caches it as the
     * portrait of the character with [characterId].
     */
    data class OnPasteCharacterImage(
        val characterId: Int
    ) : ReaderEvent()

    /**
     * Accepts the adjusted portrait of the character with [characterId].
     * [image] is the square portrait rendered from the crop viewport.
     */
    data class OnAcceptCharacterPortrait(
        val characterId: Int,
        val image: CoverImage
    ) : ReaderEvent()

    /**
     * Discards the pending portrait addition/change of a character.
     */
    data object OnCancelCharacterPortrait : ReaderEvent()

    data class OnShareImage(
        val attachmentId: Int
    ) : ReaderEvent()

    data class OnCopyImage(
        val attachmentId: Int
    ) : ReaderEvent()

    data class OnDeleteImage(
        val attachmentId: Int
    ) : ReaderEvent()

    data class OnSetCoverImage(
        val attachmentId: Int
    ) : ReaderEvent()

    data object OnShowSettingsBottomSheet : ReaderEvent()

    data object OnDismissBottomSheet : ReaderEvent()

    data object OnShowChaptersDrawer : ReaderEvent()

    data object OnDismissDrawer : ReaderEvent()

    data object OnNavigateBack : ReaderEvent()

    data class OnNavigateToBookInfo(
        val changePath: Boolean
    ) : ReaderEvent()
}