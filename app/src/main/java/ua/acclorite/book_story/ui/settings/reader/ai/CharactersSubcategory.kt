/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package ua.acclorite.book_story.ui.settings.reader.ai

import android.content.ClipboardManager
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.character.BookCharacter
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.components.progress_indicator.CircularProgressIndicator
import ua.acclorite.book_story.ui.common.helpers.noRippleClickable
import ua.acclorite.book_story.ui.settings.components.SettingsSubcategory
import java.io.File

/**
 * Characters group of the AI tab in the reader settings bottom sheet.
 * Lists the persisted characters catalog with their cached portraits and
 * provides the action to analyze the book content and (re)build the catalog.
 * Long-pressing an entry opens a popup menu with the description editing
 * and the portrait loading (via the system image picker).
 */
fun LazyListScope.CharactersSubcategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary },
    characters: List<BookCharacter>,
    characterImages: Map<Int, File>,
    isGenerating: Boolean,
    generate: (ReaderEvent.OnGenerateCharacters) -> Unit,
    onEdit: (BookCharacter) -> Unit,
    onLoadPicture: (BookCharacter) -> Unit,
    onPaste: (BookCharacter) -> Unit,
    onOpen: (BookCharacter) -> Unit
) {
    SettingsSubcategory(
        titleColor = titleColor,
        title = { stringResource(id = R.string.characters_reader_settings) },
        showTitle = true,
        showDivider = false
    ) {
        item {
            val context = LocalContext.current
            val prompt = remember(context) {
                context.resources
                    .openRawResource(R.raw.characters_prompt)
                    .bufferedReader().use { it.readText() }
                    .trim()
            }
            val startExtraction = {
                generate(ReaderEvent.OnGenerateCharacters(systemPrompt = prompt))
            }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) startExtraction()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isGenerating) Modifier
                        else Modifier.noRippleClickable {
                            if (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            } else {
                                startExtraction()
                            }
                        }
                    )
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                    StyledText(
                        text = stringResource(id = R.string.characters_generating),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                } else {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Rounded.PersonSearch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    StyledText(
                        text = stringResource(id = R.string.analyze_characters),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        }

        if (characters.isEmpty() && !isGenerating) {
            item {
                StyledText(
                    text = stringResource(id = R.string.characters_empty),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        items(characters, key = { it.id }) { character ->
            val menuExpanded = remember { mutableStateOf(false) }
            val portrait = characterImages[character.id]
            val context = LocalContext.current
            // The Paste action is enabled only when the clipboard holds
            // an image; the check is re-evaluated every time the menu
            // pops up, in case the clipboard changed between pop-ups.
            val clipboardHasImage = remember(menuExpanded.value) {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .primaryClipDescription?.hasMimeType("image/*") == true
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .noRippleClickable(
                        onClick = { onOpen(character) },
                        onLongClick = { menuExpanded.value = true }
                    )
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (portrait != null) {
                    AsyncImage(
                        // The portrait file is overwritten in place on every save,
                        // so its last-modified time changes the request and its
                        // memory cache key, forcing the new image to be decoded.
                        model = ImageRequest.Builder(context)
                            .data(portrait)
                            .setParameter("version", portrait.lastModified().toString())
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = character.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    StyledText(
                        text = character.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StyledText(
                        text = character.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 6
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded.value,
                    onDismissRequest = { menuExpanded.value = false }
                ) {
                    DropdownMenuItem(
                        text = { StyledText(text = stringResource(id = R.string.edit_description)) },
                        onClick = {
                            menuExpanded.value = false
                            onEdit(character)
                        }
                    )
                    DropdownMenuItem(
                        text = { StyledText(text = stringResource(id = R.string.load_picture)) },
                        onClick = {
                            menuExpanded.value = false
                            onLoadPicture(character)
                        }
                    )
                    DropdownMenuItem(
                        text = { StyledText(text = stringResource(id = R.string.paste)) },
                        enabled = clipboardHasImage,
                        onClick = {
                            menuExpanded.value = false
                            onPaste(character)
                        }
                    )
                }
            }
        }
    }
}
