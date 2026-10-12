/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.settings.reader.ai.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import ua.acclorite.book_story.R
import ua.acclorite.book_story.ui.common.components.settings.TextFieldWithTitle
import ua.acclorite.book_story.ui.common.helpers.LocalSettings
import ua.acclorite.book_story.ui.theme.ExpandingTransition

@Composable
fun ImageGenerationProxyPortOption() {
    val settings = LocalSettings.current

    ExpandingTransition(visible = settings.imageGenerationProxyEnabled.value) {
        TextFieldWithTitle(
            value = settings.imageGenerationProxyPort.value,
            title = stringResource(id = R.string.proxy_port_option),
            label = stringResource(id = R.string.proxy_port_option_label),
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Number
            ),
            onValueChange = {
                settings.imageGenerationProxyPort.update(it)
            }
        )
    }
}
