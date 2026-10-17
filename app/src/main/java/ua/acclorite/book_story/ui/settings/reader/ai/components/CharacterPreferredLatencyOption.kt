/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
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

@Composable
fun CharacterPreferredLatencyOption() {
    val settings = LocalSettings.current

    TextFieldWithTitle(
        value = settings.characterPreferredLatency.value.toString(),
        title = stringResource(id = R.string.character_preferred_latency_option),
        label = stringResource(id = R.string.character_preferred_latency_option_label),
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Number
        ),
        onValueChange = { raw ->
            raw.filter(Char::isDigit).take(6).toIntOrNull()?.let { seconds ->
                settings.characterPreferredLatency.update(seconds)
            }
        }
    )
}
