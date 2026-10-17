/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.settings.reader.ai.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ua.acclorite.book_story.R
import ua.acclorite.book_story.ui.common.components.settings.SwitchWithTitle
import ua.acclorite.book_story.ui.common.helpers.LocalSettings

/**
 * Switch controlling whether a collage of the book's existing
 * illustrations is attached to the image generation request
 * as a style reference.
 */
@Composable
fun ImageGenerationStyleReferenceOption() {
    val settings = LocalSettings.current

    SwitchWithTitle(
        selected = settings.imageGenerationStyleReferenceEnabled.value,
        title = stringResource(id = R.string.style_reference_option),
        description = stringResource(id = R.string.style_reference_option_desc),
        onClick = {
            settings.imageGenerationStyleReferenceEnabled
                .update(!settings.imageGenerationStyleReferenceEnabled.lastValue)
        }
    )
}
