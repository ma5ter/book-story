/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.presentation.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.R
import ua.acclorite.book_story.presentation.core.constants.provideAboutBadges
import ua.acclorite.book_story.presentation.core.util.showToast
import ua.acclorite.book_story.ui.about.AboutEvent

@Composable
fun AboutBadges(
    navigateToBrowserPage: (AboutEvent.OnNavigateToBrowserPage) -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        LazyRow(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            items(
                provideAboutBadges(),
                key = { it.id }
            ) { badge ->
                AboutBadgeItem(badge = badge) {
                    badge.url?.let {
                        navigateToBrowserPage(
                            AboutEvent.OnNavigateToBrowserPage(
                                page = it,
                                context = context
                            )
                        )
                    }
                }
            }
        }
    }
}