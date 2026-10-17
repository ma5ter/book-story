/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.settings.reader.ai.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.R
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.domain.model.character.BookCharacter
import ua.acclorite.book_story.ui.common.components.progress_indicator.CircularProgressIndicator
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.common.helpers.imageZoom

/** Side of the displayed round viewport. */
private const val VIEWPORT_SIZE_DP = 256

/** Side of the saved square portrait. */
private const val OUTPUT_SIZE = 512

/** Source decode cap (larger side), leaving headroom for zooming. */
private const val MAX_SOURCE_SIZE = 2048

/**
 * Round-viewport portrait adjustment dialog, shown after a character
 * picture is loaded or pasted. The image can be zoomed and panned inside
 * the circular viewport with the same gesture model as generated
 * illustrations in the reader. OK renders the visible square as an
 * [OUTPUT_SIZE]x[OUTPUT_SIZE] portrait and accepts it, Cancel discards
 * the addition/change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterPortraitCropDialog(
    character: BookCharacter,
    image: Uri,
    onAccept: (CoverImage) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var source by remember(image) { mutableStateOf<CoverImage?>(null) }
    var decodeFailed by remember(image) { mutableStateOf(false) }
    val zoomScale = remember(source) { mutableFloatStateOf(1f) }
    val zoomOffset = remember(source) { mutableStateOf(Offset.Zero) }
    var viewportPx by remember { mutableIntStateOf(0) }
    // Untransformed size of the whole picture fitted into the viewport,
    // so the adjustment starts from the complete un-cropped image.
    val fitSize = remember(source, viewportPx) {
        source?.takeIf { viewportPx > 0 }?.let { bitmap ->
            val fit = minOf(
                viewportPx / bitmap.width.toFloat(),
                viewportPx / bitmap.height.toFloat()
            )
            bitmap.width * fit to bitmap.height * fit
        }
    }

    LaunchedEffect(image) {
        val decoded = decodeForCrop(context, image)
        source = decoded
        decodeFailed = decoded == null
    }

    BasicAlertDialog(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.shapes.extraLarge
            )
            .padding(top = 24.dp, bottom = 12.dp),
        onDismissRequest = onDismiss
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StyledText(
                text = stringResource(id = R.string.portrait_crop_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            StyledText(
                text = stringResource(id = R.string.portrait_crop_desc),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            val bitmap = source
            when {
                bitmap != null -> {
                    Box(
                        modifier = Modifier
                            .size(VIEWPORT_SIZE_DP.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .onSizeChanged { viewportPx = it.width },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = character.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .imageZoom(zoomScale, zoomOffset, fitSize?.first, fitSize?.second)
                                .graphicsLayer {
                                    val scale = zoomScale.floatValue
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = zoomOffset.value.x
                                    translationY = zoomOffset.value.y
                                }
                        )
                    }
                }

                decodeFailed -> {
                    Box(
                        modifier = Modifier.size(VIEWPORT_SIZE_DP.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        StyledText(
                            text = stringResource(id = R.string.error_could_not_decode),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier.size(VIEWPORT_SIZE_DP.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.5.dp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    StyledText(
                        text = stringResource(id = R.string.cancel),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                TextButton(
                    enabled = source != null && viewportPx > 0,
                    onClick = {
                        val cropSource = source
                        if (cropSource == null || viewportPx <= 0) return@TextButton
                        onAccept(
                            renderCrop(
                                bitmap = cropSource,
                                zoom = zoomScale.floatValue,
                                offset = zoomOffset.value,
                                viewportPx = viewportPx
                            )
                        )
                    }
                ) {
                    StyledText(
                        text = stringResource(id = R.string.ok),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

/**
 * Renders the visible part of the round viewport as an
 * [OUTPUT_SIZE]x[OUTPUT_SIZE] portrait, applying the same fit, zoom
 * and pan transform used for the display.
 */
private fun renderCrop(
    bitmap: CoverImage,
    zoom: Float,
    offset: Offset,
    viewportPx: Int
): CoverImage {
    val outScale = OUTPUT_SIZE / viewportPx.toFloat()
    // Whole picture fitted into the viewport, matching the displayed fit.
    val scale = minOf(
        viewportPx / bitmap.width.toFloat(),
        viewportPx / bitmap.height.toFloat()
    ) * zoom * outScale

    val output = Bitmap.createBitmap(OUTPUT_SIZE, OUTPUT_SIZE, Bitmap.Config.ARGB_8888)
    Canvas(output).apply {
        translate(OUTPUT_SIZE / 2f, OUTPUT_SIZE / 2f)
        translate(offset.x * outScale, offset.y * outScale)
        scale(scale, scale)
        drawBitmap(
            bitmap,
            -bitmap.width / 2f,
            -bitmap.height / 2f,
            Paint().apply { isFilterBitmap = true }
        )
    }
    return output
}

/**
 * Decodes [image] with downsampling, so its larger side fits
 * [MAX_SOURCE_SIZE] pixels, leaving headroom for zooming.
 * Returns null when the image cannot be decoded.
 */
private suspend fun decodeForCrop(context: Context, image: Uri): CoverImage? {
    return withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(image)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > MAX_SOURCE_SIZE ||
            bounds.outHeight / sampleSize > MAX_SOURCE_SIZE
        ) {
            sampleSize *= 2
        }

        context.contentResolver.openInputStream(image)?.use { stream ->
            BitmapFactory.decodeStream(
                stream,
                null,
                BitmapFactory.Options().apply { inSampleSize = sampleSize }
            )
        }
    }
}
