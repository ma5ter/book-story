/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

@file:Suppress("FunctionName")

package ua.acclorite.book_story.ui.settings.reader.ai

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import ua.acclorite.book_story.R
import ua.acclorite.book_story.ui.settings.components.SettingsSubcategory
import ua.acclorite.book_story.ui.settings.reader.ai.components.ImageGenerationApiKeyOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.TextAssistantMaxInputPriceOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.TextAssistantPreferredLatencyOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.CharacterMaxInputPriceOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.CharacterPreferredLatencyOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.ImageGenerationModelOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.ImageGenerationPromptOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.ImageGenerationProxyHostOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.ImageGenerationProxyOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.ImageGenerationProxyPortOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.ImageGenerationProxyProtocolOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.ImageGenerationStyleReferenceOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.SummarizeModelOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.TextMaxTokensOption
import ua.acclorite.book_story.ui.settings.reader.ai.components.TextAssistantModelOption

fun LazyListScope.AiSubcategory(
    titleColor: @Composable () -> Color = { MaterialTheme.colorScheme.primary },
    title: @Composable () -> String = { stringResource(id = R.string.ai_reader_settings) },
    showTitle: Boolean = true,
    showDivider: Boolean = true,
    // Whether to show the API key and proxy options (hidden in the reader bottom sheet).
    showConnection: Boolean = true
) {
    SettingsSubcategory(
        titleColor = titleColor,
        title = title,
        showTitle = showTitle,
        showDivider = showDivider
    ) {
        if (showConnection) {
            item {
                ImageGenerationApiKeyOption()
            }
        }

        item {
            TextAssistantModelOption()
        }

        item {
            TextAssistantPreferredLatencyOption()
        }

        item {
            TextAssistantMaxInputPriceOption()
        }

        item {
            SummarizeModelOption()
        }

        item {
            TextMaxTokensOption()
        }

        item {
            CharacterPreferredLatencyOption()
        }

        item {
            CharacterMaxInputPriceOption()
        }

        item {
            ImageGenerationModelOption()
        }

        item {
            ImageGenerationStyleReferenceOption()
        }

        item {
            ImageGenerationPromptOption()
        }

        if (showConnection) {
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
}
