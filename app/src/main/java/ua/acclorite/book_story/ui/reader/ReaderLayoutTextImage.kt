/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.helpers.imageZoom
import ua.acclorite.book_story.ui.theme.model.HorizontalAlignment

/** Base (fully zoomed out) zoom factor, matching [imageZoom]'s minimum. */
private const val MIN_IMAGE_ZOOM = 1f

/**
 * Inline image. Generated images support pinch-to-zoom and pan with
 * two fingers and can be long-pressed to open a context menu
 * with Set as book cover, Share and Delete actions.
 */
@Composable
fun LazyItemScope.ReaderLayoutTextImage(
    entry: ReaderText.Image,
    sidePadding: Dp,
    imagesCornersRoundness: Dp,
    imagesAlignment: HorizontalAlignment,
    imagesWidth: Float,
    imagesColorEffects: ColorFilter?,
    shareImage: (ReaderEvent.OnShareImage) -> Unit,
    copyImage: (ReaderEvent.OnCopyImage) -> Unit,
    setCoverImage: (ReaderEvent.OnSetCoverImage) -> Unit,
    deleteImage: (ReaderEvent.OnDeleteImage) -> Unit
) {
    val menuOffset = remember { mutableStateOf<DpOffset?>(null) }
    val zoomScale = remember(entry.attachmentId) { mutableFloatStateOf(1f) }
    val zoomOffset = remember(entry.attachmentId) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .animateItem(
                fadeInSpec = null,
                fadeOutSpec = null
            )
            .padding(horizontal = sidePadding)
            .fillMaxWidth(),
        contentAlignment = imagesAlignment.alignment
    ) {
        // Inner box is sized exactly to the image, so the DropdownMenu offset
        // matches the pressed position on the image itself.
        Box {
            Image(
                modifier = Modifier
                    .clip(RoundedCornerShape(imagesCornersRoundness))
                    .fillMaxWidth(imagesWidth)
                    .then(
                        if (entry.attachmentId != null) {
                            Modifier
                                .imageZoom(zoomScale, zoomOffset)
                                .pointerInput(entry.attachmentId) {
                                    detectTapGestures(
                                        onLongPress = { offset ->
                                            menuOffset.value = DpOffset(
                                                offset.x.toDp(),
                                                offset.y.toDp()
                                            )
                                        },
                                        onDoubleTap = {
                                            zoomScale.floatValue = MIN_IMAGE_ZOOM
                                            zoomOffset.value = Offset.Zero
                                        }
                                    )
                                }
                                .graphicsLayer {
                                    // Applied after the clip, so the image bounds
                                    // (with rounded corners) act as a fixed viewport.
                                    val scale = zoomScale.floatValue
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = zoomOffset.value.x
                                    translationY = zoomOffset.value.y
                                }
                        } else Modifier
                    ),
                bitmap = entry.imageBitmap,
                contentDescription = null,
                colorFilter = imagesColorEffects,
                contentScale = ContentScale.FillWidth
            )

            menuOffset.value?.let { offset ->
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { menuOffset.value = null },
                    offset = offset
                ) {
                    DropdownMenuItem(
                        text = { StyledText(text = stringResource(id = R.string.set_cover_image)) },
                        onClick = {
                            menuOffset.value = null
                            entry.attachmentId?.let { id ->
                                setCoverImage(ReaderEvent.OnSetCoverImage(attachmentId = id))
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { StyledText(text = stringResource(id = R.string.share)) },
                        onClick = {
                            menuOffset.value = null
                            entry.attachmentId?.let { id ->
                                shareImage(ReaderEvent.OnShareImage(attachmentId = id))
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { StyledText(text = stringResource(id = R.string.copy_image)) },
                        onClick = {
                            menuOffset.value = null
                            entry.attachmentId?.let { id ->
                                copyImage(ReaderEvent.OnCopyImage(attachmentId = id))
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { StyledText(text = stringResource(id = R.string.delete)) },
                        onClick = {
                            menuOffset.value = null
                            entry.attachmentId?.let { id ->
                                deleteImage(ReaderEvent.OnDeleteImage(attachmentId = id))
                            }
                        }
                    )
                }
            }
        }
    }
}
