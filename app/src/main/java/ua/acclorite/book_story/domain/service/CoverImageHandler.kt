/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.service

import android.net.Uri
import ua.acclorite.book_story.core.CoverImage
import java.io.File

interface CoverImageHandler {
    suspend fun saveCover(coverImage: CoverImage): Result<File>
    suspend fun saveImageAttachment(coverImage: CoverImage): Result<File>

    /**
     * Saves a style collage of the book's illustrations as the style
     * reference of the image generation pipeline, overwriting the
     * previous reference of the book.
     */
    suspend fun saveStyleCollage(bookId: Int, coverImage: CoverImage): Result<File>
    /**
     * Saves the already adjusted square [coverImage] as the cached portrait
     * of the character with [characterId], overwriting the previous portrait.
     */
    suspend fun saveCharacterImage(characterId: Int, coverImage: CoverImage): Result<File>

    /**
     * Returns the cached portrait file of the character,
     * or null when no picture was loaded yet.
     */
    fun getCharacterImageFile(characterId: Int): File?
    suspend fun deleteCover(coverImage: Uri): Result<Unit>
    suspend fun compressCover(coverImage: CoverImage): Result<CoverImage>
}