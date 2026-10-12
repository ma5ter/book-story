/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.settings.reader.ai.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.mya.openrouter.ImageProxyProtocol
import ua.acclorite.book_story.R
import ua.acclorite.book_story.ui.common.components.settings.ChipsWithTitle
import ua.acclorite.book_story.ui.common.helpers.LocalSettings
import ua.acclorite.book_story.ui.common.model.ListItem
import ua.acclorite.book_story.ui.theme.ExpandingTransition

@Composable
fun ImageGenerationProxyProtocolOption() {
    val settings = LocalSettings.current

    ExpandingTransition(visible = settings.imageGenerationProxyEnabled.value) {
        ChipsWithTitle(
            title = stringResource(id = R.string.proxy_protocol_option),
            chips = ImageProxyProtocol.entries.map { item ->
                ListItem(
                    item = item,
                    title = when (item) {
                        ImageProxyProtocol.HTTP -> "HTTP"
                        ImageProxyProtocol.SOCKS5 -> "SOCKS5"
                    },
                    selected = item == settings.imageGenerationProxyProtocol.value
                )
            },
            onClick = { item ->
                settings.imageGenerationProxyProtocol.update(item)
            }
        )
    }
}
