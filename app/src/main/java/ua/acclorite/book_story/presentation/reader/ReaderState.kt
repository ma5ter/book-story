/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.reader

import android.net.Uri
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import ua.acclorite.book_story.core.BottomSheet
import ua.acclorite.book_story.core.Drawer
import ua.acclorite.book_story.core.ui.UIText
import ua.acclorite.book_story.domain.model.attachment.ImageAttachment
import ua.acclorite.book_story.domain.model.character.BookCharacter
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.domain.model.reader.ReaderText.Chapter
import ua.acclorite.book_story.presentation.reader.model.Checkpoint
import java.io.File

/**
 * Pending portrait adjustment of a character: the picked or pasted
 * image awaits zoom/pan adjustment in the round viewport dialog.
 */
@Immutable
data class CharacterPortraitCrop(
    val character: BookCharacter,
    val image: Uri
)

@Immutable
data class ReaderState(
    val book: Book = Book.default,
    val text: List<ReaderText> = emptyList(),
    val imageAttachments: List<ImageAttachment> = emptyList(),
    val characters: List<BookCharacter> = emptyList(),
    val characterImages: Map<Int, File> = emptyMap(),
    val portraitCrop: CharacterPortraitCrop? = null,
    val isGeneratingCharacters: Boolean = false,
    val listState: LazyListState = LazyListState(),

    val currentChapter: Chapter? = null,
    val currentChapterProgress: Float = 0f,

    val errorMessage: UIText? = null,
    val isLoading: Boolean = true,

    val showMenu: Boolean = false,
    val checkpoints: List<Checkpoint> = emptyList(),
    val lockMenu: Boolean = false,

    val bottomSheet: BottomSheet? = null,
    val drawer: Drawer? = null
)