/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.common.components.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.settings.components.SettingsSubcategoryTitle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderWithTitle(
    modifier: Modifier = Modifier,
    value: Pair<Int, String>,
    valuePlaceholder: String = "",
    showPlaceholder: Boolean = false,
    fromValue: Int,
    toValue: Int,
    title: String,
    horizontalPadding: Dp = 18.dp,
    verticalPadding: Dp = 8.dp,
    onValueChange: (Int) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val placeholder = remember(showPlaceholder, value, valuePlaceholder) {
        derivedStateOf {
            if (!showPlaceholder) "${value.first}${value.second}"
            else valuePlaceholder
        }
    }
    val sliderState = rememberSliderState(
        value = value.first.toFloat(),
        steps = toValue - fromValue - 1,
        trackRange = fromValue.toFloat()..toValue.toFloat()
    )

    SideEffect {
        if (sliderState.value != value.first.toFloat()) {
            sliderState.value = value.first.toFloat()
        }
    }

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.fillMaxWidth(0.2f)) {
            SettingsSubcategoryTitle(title = title, padding = 0.dp)
            Spacer(modifier = Modifier.height(4.dp))
            StyledText(
                text = placeholder.value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Slider(
            state = sliderState,
            onValueChange = {
                onValueChange(it.roundToInt())
            },
            interactionSource = interactionSource,
            thumb = {
                Label(
                    label = {
                        PlainTooltip(
                            modifier = Modifier.clip(CircleShape)
                        ) {
                            StyledText(
                                text = placeholder.value,
                                modifier = Modifier.padding(
                                    vertical = 6.dp,
                                    horizontal = 4.dp
                                )
                            )
                        }
                    },
                    interactionSource = interactionSource
                ) {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource
                    )
                }
            },
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                thumbColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                activeTickColor = MaterialTheme.colorScheme.onSecondary,
                inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderWithTitle(
    modifier: Modifier = Modifier,
    value: Pair<Float, String>,
    valuePlaceholder: String = "",
    showPlaceholder: Boolean = false,
    title: String,
    toValue: Int,
    horizontalPadding: Dp = 18.dp,
    verticalPadding: Dp = 8.dp,
    onValueChange: (Float) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val placeholder = remember(showPlaceholder, value, toValue, valuePlaceholder) {
        derivedStateOf {
            if (!showPlaceholder) "${(value.first * toValue).roundToInt()}${value.second}"
            else valuePlaceholder
        }
    }
    val sliderState = rememberSliderState(
        value = value.first,
        trackRange = 0f..1f
    )

    SideEffect {
        if (sliderState.value != value.first) {
            sliderState.value = value.first
        }
    }

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.fillMaxWidth(0.2f)) {
            SettingsSubcategoryTitle(title = title, padding = 0.dp)
            Spacer(modifier = Modifier.height(4.dp))
            StyledText(
                text = placeholder.value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Slider(
            state = sliderState,
            onValueChange = {
                onValueChange(it)
            },
            interactionSource = interactionSource,
            thumb = {
                Label(
                    label = {
                        PlainTooltip(
                            modifier = Modifier.clip(CircleShape)
                        ) {
                            StyledText(
                                text = placeholder.value,
                                modifier = Modifier.padding(
                                    vertical = 6.dp,
                                    horizontal = 4.dp
                                )
                            )
                        }
                    },
                    interactionSource = interactionSource
                ) {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource
                    )
                }
            },
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                thumbColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                activeTickColor = MaterialTheme.colorScheme.onSecondary,
                inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}