/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.settings.reader.ai.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.character.BookCharacter
import ua.acclorite.book_story.ui.common.components.common.LazyColumnWithScrollbar
import ua.acclorite.book_story.ui.common.components.common.StyledText
import java.io.File

/**
 * Character info dialog. Shows the character's portrait, name and the
 * full description text, which is scrollable when it exceeds the
 * dialog height. A placeholder icon is shown when no picture was loaded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterInfoDialog(
    character: BookCharacter,
    picture: File?,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(0.dp, 520.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.shapes.extraLarge
            )
            .padding(top = 24.dp, bottom = 12.dp),
        onDismissRequest = onDismiss
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (picture != null) {
                AsyncImage(
                    // The portrait file is overwritten in place on every save,
                    // so its last-modified time changes the request and its
                    // memory cache key, forcing the new image to be decoded.
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(picture)
                        .setParameter("version", picture.lastModified().toString())
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    contentDescription = character.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = character.name,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.padding(top = 16.dp))

            StyledText(
                text = character.name,
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.padding(top = 16.dp))

            LazyColumnWithScrollbar(modifier = Modifier.fillMaxWidth()) {
                item {
                    StyledText(
                        text = character.description.ifBlank {
                            stringResource(id = R.string.error_no_description)
                        },
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 8.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            TextButton(onClick = onDismiss) {
                StyledText(
                    text = stringResource(id = R.string.ok),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
