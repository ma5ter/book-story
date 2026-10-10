/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package ua.acclorite.book_story.ui.settings.reader.ai_image

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import ua.acclorite.book_story.R
import ua.acclorite.book_story.ui.settings.components.SettingsSubcategory
import ua.acclorite.book_story.ui.settings.reader.ai_image.components.ImageGenerationApiKeyOption
import ua.acclorite.book_story.ui.settings.reader.ai_image.components.ImageGenerationModelOption
import ua.acclorite.book_story.ui.settings.reader.ai_image.components.ImageGenerationPromptOption
import ua.acclorite.book_story.ui.settings.reader.ai_image.components.ImageGenerationProxyHostOption
import ua.acclorite.book_story.ui.settings.reader.ai_image.components.ImageGenerationProxyOption
import ua.acclorite.book_story.ui.settings.reader.ai_image.components.ImageGenerationProxyPortOption
import ua.acclorite.book_story.ui.settings.reader.ai_image.components.ImageGenerationProxyProtocolOption

fun LazyListScope.AiImageSubcategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary },
    title: @Composable () -> String = { stringResource(id = R.string.ai_image_reader_settings) },
    showTitle: Boolean = true,
    showDivider: Boolean = true
) {
    SettingsSubcategory(
        titleColor = titleColor,
        title = title,
        showTitle = showTitle,
        showDivider = showDivider
    ) {
        item {
            ImageGenerationApiKeyOption()
        }

        item {
            ImageGenerationModelOption()
        }

        item {
            ImageGenerationPromptOption()
        }

        item {
            ImageGenerationProxyOption()
        }

        item {
            ImageGenerationProxyProtocolOption()
        }

        item {
            ImageGenerationProxyHostOption()
        }

        item {
            ImageGenerationProxyPortOption()
        }
    }
}
