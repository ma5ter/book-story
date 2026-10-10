/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.presentation.reader.ReaderEvent
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.theme.model.HorizontalAlignment

/** Minimum zoom factor of a generated illustration (fully zoomed out). */
private const val MIN_IMAGE_ZOOM = 1f

/** Maximum zoom factor of a generated illustration. */
private const val MAX_IMAGE_ZOOM = 4f

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
    setCoverImage: (ReaderEvent.OnSetCoverImage) -> Unit,
    deleteImage: (ReaderEvent.OnDeleteImage) -> Unit
) {
    val menuOffset = remember { mutableStateOf<DpOffset?>(null) }
    val zoomScale = remember(entry.attachmentId) { mutableFloatStateOf(MIN_IMAGE_ZOOM) }
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
                                .generatedImageZoom(zoomScale, zoomOffset)
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

/**
 * Pinch-to-zoom and pan behavior for a generated illustration.
 * Two-finger pinch zooms around the gesture centroid, panning (also with a
 * single finger while zoomed) explores the zoomed image. Pointer events are
 * consumed while transforming, so the reader list does not scroll during
 * the gesture. Once fully zoomed out, the transform snaps back to base state.
 */
private fun Modifier.generatedImageZoom(
    scale: MutableFloatState,
    offset: MutableState<Offset>
): Modifier = pointerInput(scale, offset) {
    // Constrains the pan offset so the zoomed image always covers the viewport:
    // its borders can never move inside the visible bounds.
    fun clampOffset(newOffset: Offset, newScale: Float): Offset {
        val maxX = (newScale - MIN_IMAGE_ZOOM) * size.width / 2f
        val maxY = (newScale - MIN_IMAGE_ZOOM) * size.height / 2f
        return Offset(
            x = newOffset.x.coerceIn(-maxX, maxX),
            y = newOffset.y.coerceIn(-maxY, maxY)
        )
    }

    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var transforming = scale.floatValue > MIN_IMAGE_ZOOM

        do {
            val event = awaitPointerEvent()
            if (!transforming && event.changes.count { it.pressed } >= 2) {
                transforming = true
            }

            if (transforming) {
                val currentScale = scale.floatValue
                val newScale = (currentScale * event.calculateZoom())
                    .coerceIn(MIN_IMAGE_ZOOM, MAX_IMAGE_ZOOM)
                val factor = newScale / currentScale
                var newOffset = offset.value

                if (factor != 1f) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    // Keep the point under the centroid stationary while zooming.
                    newOffset = Offset(
                        x = centroid.x - centerX - (centroid.x - centerX - newOffset.x) * factor,
                        y = centroid.y - centerY - (centroid.y - centerY - newOffset.y) * factor
                    )
                }
                if (newScale > MIN_IMAGE_ZOOM) {
                    newOffset += event.calculatePan()
                }

                scale.floatValue = newScale
                offset.value = clampOffset(newOffset, newScale)
                event.changes.forEach { change ->
                    if (change.positionChanged()) change.consume()
                }
            }
        } while (event.changes.any { it.pressed })

        // Snap back to the base state once fully zoomed out.
        if (scale.floatValue <= MIN_IMAGE_ZOOM) {
            scale.floatValue = MIN_IMAGE_ZOOM
            offset.value = Offset.Zero
        }
    }
}
