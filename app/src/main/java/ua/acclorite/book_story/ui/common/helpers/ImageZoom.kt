/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.common.helpers

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

/** Minimum zoom factor of a zoomable image (fully zoomed out). */
private const val MIN_IMAGE_ZOOM = 1f

/** Maximum zoom factor of a zoomable image. */
private const val MAX_IMAGE_ZOOM = 4f

/**
 * Pinch-to-zoom and pan behavior for an image filling its viewport node
 * (a generated illustration in the reader, a portrait crop dialog, etc.).
 * Two-finger pinch zooms around the gesture centroid, panning (also with a
 * single finger while zoomed) explores the zoomed image. Pointer events are
 * consumed while transforming, so scrollable parents do not scroll during
 * the gesture. Once fully zoomed out, the transform snaps back to base state.
 *
 * [contentWidth] and [contentHeight] are the image's untransformed displayed
 * size in pixels inside the viewport node, for images that do not fill the
 * viewport (e.g. a whole picture fitted into the portrait crop viewport).
 * When omitted, the viewport size is used, i.e. the image covers the node.
 */
fun Modifier.imageZoom(
    scale: MutableFloatState,
    offset: MutableState<Offset>,
    contentWidth: Float? = null,
    contentHeight: Float? = null
): Modifier = pointerInput(scale, offset, contentWidth, contentHeight) {
    // Constrains the pan offset, so the image cannot be dragged beyond its
    // transformed bounds: on each axis where the scaled image is larger than
    // the viewport its borders can never move inside the visible bounds;
    // where it is smaller it stays centered.
    fun clampOffset(newOffset: Offset, newScale: Float): Offset {
        val baseWidth = contentWidth ?: size.width.toFloat()
        val baseHeight = contentHeight ?: size.height.toFloat()
        val maxX = ((baseWidth * newScale - size.width) / 2f).coerceAtLeast(0f)
        val maxY = ((baseHeight * newScale - size.height) / 2f).coerceAtLeast(0f)
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
