/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.data.service

import android.app.Application
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import ua.acclorite.book_story.core.log.logI
import ua.acclorite.book_story.core.log.logW
import ua.acclorite.book_story.data.model.file.CachedFile
import ua.acclorite.book_story.data.parser.cover.fb2XlinkPrefix
import ua.acclorite.book_story.data.parser.cover.XLINK_NAMESPACE
import ua.acclorite.book_story.data.parser.text.Fb2TextParser
import ua.acclorite.book_story.domain.model.attachment.ImageAttachment
import ua.acclorite.book_story.domain.model.attachment.ImageAttachmentStatus
import ua.acclorite.book_story.domain.service.Fb2BookExporter
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Fb2BookExporter"

/** Id prefix of the embedded generated image binaries. */
private const val GENERATED_ID_PREFIX = "gen"

/** One generated image prepared for embedding. */
private data class EmbeddedImage(
    val attachment: ImageAttachment,
    val id: String,
    val bytes: ByteArray,
    val contentType: String
)

/**
 * Embeds completed generated images into the FB2 DOM and writes
 * the result as a new file, ready for sharing.
 */
@Singleton
class Fb2BookExporterImpl @Inject constructor(
    private val application: Application,
    private val fb2TextParser: Fb2TextParser
) : Fb2BookExporter {

    override suspend fun exportWithImages(
        cachedFile: CachedFile,
        attachments: List<ImageAttachment>
    ): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            val document = cachedFile.openInputStream()?.use { stream ->
                Jsoup.parse(stream, null, "", Parser.xmlParser())
            } ?: throw IOException("Could not read FB2 file.")
            val root = document.children().firstOrNull()
                ?: throw IOException("FB2 file is empty.")

            val completed = attachments.filter {
                it.status == ImageAttachmentStatus.COMPLETED && it.filePath.isNotBlank()
            }
            if (completed.isEmpty()) {
                throw IOException("No generated images to embed.")
            }

            // Re-run the FB2 text parsing to align the persisted paragraph
            // indexes with the original DOM elements.
            val (_, elementMap) = fb2TextParser.parseWithElements(document)
            if (elementMap.isEmpty()) {
                throw IOException("Could not parse FB2 text.")
            }

            val usedIds = root.children()
                .filter { it.tagName().equals("binary", ignoreCase = true) }
                .mapTo(mutableSetOf()) { it.attr("id") }
            val embedded = completed.mapNotNull { attachment ->
                embed(attachment, usedIds) ?: run {
                    logW(TAG, "Skipping unreadable generated image [${attachment.id}].")
                    null
                }
            }
            if (embedded.isEmpty()) {
                throw IOException("Generated images are unreadable.")
            }

            val xlinkPrefix = document.fb2XlinkPrefix()

            // <p><image xlink:href="#id"/></p> right after the matched
            // paragraph, in reading order.
            embedded.forEach { image ->
                elementMap.getOrNull(image.attachment.paragraphIndex)
                    ?.after(imageParagraph(document, image, xlinkPrefix))
                    ?: logW(
                        TAG,
                        "Skipping image [${image.id}]: paragraph " +
                                "[${image.attachment.paragraphIndex}] not found."
                    )
            }

            // <binary id="..." content-type="...">base64</binary> as the last
            // children of the root element, existing binaries are preserved.
            embedded.forEach { image ->
                val binary = document.createElement("binary")
                binary.attr("id", image.id)
                binary.attr("content-type", image.contentType)
                binary.appendText(Base64.encodeToString(image.bytes, Base64.NO_WRAP))
                root.appendChild(binary)
            }

            val exportsDir = File(application.cacheDir, "exports").apply { mkdirs() }
            val exportedFile = File(
                exportsDir,
                cachedFile.name.substringBeforeLast(".").trim()
                    .ifBlank { "book" } + "_with_images.fb2"
            )

            document.childNodes().toList()
                .filter { it.nodeName() == "#declaration" }
                .forEach { it.remove() }
            document.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .charset(StandardCharsets.UTF_8)
                .prettyPrint(false)

            exportedFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                writer.newLine()
                writer.write(document.outerHtml())
            }

            logI(
                TAG,
                "Exported FB2 with ${embedded.size} generated images: " +
                        exportedFile.absolutePath
            )
            exportedFile
        }
    }

    /**
     * Reads the generated image file of the [attachment] and generates
     * a binary id unique within the document, tracked in [usedIds].
     */
    private fun embed(attachment: ImageAttachment, usedIds: MutableSet<String>): EmbeddedImage? {
        val file = File(attachment.filePath)
        if (!file.exists() || !file.canRead()) return null
        val bytes = try {
            file.readBytes()
        } catch (e: Exception) {
            return null
        }

        // CoverImageHandlerImpl always saves generated images as WEBP.
        val contentType = "image/webp"

        var id = "${GENERATED_ID_PREFIX}_${attachment.id}"
        var suffix = 0
        while (!usedIds.add(id)) {
            id = "${GENERATED_ID_PREFIX}_${attachment.id}_${++suffix}"
        }

        return EmbeddedImage(
            attachment = attachment,
            id = id,
            bytes = bytes,
            contentType = contentType
        )
    }

    /**
     * Builds the `<p><image/></p>` paragraph referencing the [image].
     */
    private fun imageParagraph(
        document: Document,
        image: EmbeddedImage,
        xlinkPrefix: String
    ): Element {
        val paragraph = document.createElement("p")
        val generatedImage = document.createElement("image")
        if (xlinkPrefix.isBlank()) {
            // XLink is bound to no prefix or not declared at all:
            // bind it locally, so the reference is always resolvable.
            generatedImage.attr("xmlns:xlink", XLINK_NAMESPACE)
            generatedImage.attr("xlink:href", "#${image.id}")
        } else {
            generatedImage.attr("${xlinkPrefix}href", "#${image.id}")
        }
        paragraph.appendChild(generatedImage)
        return paragraph
    }
}
