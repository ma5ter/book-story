/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2026 ma5ter
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.local.dto

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Relation between a generated illustration and a character of the
 * book's catalog that it includes. Both foreign keys cascade: removing
 * the attachment or the character removes the relation.
 */
@Entity(
    tableName = "BookAttachmentCharacterEntity",
    primaryKeys = ["attachmentId", "characterId"],
    foreignKeys = [
        ForeignKey(
            entity = BookAttachmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["attachmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookCharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterId")]
)
data class BookAttachmentCharacterEntity(
    val attachmentId: Int,
    val characterId: Int
)
