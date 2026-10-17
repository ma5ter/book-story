/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.settings.reader.ai.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ua.acclorite.book_story.R
import ua.acclorite.book_story.ui.common.components.settings.TextFieldWithTitle
import ua.acclorite.book_story.ui.common.helpers.LocalSettings

@Composable
fun ImageGenerationModelOption() {
    val settings = LocalSettings.current

    TextFieldWithTitle(
        value = settings.imageGenerationModel.value,
        title = stringResource(id = R.string.image_model_option),
        label = stringResource(id = R.string.image_model_option_label),
        onValueChange = {
            settings.imageGenerationModel.update(it)
        }
    )
}
