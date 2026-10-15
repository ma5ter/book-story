/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.core.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Renders a collage of the given images into a single bitmap.
 *
 * The grid is derived from the number of selected images:
 * `rows = ceil(sqrt(n))`, `cols = ceil(n / rows)`, so the whole canvas is
 * covered. When the grid has more slots than selected images (e.g. 5
 * images in a 3x2 grid), the remaining slots are filled by cycling
 * through the shuffled selection.
 *
 * Every cell is filled with the ContentScale.Crop equivalent: the image
 * is uniformly scaled so it covers the whole cell and is centered inside
 * it, so cells never show gaps or letterboxing.
 */
object CollageRenderer {

    /**
     * Renders a [width] x [height] collage from [images].
     * Up to [maxImages] images are randomly selected; the selection is
     * shuffled and the slots are filled in that shuffled order.
     */
    fun render(
        images: List<Bitmap>,
        width: Int,
        height: Int,
        maxImages: Int
    ): Bitmap {
        require(width > 0 && height > 0) { "Collage size must be positive." }
        require(images.isNotEmpty()) { "Collage needs at least one image." }

        val selected = images.shuffled().take(maxImages)
        val rows = ceil(sqrt(selected.size.toDouble())).toInt().coerceAtLeast(1)
        val cols = ceil(selected.size / rows.toDouble()).toInt().coerceAtLeast(1)

        val cellWidth = width / cols
        val cellHeight = height / rows

        val collage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(collage)
        // Filtered drawing, so scaled-down images are smooth.
        val paint = Paint().apply { isFilterBitmap = true }

        // Slots are filled in a shuffled order, cycling through the
        // selection when there are more slots than images.
        val slots = (0 until rows * cols).shuffled()

        slots.forEachIndexed { fillOrder, slot ->
            val bitmap = selected[fillOrder % selected.size]
            drawCell(
                canvas = canvas,
                paint = paint,
                bitmap = bitmap,
                left = (slot % cols) * cellWidth,
                top = (slot / cols) * cellHeight,
                cellWidth = cellWidth,
                cellHeight = cellHeight
            )
        }

        return collage
    }

    /**
     * Draws [bitmap] scale-to-fill, center-cropped into the cell with
     * its top-left corner at ([left], [top]) and the given cell size.
     */
    private fun drawCell(
        canvas: Canvas,
        paint: Paint,
        bitmap: Bitmap,
        left: Int,
        top: Int,
        cellWidth: Int,
        cellHeight: Int
    ) {
        canvas.save()
        canvas.clipRect(left, top, left + cellWidth, top + cellHeight)

        // Uniform scale so the bitmap covers the whole cell.
        val scale = max(
            cellWidth / bitmap.width.toFloat(),
            cellHeight / bitmap.height.toFloat()
        )
        // Center the scaled bitmap inside the cell; the overflow is
        // clipped away, which is the center-crop part.
        canvas.translate(
            left + (cellWidth - bitmap.width * scale) / 2f,
            top + (cellHeight - bitmap.height * scale) / 2f
        )
        canvas.scale(scale, scale)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        canvas.restore()
    }
}
