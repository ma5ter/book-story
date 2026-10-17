/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ua.acclorite.book_story.data.local.dto.BookAttachmentEntity

@Dao
interface BookAttachmentDao {

    @Query("SELECT * FROM BookAttachmentEntity WHERE bookId = :bookId ORDER BY paragraphIndex ASC")
    suspend fun getAttachmentsForBook(bookId: Int): List<BookAttachmentEntity>

    @Query("SELECT * FROM BookAttachmentEntity WHERE id = :id LIMIT 1")
    suspend fun findAttachmentById(id: Int): BookAttachmentEntity?

    @Insert
    suspend fun insertAttachment(attachment: BookAttachmentEntity): Long

    @Update
    suspend fun updateAttachment(attachment: BookAttachmentEntity)

    @Query("DELETE FROM BookAttachmentEntity WHERE id = :id")
    suspend fun deleteAttachmentById(id: Int)

    @Query("DELETE FROM BookAttachmentEntity WHERE bookId = :bookId AND status = 'PENDING'")
    suspend fun deletePendingAttachmentsForBook(bookId: Int)
}
