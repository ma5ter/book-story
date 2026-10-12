/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.parser.character

import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.domain.model.character.BookCharacter
import javax.inject.Inject

/**
 * Parses the model's markdown characters catalog.
 */
interface CharacterParser {

    /**
     * Extracts characters from the reply. Each entry is returned with
     * default ids; persistence assigns real ones.
     */
    fun parse(markdown: String): List<BookCharacter>
}

class CharacterParserImpl @Inject constructor() : CharacterParser {

    /**
     * Tolerant markdown parsing: a level-one heading starts a new character,
     * subsequent non-empty lines accumulate its description. Code fences and
     * other headings/dividers are skipped.
     */
    override fun parse(markdown: String): List<BookCharacter> {
        val characters = mutableListOf<BookCharacter>()
        var name: String? = null
        val description = StringBuilder()
        var inCodeFence = false

        fun flushCharacter() {
            val characterName = name?.trim().orEmpty()
            val characterDescription = description.toString().trim()
            if (characterName.isNotBlank()) {
                characters.add(
                    BookCharacter(
                        name = characterName,
                        description = characterDescription
                    )
                )
            }
            name = null
            description.clear()
        }

        markdown.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                inCodeFence = !inCodeFence
                return@forEach
            }
            if (inCodeFence) return@forEach

            when {
                trimmed.startsWith("# ") -> {
                    flushCharacter()
                    name = trimmed.removePrefix("# ").trim()
                }

                trimmed.startsWith("#") ||
                        trimmed.startsWith("---") ||
                        trimmed.startsWith("===") -> return@forEach

                trimmed.isBlank() -> {
                    if (description.isNotEmpty()) description.append("\n\n")
                }

                else -> {
                    // Joined lines are wrapped fragments of one paragraph:
                    // restore the whitespace dropped by trimming each line.
                    if (description.isNotEmpty() && !description.endsWith("\n")) {
                        description.append(' ')
                    }
                    description.append(trimmed)
                }
            }
        }
        flushCharacter()

        logI(TAG, "parse: extracted ${characters.size} characters.")
        return characters
    }

    private companion object {
        private const val TAG = "CharacterParser"
    }
}
