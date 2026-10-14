/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.book_info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.presentation.book_info.BookInfoEvent

@Composable
fun BookInfoLayoutActions(
    book: Book,
    showMoveDialog: (BookInfoEvent.OnShowMoveDialog) -> Unit,
    showDeleteDialog: (BookInfoEvent.OnShowDeleteDialog) -> Unit,
    exportWithImages: (BookInfoEvent.OnExportWithImages) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(MaterialTheme.shapes.large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        BookInfoLayoutActionsItem(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
            title = stringResource(id = R.string.move),
            icon = Icons.Default.MoveUp,
            onClick = {
                showMoveDialog(BookInfoEvent.OnShowMoveDialog)
            }
        )

        // Generated images can only be embedded back into an FB2 file.
        if (book.filePath.endsWith(".fb2", ignoreCase = true)) {
            BookInfoLayoutActionsItem(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(4.dp),
                title = stringResource(id = R.string.export_with_images),
                icon = Icons.Default.FileDownload,
                onClick = {
                    exportWithImages(BookInfoEvent.OnExportWithImages)
                }
            )
        }

        BookInfoLayoutActionsItem(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp),
            title = stringResource(id = R.string.delete),
            icon = Icons.Default.Delete,
            onClick = {
                showDeleteDialog(BookInfoEvent.OnShowDeleteDialog)
            }
        )
    }
}
