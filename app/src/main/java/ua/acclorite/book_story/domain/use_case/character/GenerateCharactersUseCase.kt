/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.domain.use_case.character

import su.mya.openrouter.ChatMessage
import su.mya.openrouter.OpenRouterClient
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.core.log.logW
import ua.acclorite.book_story.data.parser.character.CharacterParser
import ua.acclorite.book_story.data.settings.SettingsManager
import ua.acclorite.book_story.domain.model.character.BookCharacter
import ua.acclorite.book_story.domain.model.reader.ReaderText
import ua.acclorite.book_story.domain.repository.CharacterRepository
import ua.acclorite.book_story.domain.use_case.book.GetTextUseCase
import java.io.IOException
import javax.inject.Inject

private const val TAG = "GenerateCharacters"

/**
 * Instruction prepended to continuation requests together with the
 * catalog accumulated so far, so the model extends it with the next
 * text part instead of starting over.
 */
private const val EXTEND_PROMPT =
    "This is a continuation: previous parts of the book text were already analyzed. " +
    "Below is the characters catalog built so far, followed by the next part of the book text. " +
    "Extend the catalog with any new characters found in this part and enrich existing entries " +
    "with new visual details. Keep already described characters and their wording unchanged " +
    "unless this part corrects them. Output only the full, updated characters catalog, " +
    "in the same strict format as instructed."

class GenerateCharactersUseCase @Inject constructor(
    private val getTextUseCase: GetTextUseCase,
    private val characterRepository: CharacterRepository,
    private val openRouterClient: OpenRouterClient,
    private val characterParser: CharacterParser,
    private val settingsManager: SettingsManager
) {

    /**
     * Analyzes the whole book text (without images, only clean text
     * delimited with \n) with the configured summarize model,
     * parses the returned markdown catalog and
     * persists it as book metadata, replacing any previous catalog.
     *
     * When the text may not fit into the model's max input tokens
     * (Settings -> Reader -> AI), it is fed in chunks; every continuation
     * request supplies the catalog received so far, so the model
     * extends it with missing information instead of starting over.
     * [onProgress] reports the percentage of completed chunk requests;
     * it is not called when the whole text fits into a single request.
     *
     * [onDownloadProgress] is called each time the server emits a
     * keep-alive blank line while processing a request.
     */
    suspend operator fun invoke(
        bookId: Int,
        systemPrompt: String,
        onProgress: (Int?) -> Unit = {},
        onDownloadProgress: () -> Unit = {}
    ): Result<List<BookCharacter>> {
        logI(TAG, "Generating characters catalog for book [$bookId].")

        val bookText = getTextUseCase(bookId)
            .mapNotNull { entry ->
                when (entry) {
                    is ReaderText.Chapter -> entry.title.trim()
                    is ReaderText.Text -> entry.line.text.trim()
                    else -> null
                }
            }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")

        if (bookText.isBlank()) {
            return Result.failure(IllegalStateException("Book text is empty."))
        }

        val catalog = generateCatalog(systemPrompt, bookText, onProgress, onDownloadProgress)
            .getOrElse {
            return Result.failure(it)
        }

        val characters = characterParser.parse(catalog)
        if (characters.isEmpty()) {
            return Result.failure(IOException("Model returned no characters."))
        }

        return characterRepository.saveCharacters(bookId, characters)
    }

    /**
     * Sends the book text to the model in one request or in chunks when
     * the estimated token count may overflow the configured max input
     * tokens. Reports the percentage of completed requests via
     * [onProgress]. Returns the final characters catalog markdown.
     */
    private suspend fun generateCatalog(
        systemPrompt: String,
        bookText: String,
        onProgress: (Int?) -> Unit,
        onDownloadProgress: () -> Unit
    ): Result<String> {
        val maxTokens = settingsManager.textMaxTokens.lastValue
        val systemTokens = estimateTokens(systemPrompt)
        val chunkBudget = maxTokens - systemTokens -
                OUTPUT_TOKENS_RESERVE - EXTEND_PROMPT_RESERVE

        if (chunkBudget < MIN_CHUNK_TOKENS) {
            return Result.failure(
                IllegalStateException(
                    "Text model max tokens ($maxTokens) is too small to fit " +
                            "the system prompt and the model's reply. " +
                            "Increase it under Settings -> Reader -> AI."
                )
            )
        }

        val chunks = chunkBookText(bookText, chunkBudget)
        logI(
            TAG,
            "generateCatalog: estimated ${estimateTokens(bookText)} tokens in " +
                    "${bookText.length} characters, ${chunks.size} request(s), " +
                    "max tokens: $maxTokens."
        )

        var catalog: String? = null
        for ((index, chunk) in chunks.withIndex()) {
            // A single request cannot be estimated, stay indeterminate.
            if (chunks.size > 1) onProgress(index * 100 / chunks.size)
            val messages = if (catalog == null) {
                listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = chunk)
                )
            } else {
                listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(
                        role = "user",
                        content = "$EXTEND_PROMPT\n\n$catalog\n\n" +
                                "--- Next part of the book text ---\n\n$chunk"
                    )
                )
            }

            val reply = openRouterClient.chatCompletion(
                model = settingsManager.summarizeModel.lastValue.trim(),
                messages = messages,
                onProgress = onDownloadProgress
            )
            if (reply.isFailure) {
                val error = reply.exceptionOrNull() ?: IOException("Character generation failed.")
                if (catalog.isNullOrBlank()) return Result.failure(error)
                // Keep the catalog built so far instead of losing all progress.
                logW(TAG, "generateCatalog: request ${index + 1} failed: ${error.message}.")
                break
            }

            reply.getOrNull()?.content?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { catalog = it }
        }

        return catalog?.takeIf { it.isNotBlank() }?.let { Result.success(it) }
            ?: Result.failure(IOException("Model returned no characters."))
    }

    /**
     * Splits the book text into line-aligned chunks, each fitting into
     * [chunkTokens] estimated tokens. A text that fits whole is not chunked.
     */
    private fun chunkBookText(text: String, chunkTokens: Int): List<String> {
        if (estimateTokens(text) <= chunkTokens) return listOf(text)

        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        text.lineSequence().forEach { line ->
            if (
                current.isNotEmpty() &&
                estimateTokens(current.toString()) + estimateTokens(line) > chunkTokens
            ) {
                chunks.add(current.toString())
                current.clear()
            }
            if (current.isNotEmpty()) current.append('\n')
            current.append(line)
        }
        if (current.isNotEmpty()) chunks.add(current.toString())
        return chunks
    }

    /**
     * Rough token estimate: models average 3-4 characters per token,
     * the conservative lower bound is used to stay on the safe side.
     */
    private fun estimateTokens(text: String): Int {
        return (text.length + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN
    }

    private companion object {
        /** Conservative characters-per-token ratio for rough token estimation. */
        private const val CHARS_PER_TOKEN = 3

        /** Tokens reserved in every request for the model's reply. */
        private const val OUTPUT_TOKENS_RESERVE = 4096

        /** Tokens reserved for the extension instruction of continuation requests. */
        private const val EXTEND_PROMPT_RESERVE = 256

        /** Smallest sensible chunk budget; smaller requests cannot fit the catalog. */
        private const val MIN_CHUNK_TOKENS = 1024
    }
}
