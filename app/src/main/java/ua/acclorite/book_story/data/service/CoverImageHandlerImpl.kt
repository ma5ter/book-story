/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.service

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.acclorite.book_story.core.CoverImage
import ua.acclorite.book_story.domain.service.CoverImageHandler
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

class CoverImageHandlerImpl @Inject constructor(
    application: Application
) : CoverImageHandler {

    private val filesDir: File = application.filesDir
    private val coversDir = File(filesDir, "covers")
    private val imageAttachmentsDir = File(filesDir, "image_attachments")
    private val styleCollagesDir = File(application.cacheDir, "style_collages")

    override suspend fun saveCover(coverImage: CoverImage): Result<File> = runCatching {
        if (!coversDir.exists()) {
            coversDir.mkdirs()
        }

        val coverUri = "${UUID.randomUUID()}.webp"
        val cover = File(coversDir, coverUri)

        withContext(Dispatchers.IO) {
            BufferedOutputStream(FileOutputStream(cover)).use { output ->
                coverImage
                    .copy(Bitmap.Config.RGB_565, false)
                    .compress(Bitmap.CompressFormat.WEBP, 20, output)
                    .let { success ->
                        if (success) return@let
                        throw Exception("Couldn't save cover image.")
                    }
            }
        }

        cover
    }

    override suspend fun saveImageAttachment(coverImage: CoverImage): Result<File> = runCatching {
        if (!imageAttachmentsDir.exists()) {
            imageAttachmentsDir.mkdirs()
        }

        val attachmentUri = "${UUID.randomUUID()}.webp"
        val attachment = File(imageAttachmentsDir, attachmentUri)

        withContext(Dispatchers.IO) {
            BufferedOutputStream(FileOutputStream(attachment)).use { output ->
                coverImage
                    .compress(Bitmap.CompressFormat.WEBP, 90, output)
                    .let { success ->
                        if (success) return@let
                        throw Exception("Couldn't save generated image.")
                    }
            }
        }

        attachment
    }

    /**
     * Saves the style collage of [bookId] under a deterministic name in
     * the cache directory, overwriting the previous reference. Cache
     * storage is used because the collage is transient, regenerable data.
     */
    override suspend fun saveStyleCollage(
        bookId: Int,
        coverImage: CoverImage
    ): Result<File> = runCatching {
        if (!styleCollagesDir.exists()) {
            styleCollagesDir.mkdirs()
        }

        val collage = File(styleCollagesDir, "collage_$bookId.webp")

        withContext(Dispatchers.IO) {
            BufferedOutputStream(FileOutputStream(collage)).use { output ->
                coverImage
                    .compress(Bitmap.CompressFormat.WEBP, 85, output)
                    .let { success ->
                        if (success) return@let
                        throw Exception("Couldn't save style collage.")
                    }
            }
        }

        collage
    }

    override suspend fun deleteCover(coverImage: Uri): Result<Unit> = runCatching {
        coverImage.toFile().apply {
            if (exists() && !delete()) throw Exception("Couldn't delete cover image.")
        }
    }

    override suspend fun compressCover(coverImage: CoverImage): Result<CoverImage> = runCatching {
        val stream = ByteArrayOutputStream()
        coverImage.copy(Bitmap.Config.RGB_565, false)
            .compress(Bitmap.CompressFormat.WEBP, 20, stream)
        val byteArray = stream.toByteArray()
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}