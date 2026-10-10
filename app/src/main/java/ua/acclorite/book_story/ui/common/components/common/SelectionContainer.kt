/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("DEPRECATION")

package ua.acclorite.book_story.ui.common.components.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import ua.acclorite.book_story.R

private data object CopyContextMenuKey
private data object ShareContextMenuKey
private data object WebSearchContextMenuKey
private data object TranslateContextMenuKey
private data object DictionaryContextMenuKey
private data object ImageContextMenuKey

private val customTextContextMenuKeys = setOf(
    CopyContextMenuKey,
    ShareContextMenuKey,
    WebSearchContextMenuKey,
    TranslateContextMenuKey,
    DictionaryContextMenuKey,
    ImageContextMenuKey
)

/**
 * Selection container.
 *
 * @param onCopyRequested Callback for when the copy option is clicked.
 * @param onTranslateRequested Callback for when the translate option is clicked.
 * @param onDictionaryRequested Callback for when the dictionary option is clicked.
 * @param onImageRequested Callback for when the generate image option is clicked.
 */
@Composable
fun SelectionContainer(
    onCopyRequested: (() -> Unit),
    onShareRequested: ((String) -> Unit),
    onWebSearchRequested: ((String) -> Unit),
    onTranslateRequested: ((String) -> Unit),
    onDictionaryRequested: ((String) -> Unit),
    onImageRequested: ((String) -> Unit),
    content: @Composable (toolbarHidden: Boolean) -> Unit
) {
    val selectionState = rememberSelectionState()
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    /**
     * Joins selected fragments preserving boundaries between them:
     * a space is inserted only when neither side of the boundary
     * already contains whitespace.
     */
    fun selectedText(): String = selectionState.selectedTexts
        .fold(StringBuilder()) { builder, selectedText ->
            val previous = builder.lastOrNull()
            val next = selectedText.text.firstOrNull()
            if (
                builder.isNotEmpty() &&
                previous?.isWhitespace() != true &&
                next?.isWhitespace() != true
            ) {
                builder.append(' ')
            }
            builder.append(selectedText.text)
        }.toString()

    val isToolbarHidden = remember(selectionState) {
        derivedStateOf {
            selectionState.selectedTexts.isEmpty()
        }
    }

    SelectionContainer(
        state = selectionState,
        modifier = Modifier
            .appendTextContextMenuComponents {
                item(
                    key = CopyContextMenuKey,
                    label = context.getString(R.string.copy)
                ) {
                    val text = selectedText()
                    clipboardManager.setPrimaryClip(ClipData.newPlainText(null, text))
                    onCopyRequested()
                    selectionState.clear()
                    close()
                }
                item(
                    key = ShareContextMenuKey,
                    label = context.getString(R.string.share)
                ) {
                    onShareRequested(selectedText())
                    selectionState.clear()
                    close()
                }
                item(
                    key = WebSearchContextMenuKey,
                    label = context.getString(R.string.web_search)
                ) {
                    onWebSearchRequested(selectedText())
                    selectionState.clear()
                    close()
                }
                item(
                    key = TranslateContextMenuKey,
                    label = context.getString(R.string.translate)
                ) {
                    onTranslateRequested(selectedText())
                    selectionState.clear()
                    close()
                }
                item(
                    key = DictionaryContextMenuKey,
                    label = context.getString(R.string.dictionary)
                ) {
                    onDictionaryRequested(selectedText())
                    selectionState.clear()
                    close()
                }
                item(
                    key = ImageContextMenuKey,
                    label = context.getString(R.string.generate_image)
                ) {
                    onImageRequested(selectedText())
                    selectionState.clear()
                    close()
                }
            }
            .filterTextContextMenuComponents { it.key in customTextContextMenuKeys }
    ) {
        content(isToolbarHidden.value)
    }
}
