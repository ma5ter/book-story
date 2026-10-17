/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ua.acclorite.book_story.data.local.dto.BookAttachmentCharacterEntity

@Dao
interface BookAttachmentCharacterDao {

    @Query(
        "SELECT characterId FROM BookAttachmentCharacterEntity " +
                "WHERE attachmentId = :attachmentId ORDER BY characterId ASC"
    )
    suspend fun getCharacterIdsForAttachment(attachmentId: Int): List<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAttachmentsCharacters(relations: List<BookAttachmentCharacterEntity>)
}
