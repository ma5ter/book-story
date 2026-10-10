/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.settings.reader.ai_image.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ua.acclorite.book_story.R
import ua.acclorite.book_story.ui.common.components.settings.TextFieldWithTitle
import ua.acclorite.book_story.ui.common.helpers.LocalSettings

@Composable
fun ImageGenerationPromptOption() {
    val settings = LocalSettings.current

    TextFieldWithTitle(
        value = settings.imageGenerationPrompt.value,
        title = stringResource(id = R.string.image_prompt_option),
        label = stringResource(id = R.string.image_prompt_option_label),
        singleLine = false,
        onValueChange = {
            settings.imageGenerationPrompt.update(it)
        }
    )
}
