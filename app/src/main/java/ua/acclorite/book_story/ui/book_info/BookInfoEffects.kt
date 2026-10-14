/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.book_info

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.SharedFlow
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.library.Book
import ua.acclorite.book_story.presentation.book_info.BookInfoEffect
import ua.acclorite.book_story.presentation.history.HistoryScreen
import ua.acclorite.book_story.presentation.reader.ReaderScreen
import ua.acclorite.book_story.presentation.settings.LibrarySettingsScreen
import ua.acclorite.book_story.ui.common.helpers.showToast
import ua.acclorite.book_story.ui.common.helpers.LocalActivity
import ua.acclorite.book_story.ui.common.helpers.launchActivity
import ua.acclorite.book_story.ui.navigator.LocalNavigator

@Composable
fun BookInfoEffects(effects: SharedFlow<BookInfoEffect>, book: Book) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val activity = LocalActivity.current

    LaunchedEffect(effects, context, activity, navigator, book) {
        effects.collect { effect ->
            when (effect) {
                is BookInfoEffect.OnChangedCover -> {
                    context.getString(R.string.cover_image_changed)
                        .showToast(context = context)
                }

                is BookInfoEffect.OnErrorResetCover -> {
                    context.getString(R.string.error_could_not_reset_cover)
                        .showToast(context = context)
                }

                is BookInfoEffect.OnResetCover -> {
                    context.getString(R.string.cover_reset)
                        .showToast(context = context)
                }

                is BookInfoEffect.OnDeletedCover -> {
                    context.getString(R.string.cover_image_deleted)
                        .showToast(context = context)
                }

                is BookInfoEffect.OnTitleChanged -> {
                    context.getString(R.string.title_changed)
                        .showToast(context = context)
                }

                is BookInfoEffect.OnAuthorChanged -> {
                    context.getString(R.string.author_changed)
                        .showToast(context = context)
                }

                is BookInfoEffect.OnDescriptionChanged -> {
                    context.getString(R.string.description_changed)
                        .showToast(context = context)
                }

                is BookInfoEffect.OnPathChanged -> {
                    context.getString(R.string.path_changed)
                        .showToast(context = context)
                }

                is BookInfoEffect.OnBookDeleted -> {
                    context.getString(R.string.book_deleted)
                        .showToast(context = context)
                }

                is BookInfoEffect.OnBookMoved -> {
                    context.getString(R.string.book_moved)
                        .showToast(context = context)
                }

                is BookInfoEffect.OnShareExportedBook -> {
                    val uri = FileProvider.getUriForFile(
                        activity,
                        "${activity.packageName}.file_provider",
                        effect.file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/xml"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, book.title)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    shareIntent.launchActivity(
                        activity = activity,
                        createChooser = true,
                        success = {
                            return@collect
                        }
                    )

                    activity.getString(R.string.error_no_share_app)
                        .showToast(context = activity, longToast = false)
                }

                is BookInfoEffect.OnErrorExport -> {
                    activity.getString(R.string.error_could_not_export)
                        .showToast(context = activity, longToast = false)
                }

                is BookInfoEffect.OnNavigateBack -> {
                    navigator.pop()
                }

                is BookInfoEffect.OnNavigateToLibrarySettings -> {
                    navigator.push(LibrarySettingsScreen)
                }

                is BookInfoEffect.OnNavigateToReader -> {
                    if (book.id != -1) {
                        HistoryScreen.insertHistoryChannel.trySend(book.id)
                        navigator.push(ReaderScreen(book.id))
                    }
                }
            }
        }
    }
}